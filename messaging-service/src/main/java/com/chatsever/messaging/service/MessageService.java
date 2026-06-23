package com.chatsever.messaging.service;

import com.chatsever.grpc.role.*;
import com.chatsever.grpc.server.*;
import net.devh.boot.grpc.client.inject.GrpcClient;

import com.chatsever.common.dto.LogEntry;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import com.chatsever.messaging.entity.ChatMessage;
import com.chatsever.messaging.entity.MessageReaction;
import com.chatsever.messaging.repository.MessageRepository;
import com.chatsever.messaging.repository.MessageReactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.chatsever.messaging.adapter.ServerInfoGrpcAdapter;
import com.chatsever.messaging.adapter.RoleGrpcAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.stream.Collectors;

@Service
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final MessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;


    @Autowired
    private ServerInfoGrpcAdapter serverServiceClient;

    @Autowired
    private RoleGrpcAdapter roleServiceClient;

    @Autowired
    private com.chatsever.messaging.adapter.PresenceGrpcAdapter presenceServiceClient;

    @Autowired
    private com.chatsever.messaging.adapter.ChannelGrpcAdapter channelServiceClient;

    public MessageService(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, MessageRepository messageRepository, MessageReactionRepository reactionRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.messageRepository = messageRepository;
        this.reactionRepository = reactionRepository;
    }

    public boolean hasPermission(Long serverId, String username) {
        try {
            GetServerDetailsRequest req = GetServerDetailsRequest.newBuilder().setServerId(serverId).build();
            GetServerDetailsResponse resp = serverServiceClient.getServerDetails(req);
            
            for (MemberDetails member : resp.getMembersList()) {
                if (username.equals(member.getUserId())) {
                    return true;
                }
            }
            logger.info("User {} chưa là member của server {} → tự động thêm", username, serverId);
            serverServiceClient.ensureMember(EnsureMemberRequest.newBuilder().setServerId(serverId).setUserId(username).build());
            return true;
        } catch (Exception e) {
            logger.error("Error checking permission: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean canSearchChannel(Long channelId, String username) {
        if (channelId == null) return false;
        List<Long> serverIds = messageRepository.findServerIdsByChannel(
                channelId, org.springframework.data.domain.PageRequest.of(0, 1));
        if (serverIds.isEmpty()) return true;
        return isServerMember(serverIds.get(0), username);
    }

    private boolean isServerMember(Long serverId, String username) {
        if (serverId == null) return true;
        try {
            GetServerDetailsRequest req = GetServerDetailsRequest.newBuilder().setServerId(serverId).build();
            GetServerDetailsResponse resp = serverServiceClient.getServerDetails(req);
            
            return resp.getMembersList().stream().anyMatch(m -> username.equals(m.getUserId()));
        } catch (Exception e) {
            logger.error("Error checking server membership: {}", e.getMessage());
        }
        return false;
    }

    public void broadcastToChannel(MessageDTO msg) {
        rabbitTemplate.convertAndSend("chat.fanout", "", msg);
    }

    public void sendToLocalSessions(MessageDTO msg, Map<String, WebSocketSession> sessions) throws Exception {
        Set<String> serverMembers = null;
        if (msg.getServerId() != null) {
            serverMembers = getServerMembers(msg.getServerId());
        }
        
        String payload = objectMapper.writeValueAsString(msg);
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String username = entry.getKey();
            WebSocketSession s = entry.getValue();
            if (s.isOpen()) {
                if (serverMembers == null || serverMembers.contains(username)) {
                    s.sendMessage(new TextMessage(payload));
                }
            }
        }
    }

    private Set<String> getServerMembers(Long serverId) {
        try {
            if (serverId == null) return Set.of();
            GetServerDetailsRequest req = GetServerDetailsRequest.newBuilder().setServerId(serverId).build();
            GetServerDetailsResponse resp = serverServiceClient.getServerDetails(req);
            
            return resp.getMembersList().stream()
                    .map(MemberDetails::getUserId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            logger.error("Error fetching server members: {}", e.getMessage(), e);
        }
        return Set.of();
    }

    public void sendPrivate(MessageDTO msg, WebSocketSession senderSession, Map<String, WebSocketSession> sessions) throws Exception {
        String payload = objectMapper.writeValueAsString(msg);
        
        WebSocketSession receiver = sessions.get(msg.getReceiver());
        if (receiver != null && receiver.isOpen()) {
            receiver.sendMessage(new TextMessage(payload));
        } 
        
        if (senderSession != null && senderSession.isOpen()) {
            senderSession.sendMessage(new TextMessage(payload));
        }
    }

    public void notifyPresence(String username, String action) {
        try {
            com.chatsever.grpc.presence.NotifyPresenceRequest req = com.chatsever.grpc.presence.NotifyPresenceRequest.newBuilder()
                    .setUsername(username)
                    .setAction(action)
                    .build();
            presenceServiceClient.notifyPresence(req);
        } catch (Exception e) {
            logger.error("Error notifying presence: {}", e.getMessage(), e);
        }
    }

    public void publishLogEvent(MessageDTO msg) {
        LogEntry log = new LogEntry(msg.getTimestamp(), msg.getType().name(),
                msg.getSender(), msg.getReceiver(), msg.getContent(),
                msg.getChannelId(), msg.getServerId());
        rabbitTemplate.convertAndSend("chat.exchange", "log." + msg.getType().name().toLowerCase(), log);
    }

    public void publishNotificationEvent(MessageDTO msg) {
        rabbitTemplate.convertAndSend("chat.exchange", "notify.message", msg);
    }

    public void sendError(WebSocketSession session, String errorMsg) throws Exception {
        MessageDTO error = new MessageDTO(MessageType.ERROR, "SERVER", null, errorMsg, LocalDateTime.now());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
    }

    public ChatMessage saveMessage(MessageDTO msg) {
        ChatMessage entity = new ChatMessage();
        entity.setSender(msg.getSender());
        entity.setContent(msg.getContent());
        entity.setReceiver(msg.getReceiver()); 
        entity.setChannelId(msg.getChannelId());
        entity.setServerId(msg.getServerId());
        entity.setTimestamp(msg.getTimestamp() != null ? msg.getTimestamp() : LocalDateTime.now());
        entity.setType(msg.getType());
        entity.setIsEdited(msg.getIsEdited() != null ? msg.getIsEdited() : false);
        entity.setReplyToMessageId(msg.getReplyToMessageId());
        
        ChatMessage saved = messageRepository.save(entity);
        msg.setMessageId(saved.getId());

        if (msg.getReplyToMessageId() != null) {
            messageRepository.findById(msg.getReplyToMessageId()).ifPresent(original -> {
                msg.setReplyToSender(original.getSender());
                msg.setReplyToContent(original.getContent());
            });
        }
        return saved;
    }

    public ChatMessage updateMessage(Long messageId, String newContent) {
        ChatMessage entity = messageRepository.findById(messageId).orElse(null);
        if (entity != null) {
            entity.setContent(newContent);
            entity.setIsEdited(true);
            return messageRepository.save(entity);
        }
        return null;
    }

    public ChatMessage deleteMessage(Long messageId) {
        ChatMessage entity = messageRepository.findById(messageId).orElse(null);
        if (entity != null) {
            entity.setContent(""); 
            entity.setIsDeleted(true);
            ChatMessage saved = messageRepository.save(entity);

            if (entity.getChannelId() != null) {
                try {
                    com.chatsever.grpc.channel.RemovePinnedMessageRequest req = com.chatsever.grpc.channel.RemovePinnedMessageRequest.newBuilder()
                            .setChannelId(entity.getChannelId())
                            .setMessageId(messageId)
                            .setUserId("system")
                            .build();
                    channelServiceClient.removePinnedMessage(req);
                } catch (Exception e) {
                    logger.error("Lỗi khi xóa ghim tin nhắn bị xóa: {}", e.getMessage());
                }
            }
            return saved;
        }
        return null;
    }

    public boolean isMessageOwner(Long messageId, String username) {
        ChatMessage entity = messageRepository.findById(messageId).orElse(null);
        return entity != null && username.equals(entity.getSender());
    }

    public boolean canDeleteMessage(Long messageId, String username, Long serverId) {
        if (isMessageOwner(messageId, username)) {
            return true;
        }
        if (serverId != null) {
            try {
                GetPermissionsRequest req = GetPermissionsRequest.newBuilder().setServerId(serverId).setUserId(username).build();
                GetPermissionsResponse resp = roleServiceClient.getPermissions(req);
                
                int bitmask = resp.getPermissionBitmask();
                return (bitmask & 4) != 0 || (bitmask & 128) != 0 || bitmask == 255;
            } catch (Exception e) {
                logger.error("Error checking delete permission: {}", e.getMessage());
            }
        }
        return false;
    }

    @org.springframework.transaction.annotation.Transactional
    public void addReaction(Long messageId, String userId, String emoji) {
        java.util.Optional<MessageReaction> opt = reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);
        if (opt.isPresent()) {
            reactionRepository.delete(opt.get());
            broadcastReactionEvent(messageId, emoji, "REMOVE", userId);
        } else {
            reactionRepository.save(new MessageReaction(messageId, userId, emoji));
            broadcastReactionEvent(messageId, emoji, "ADD", userId);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void removeReaction(Long messageId, String userId, String emoji) {
        reactionRepository.deleteByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);
        broadcastReactionEvent(messageId, emoji, "REMOVE", userId);
    }

    private void broadcastReactionEvent(Long messageId, String emoji, String action, String userId) {
        messageRepository.findById(messageId).ifPresent(msg -> {
            MessageDTO event = new MessageDTO(MessageType.REACT, userId, null, action + ":" + emoji, LocalDateTime.now());
            event.setMessageId(messageId);
            event.setChannelId(msg.getChannelId());
            event.setServerId(msg.getServerId());
            broadcastToChannel(event);
        });
    }
}

