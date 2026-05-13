package com.chatsever.presence.service;

import com.chatsever.presence.model.UserStatus;
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

    public void connect(String username) {
        UserStatus status = new UserStatus(username, UserStatus.Status.ONLINE, LocalDateTime.now());
        userStatusMap.put(username, status);
        logger.info("[Presence] User connected: {}", username);
    }

    public void disconnect(String username) {
        UserStatus status = new UserStatus(username, UserStatus.Status.OFFLINE, LocalDateTime.now());
        userStatusMap.put(username, status);
        logger.info("[Presence] User disconnected: {}", username);
    }

    public List<String> getOnlineUsers() {
        return userStatusMap.values().stream()
                .filter(user -> user.getStatus() == UserStatus.Status.ONLINE)
                .map(UserStatus::getUserId)
                .collect(Collectors.toList());
    }

    public boolean isOnline(String username) {
        UserStatus status = userStatusMap.get(username);
        return status != null && status.getStatus() == UserStatus.Status.ONLINE;
    }

    public UserStatus getUserStatus(String username) {
        return userStatusMap.getOrDefault(username,
                new UserStatus(username, UserStatus.Status.OFFLINE, LocalDateTime.now()));
    }

    // THÊM MỚI (P5): Hàm đổi trạng thái tùy chỉnh
    public void updateCustomStatus(String username, UserStatus.Status customStatus) {
        UserStatus status = userStatusMap.getOrDefault(username,
                new UserStatus(username, customStatus, LocalDateTime.now()));
        status.setStatus(customStatus);
        status.setLastSeen(LocalDateTime.now());
        userStatusMap.put(username, status);
        logger.info("[Presence] User {} changed status to: {}", username, customStatus);
    }
}