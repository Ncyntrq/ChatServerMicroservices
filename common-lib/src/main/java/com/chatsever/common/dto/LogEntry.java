package com.chatsever.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * DTO log event — truyền qua RabbitMQ từ messaging-service → log-service.
 * eventType: BROADCAST | PRIVATE | USER_LOGIN | USER_LOGOUT | USER_REGISTER
 */
public class LogEntry {

    // Thời điểm sự kiện. Null → LogService tự gán = now()
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private String eventType;  // Loại sự kiện
    private String sender;     // Người gửi (null nếu system event)
    private String receiver;   // Người nhận (null nếu broadcast)
    private String content;    // Nội dung tin nhắn / mô tả event
    private Long channelId;    // ID channel nguồn (null nếu không liên quan)
    private Long serverId;     // ID server nguồn (null nếu không liên quan)

    // Constructor mặc định — Jackson cần để deserialize
    public LogEntry() {}

    public LogEntry(LocalDateTime timestamp, String eventType, String sender, String receiver, String content) {
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    public LogEntry(LocalDateTime timestamp, String eventType, String sender, String receiver,
                    String content, Long channelId, Long serverId) {
        this(timestamp, eventType, sender, receiver, content);
        this.channelId = channelId;
        this.serverId = serverId;
    }

    // --- Getter & Setter ---

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }

    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
}
