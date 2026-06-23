package com.chatsever.common.dto;

import com.chatsever.common.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO tin nhắn WebSocket giữa client ↔ server.
 * Field null sẽ bị bỏ khỏi JSON nhờ @JsonInclude(NON_NULL).
 * KHÔNG chứa token — JWT chỉ truyền qua query param lúc WS handshake.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDTO {

    private MessageType type;       // Loại tin: CHAT, PRIVATE, SYSTEM, ...
    private String sender;          // Người gửi
    private String receiver;        // Người nhận (null nếu broadcast)
    private String content;         // Nội dung tin nhắn
    private Long channelId;         // ID của channel
    private Long serverId;          // ID của server
    private Boolean isEdited;       // Cờ đánh dấu tin nhắn đã chỉnh sửa
    private Boolean isDeleted;      // Cờ đánh dấu tin nhắn đã bị thu hồi/gỡ
    private Long messageId;         // ID của tin nhắn (dùng cho EDIT/DELETE)
    private Long replyToMessageId;  // ID của tin nhắn đang trả lời
    private String replyToSender;   // Tên người gửi tin nhắn gốc
    private String replyToContent;  // Nội dung tin nhắn gốc được trích dẫn

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    // Constructor mặc định — Jackson cần để deserialize
    public MessageDTO() {}

    public MessageDTO(MessageType type, String sender, String receiver, String content, LocalDateTime timestamp) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = timestamp;
    }

    // --- Getter & Setter ---

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }

    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }

    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public Long getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(Long replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public String getReplyToSender() { return replyToSender; }
    public void setReplyToSender(String replyToSender) { this.replyToSender = replyToSender; }

    public String getReplyToContent() { return replyToContent; }
    public void setReplyToContent(String replyToContent) { this.replyToContent = replyToContent; }

    private java.util.List<ReactionDTO> reactions;
    public java.util.List<ReactionDTO> getReactions() { return reactions; }
    public void setReactions(java.util.List<ReactionDTO> reactions) { this.reactions = reactions; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReactionDTO {
        private String userId;
        private String emoji;
        private int count = 1;

        public ReactionDTO() {}

        public ReactionDTO(String userId, String emoji) {
            this.userId = userId;
            this.emoji = emoji;
            this.count = 1;
        }

        public ReactionDTO(String userId, String emoji, int count) {
            this.userId = userId;
            this.emoji = emoji;
            this.count = count;
        }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getEmoji() { return emoji; }
        public void setEmoji(String emoji) { this.emoji = emoji; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }
}
