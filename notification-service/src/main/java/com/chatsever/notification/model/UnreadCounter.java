package com.chatsever.notification.model;

import jakarta.persistence.*;

@Entity
@Table(name = "unread_counter", indexes = {
        @Index(name = "idx_uc_user", columnList = "userId")
})
public class UnreadCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String userId;

    private Long channelId;         // Có thể null nếu là DM

    @Column(length = 100)
    private String senderUsername;  // Tên người gửi nếu là DM (khóa để gộp đếm), null nếu là channel

    @Column(nullable = false)
    private int unreadCount = 0;

    public UnreadCounter() {}

    public UnreadCounter(String userId, Long channelId, String senderUsername, int unreadCount) {
        this.userId = userId;
        this.channelId = channelId;
        this.senderUsername = senderUsername;
        this.unreadCount = unreadCount;
    }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; } public void setUserId(String userId) { this.userId = userId; }
    public Long getChannelId() { return channelId; } public void setChannelId(Long channelId) { this.channelId = channelId; }
    public String getSenderUsername() { return senderUsername; } public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public int getUnreadCount() { return unreadCount; } public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}
