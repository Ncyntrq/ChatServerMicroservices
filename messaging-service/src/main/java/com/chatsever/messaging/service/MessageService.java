package com.chatsever.messaging.service;

import com.chatsever.common.dto.LogEntry;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import com.chatsever.messaging.client.RoleClient;
import com.chatsever.messaging.client.ServerServiceClient;
import com.chatsever.messaging.entity.ChatMessage;
import com.chatsever.messaging.repository.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final ServerServiceClient serverServiceClient;
    private final MessageRepository messageRepository;
    private final RoleClient roleClient;
    private final String presenceUrl;

    public MessageService(RestTemplate restTemplate, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, ServerServiceClient serverServiceClient, MessageRepository messageRepository, RoleClient roleClient, @Value("${services.presence-url}") String presenceUrl) {
        this.restTemplate = restTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.serverServiceClient = serverServiceClient;
        this.messageRepository = messageRepository;
        this.roleClient = roleClient;
        this.presenceUrl = presenceUrl;
    }

    // Kiểm tra quyền của User — nếu chưa là member, tự động thêm vào server
    @SuppressWarnings("unchecked")
    public boolean hasPermission(Long serverId, String username) {
        try {
            Map<String, Object> details = serverServiceClient.getServerDetails(serverId);
            if (details != null && details.containsKey("members")) {
                List<Map<String, Object>> members = (List<Map<String, Object>>) details.get("members");
                for (Map<String, Object> member : members) {
                    if (username.equals(member.get("userId"))) {
                        return true;
                    }
                }
            }
            // User chưa là member → tự động thêm vào server
            logger.info("User {} chưa là member của server {} → tự động thêm", username, serverId);
            serverServiceClient.ensureMember(serverId, username);
            return true;
        } catch (Exception e) {
            logger.error("Error checking permission: {}", e.getMessage(), e);
        }
        return false;
    }

    // Gửi tin nhắn Broadcast theo Channel hoặc Global nếu serverId null
    public void broadcastToChannel(MessageDTO msg, Map<String, WebSocketSession> sessions) throws Exception {
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

    @SuppressWarnings("unchecked")
    private Set<String> getServerMembers(Long serverId) {
        try {
            if (serverId == null) return Set.of();
            Map<String, Object> details = serverServiceClient.getServerDetails(serverId);
            if (details != null && details.containsKey("members")) {
                List<Map<String, Object>> members = (List<Map<String, Object>>) details.get("members");
                return members.stream()
                        .map(m -> (String) m.get("userId"))
                        .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            logger.error("Error fetching server members: {}", e.getMessage(), e);
        }
        return Set.of();
    }

    // Gửi tin nhắn riêng (Private)
    public void sendPrivate(MessageDTO msg, WebSocketSession senderSession, Map<String, WebSocketSession> sessions) throws Exception {
        WebSocketSession receiver = sessions.get(msg.getReceiver());
        if (receiver != null && receiver.isOpen()) {
            String payload = objectMapper.writeValueAsString(msg);
            receiver.sendMessage(new TextMessage(payload));
            senderSession.sendMessage(new TextMessage(payload)); // Xác nhận cho người gửi
        } else {
            sendError(senderSession, "User " + msg.getReceiver() + " không online!");
        }
    }

    // Gọi Presence Service báo trạng thái
    public void notifyPresence(String username, String action) {
        try {
            String url = presenceUrl + "/api/presence/" + action + "?username=" + username;
            restTemplate.postForObject(url, null, Void.class);
        } catch (Exception e) {
            logger.error("Error notifying presence: {}", e.getMessage(), e);
        }
    }

    // Bắn Log sang RabbitMQ
    public void publishLogEvent(MessageDTO msg) {
        LogEntry log = new LogEntry(msg.getTimestamp(), msg.getType().name(),
                msg.getSender(), msg.getReceiver(), msg.getContent(),
                msg.getChannelId(), msg.getServerId());
        rabbitTemplate.convertAndSend("chat.exchange", "log." + msg.getType().name().toLowerCase(), log);
    }

    // Publish Notification Event sang RabbitMQ (cho notification-service)
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
        entity.setReceiver(msg.getReceiver()); // Mapped receiver field
        entity.setChannelId(msg.getChannelId());
        entity.setServerId(msg.getServerId());
        entity.setTimestamp(msg.getTimestamp() != null ? msg.getTimestamp() : LocalDateTime.now());
        entity.setType(msg.getType());
        entity.setIsEdited(msg.getIsEdited() != null ? msg.getIsEdited() : false);
        return messageRepository.save(entity);
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

    public void deleteMessage(Long messageId) {
        messageRepository.deleteById(messageId);
    }

    // M6: Kiểm tra quyền sở hữu tin nhắn
    public boolean isMessageOwner(Long messageId, String username) {
        ChatMessage entity = messageRepository.findById(messageId).orElse(null);
        return entity != null && username.equals(entity.getSender());
    }

    // M7: Kiểm tra quyền xóa tin nhắn (Owner HOẶC có quyền MANAGE_MESSAGES/ADMIN)
    public boolean canDeleteMessage(Long messageId, String username, Long serverId) {
        if (isMessageOwner(messageId, username)) {
            return true;
        }
        if (serverId != null) {
            try {
                Map<String, Object> perms = roleClient.getPermissions(serverId, username);
                if (perms != null && perms.containsKey("permissionBitmask")) {
                    int bitmask = (int) perms.get("permissionBitmask");
                    // MANAGE_MESSAGES (4), ADMIN (128), OWNER (255)
                    return (bitmask & 4) != 0 || (bitmask & 128) != 0 || bitmask == 255;
                }
            } catch (Exception e) {
                logger.error("Error checking delete permission: {}", e.getMessage());
            }
        }
        return false;
    }
}