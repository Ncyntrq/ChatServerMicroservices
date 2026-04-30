package com.chatsever.presence.service;

import com.chatsever.presence.model.UserStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PresenceService {
    
    // Sử dụng ConcurrentHashMap để tránh lỗi khi có nhiều request đồng thời
    private final Map<String, UserStatus> userStatusMap = new ConcurrentHashMap<>();

    // 1. connect() - Đánh dấu user online
    public void connect(String username) {
        UserStatus status = new UserStatus(username, UserStatus.Status.ONLINE, LocalDateTime.now());
        userStatusMap.put(username, status);
        System.out.println("[Presence] User connected: " + username);
        // Phase tiếp theo: Bắn sự kiện "User Online" vào RabbitMQ tại đây
    }

    // 2. disconnect() - Đánh dấu user offline
    public void disconnect(String username) {
        UserStatus status = new UserStatus(username, UserStatus.Status.OFFLINE, LocalDateTime.now());
        userStatusMap.put(username, status);
        System.out.println("[Presence] User disconnected: " + username);
        // Phase tiếp theo: Bắn sự kiện "User Offline" vào RabbitMQ tại đây
    }

    // 3. getOnlineUsers() - Lấy danh sách những ai đang online
    public List<String> getOnlineUsers() {
        return userStatusMap.values().stream()
                .filter(user -> user.getStatus() == UserStatus.Status.ONLINE)
                .map(UserStatus::getUserId) // Dùng getUserId() theo đúng model của bạn
                .collect(Collectors.toList());
    }

    // 4. isOnline() - Kiểm tra nhanh xem 1 user có đang online không
    public boolean isOnline(String username) {
        UserStatus status = userStatusMap.get(username);
        return status != null && status.getStatus() == UserStatus.Status.ONLINE;
    }

    // Hàm bổ trợ cho API GET /status/{username} của Controller
    public UserStatus getUserStatus(String username) {
        return userStatusMap.getOrDefault(username, 
            new UserStatus(username, UserStatus.Status.OFFLINE, LocalDateTime.now()));
    }
}