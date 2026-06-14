package com.chatsever.messaging.service;

import com.chatsever.common.dto.LogEntry;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import com.chatsever.messaging.client.RoleClient;
import com.chatsever.messaging.client.ServerServiceClient;
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
    private final MessageReactionRepository reactionRepository;
    private final RoleClient roleClient;
    private final String presenceUrl;
    private final String channelUrl;

    public MessageService(RestTemplate restTemplate, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, ServerServiceClient serverServiceClient, MessageRepository messageRepository, MessageReactionRepository reactionRepository, RoleClient roleClient, @Value("${services.presence-url}") String presenceUrl, @Value("${services.channel-url}") String channelUrl) {
        this.restTemplate = restTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.serverServiceClient = serverServiceClient;
        this.messageRepository = messageRepository;
        this.reactionRepository = reactionRepository;
        this.roleClient = roleClient;
        this.presenceUrl = presenceUrl;
        this.channelUrl = channelUrl;
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

    /**
     * Kiểm tra quyền ĐỌC (tìm kiếm) tin nhắn của 1 channel — KHÔNG side-effect
     * (khác hasPermission: không tự thêm member). Dùng cho message search.
     * - Channel chưa có tin nhắn hoặc serverId null (global) → không ràng buộc.
     * - Ngược lại: chỉ cho phép nếu user là member của server sở hữu channel.
     */
    public boolean canSearchChannel(Long channelId, String username) {
        if (channelId == null) return false;
        List<Long> serverIds = messageRepository.findServerIdsByChannel(
                channelId, org.springframework.data.domain.PageRequest.of(0, 1));
        if (serverIds.isEmpty()) return true; // không có dữ liệu để lộ
        return isServerMember(serverIds.get(0), username);
    }

    @SuppressWarnings("unchecked")
    private boolean isServerMember(Long serverId, String username) {
        if (serverId == null) return true;
        try {
            Map<String, Object> details = serverServiceClient.getServerDetails(serverId);
            if (details != null && details.containsKey("members")) {
                List<Map<String, Object>> members = (List<Map<String, Object>>) details.get("members");
                return members.stream().anyMatch(m -> username.equals(m.get("userId")));
            }
        } catch (Exception e) {
            logger.error("Error checking server membership: {}", e.getMessage());
        }
        return false;
    }

    // Bắn tin nhắn vào RabbitMQ Fanout để phân phối cho tất cả các node WebSocket
    public void broadcastToChannel(MessageDTO msg) {
        rabbitTemplate.convertAndSend("chat.fanout", "", msg);
    }

    // Node cục bộ nhận từ RabbitMQ và đẩy xuống các WebSocket sessions nó đang quản lý
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
        String payload = objectMapper.writeValueAsString(msg);
        
        WebSocketSession receiver = sessions.get(msg.getReceiver());
        if (receiver != null && receiver.isOpen()) {
            receiver.sendMessage(new TextMessage(payload));
        } 
        
        // Luôn echo lại cho người gửi để hiển thị trên màn hình của họ
        if (senderSession != null && senderSession.isOpen()) {
            senderSession.sendMessage(new TextMessage(payload));
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
        entity.setReplyToMessageId(msg.getReplyToMessageId());
        
        ChatMessage saved = messageRepository.save(entity);
        msg.setMessageId(saved.getId());

        // Lấy thông tin tin nhắn gốc để gán vào DTO
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
            entity.setContent(""); // Xóa nội dung thật
            entity.setIsDeleted(true);
            ChatMessage saved = messageRepository.save(entity);

            // Bỏ ghim nếu tin nhắn này đang được ghim
            if (entity.getChannelId() != null) {
                try {
                    String url = channelUrl + "/api/channels/" + entity.getChannelId() + "/pins/" + messageId;
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("X-User-Id", "system"); // Bypass quyền
                    restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
                } catch (Exception e) {
                    logger.error("Lỗi khi xóa ghim tin nhắn bị xóa: {}", e.getMessage());
                }
            }
            return saved;
        }
        return null;
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

    // --- Reactions ---
    @org.springframework.transaction.annotation.Transactional
    public void addReaction(Long messageId, String userId, String emoji) {
        java.util.Optional<MessageReaction> opt = reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);
        if (opt.isPresent()) {
            // Nếu đã thả rồi thì xóa (undo)
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