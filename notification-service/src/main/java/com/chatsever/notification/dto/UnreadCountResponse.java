package com.chatsever.notification.dto;

import java.util.Map;

/**
 * Response cho API unread count — map channelId → số tin chưa đọc.
 */
public class UnreadCountResponse {

    private String userId;
    private Map<Long, Long> unreadCounts;   // channelId → count
    private Map<String, Long> privateCounts; // senderUsername → count

    public UnreadCountResponse() {}

    public UnreadCountResponse(String userId, Map<Long, Long> unreadCounts, Map<String, Long> privateCounts) {
        this.userId = userId;
        this.unreadCounts = unreadCounts;
        this.privateCounts = privateCounts;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Map<Long, Long> getUnreadCounts() { return unreadCounts; }
    public void setUnreadCounts(Map<Long, Long> unreadCounts) { this.unreadCounts = unreadCounts; }
    
    public Map<String, Long> getPrivateCounts() { return privateCounts; }
    public void setPrivateCounts(Map<String, Long> privateCounts) { this.privateCounts = privateCounts; }
}
