package com.chatsever.presence.model;

import java.time.LocalDateTime;

public class UserStatus {
    private String userId;
    private Status status;
    private LocalDateTime lastSeen;

    // Constructor không đối số (No-args)
    public UserStatus() {}

    // Constructor đầy đủ đối số (All-args) - ĐÂY LÀ DÒNG GIẢI QUYẾT LỖI CỦA BẠN
    public UserStatus(String userId, Status status, LocalDateTime lastSeen) {
        this.userId = userId;
        this.status = status;
        this.lastSeen = lastSeen;
    }

    // Enum trạng thái
    public enum Status {
        ONLINE, OFFLINE, AWAY
    }

    // Getter và Setter (để các module khác đọc được dữ liệu)
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}