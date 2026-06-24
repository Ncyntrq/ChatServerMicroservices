package com.chatsever.presence.model;

import java.time.LocalDateTime;

public class UserStatus {
    private String userId;
    private Status status;
    private LocalDateTime lastSeen;

    public UserStatus() {}

    public UserStatus(String userId, Status status, LocalDateTime lastSeen) {
        this.userId = userId;
        this.status = status;
        this.lastSeen = lastSeen;
    }

    // Đã bổ sung thêm IDLE, DO_NOT_DISTURB, INVISIBLE cho đủ chuẩn P5
    public enum Status {
        ONLINE, OFFLINE, AWAY, IDLE, DO_NOT_DISTURB, INVISIBLE
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}
