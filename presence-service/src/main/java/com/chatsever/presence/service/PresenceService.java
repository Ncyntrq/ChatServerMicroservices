package com.chatsever.presence.service;

import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import com.chatsever.presence.model.UserStatus;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PresenceService {
    private static final Logger logger = LoggerFactory.getLogger(PresenceService.class);
    private final Map<String, UserStatus> userStatusMap = new ConcurrentHashMap<>();
    private final RabbitTemplate rabbitTemplate;

    public PresenceService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void connect(String username) {
        UserStatus status = new UserStatus(username, UserStatus.Status.ONLINE, LocalDateTime.now());
        userStatusMap.put(username, status);
        logger.info("[Presence] User connected: {}", username);
        publishStatusChange(username, UserStatus.Status.ONLINE);
    }

    public void disconnect(String username) {
        UserStatus status = new UserStatus(username, UserStatus.Status.OFFLINE, LocalDateTime.now());
        userStatusMap.put(username, status);
        logger.info("[Presence] User disconnected: {}", username);
        publishStatusChange(username, UserStatus.Status.OFFLINE);
    }

    public List<String> getOnlineUsers() {
        return userStatusMap.values().stream()
                .filter(user -> user.getStatus() == UserStatus.Status.ONLINE
                             || user.getStatus() == UserStatus.Status.IDLE
                             || user.getStatus() == UserStatus.Status.AWAY)
                .map(UserStatus::getUserId)
                .collect(Collectors.toList());
    }

    public boolean isOnline(String username) {
        UserStatus status = userStatusMap.get(username);
        return status != null && status.getStatus() != UserStatus.Status.OFFLINE
                              && status.getStatus() != UserStatus.Status.INVISIBLE;
    }

    public UserStatus getUserStatus(String username) {
        return userStatusMap.getOrDefault(username,
                new UserStatus(username, UserStatus.Status.OFFLINE, LocalDateTime.now()));
    }

    /**
     * P5 — Đổi trạng thái tùy chỉnh (ONLINE, IDLE, AWAY, DO_NOT_DISTURB, INVISIBLE).
     * Sau khi cập nhật, publish event STATUS lên chat.exchange → messaging-service
     * sẽ broadcast xuống toàn bộ WebSocket clients.
     */
    public void updateCustomStatus(String username, UserStatus.Status customStatus) {
        UserStatus status = userStatusMap.getOrDefault(username,
                new UserStatus(username, customStatus, LocalDateTime.now()));
        status.setStatus(customStatus);
        status.setLastSeen(LocalDateTime.now());
        userStatusMap.put(username, status);
        logger.info("[Presence] User {} changed status to: {}", username, customStatus);
        publishStatusChange(username, customStatus);
    }

    /**
     * Publish sự kiện đổi trạng thái lên RabbitMQ.
     * messaging-service lắng nghe routing key "presence.status" và broadcast
     * MessageDTO(type=STATUS, sender=username, content=statusName) tới tất cả clients.
     */
    private void publishStatusChange(String username, UserStatus.Status newStatus) {
        try {
            MessageDTO msg = new MessageDTO();
            msg.setType(MessageType.STATUS);
            msg.setSender(username);
            msg.setContent(newStatus.name()); // "ONLINE" | "IDLE" | "AWAY" | "DO_NOT_DISTURB" | "INVISIBLE" | "OFFLINE"
            msg.setTimestamp(LocalDateTime.now());
            rabbitTemplate.convertAndSend("chat.exchange", "presence.status", msg);
            logger.debug("[Presence] Published STATUS event: {} → {}", username, newStatus);
        } catch (Exception e) {
            logger.error("[Presence] Failed to publish status event: {}", e.getMessage());
        }
    }
}