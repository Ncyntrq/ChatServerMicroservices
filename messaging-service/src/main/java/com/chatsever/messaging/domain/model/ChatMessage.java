package com.chatsever.messaging.domain.model;

import com.chatsever.common.enums.MessageType;
import com.chatsever.messaging.domain.vo.MessageContent;
import com.chatsever.messaging.domain.vo.UserId;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_messages_channel_id", columnList = "channelId"),
    @Index(name = "idx_chat_messages_sender", columnList = "sender"),
    @Index(name = "idx_chat_messages_receiver", columnList = "receiver")
})
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender; // Should ideally map to UserId, but we keep String for DB compatibility
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private String receiver; // For private messages
    
    private Long channelId;
    private Long serverId;
    
    private LocalDateTime timestamp;
    
    @Enumerated(EnumType.STRING)
    private MessageType type;
    
    private Boolean isEdited = false;
    
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    @Transient
    private String replyToSender;

    @Transient
    private String replyToContent;

    @Transient
    private String status = "SENT";

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "message_id", referencedColumnName = "id", insertable = false, updatable = false)
    private List<MessageReaction> reactions = new ArrayList<>();

    protected ChatMessage() {} // For JPA

    // Factory method or rich constructor
    public static ChatMessage createChannelMessage(UserId sender, MessageContent content, Long channelId, Long serverId, MessageType type, Long replyToMessageId) {
        ChatMessage msg = new ChatMessage();
        msg.sender = sender.value();
        msg.content = content.value();
        msg.channelId = channelId;
        msg.serverId = serverId;
        msg.timestamp = LocalDateTime.now();
        msg.type = type != null ? type : MessageType.CHAT;
        msg.replyToMessageId = replyToMessageId;
        return msg;
    }

    public static ChatMessage createPrivateMessage(UserId sender, UserId receiver, MessageContent content, MessageType type, Long replyToMessageId) {
        ChatMessage msg = new ChatMessage();
        msg.sender = sender.value();
        msg.receiver = receiver.value();
        msg.content = content.value();
        msg.timestamp = LocalDateTime.now();
        msg.type = type != null ? type : MessageType.PRIVATE;
        msg.replyToMessageId = replyToMessageId;
        return msg;
    }

    // Business Logic Methods
    public void edit(MessageContent newContent) {
        if (Boolean.TRUE.equals(this.isDeleted)) {
            throw new IllegalStateException("Cannot edit a deleted message.");
        }
        this.content = newContent.value();
        this.isEdited = true;
    }

    public void delete() {
        if (Boolean.TRUE.equals(this.isDeleted)) {
            throw new IllegalStateException("Message is already deleted.");
        }
        this.content = ""; // Clear content for privacy
        this.isDeleted = true;
    }

    public void setReplyInfo(String sender, String content) {
        this.replyToSender = sender;
        this.replyToContent = content;
    }

    public void markAsPending() {
        this.status = "PENDING";
    }

    public boolean isOwnedBy(UserId userId) {
        return this.sender != null && this.sender.equals(userId.value());
    }

    // Getters for serialization/DTO mapping
    public Long getId() { return id; }
    public String getSender() { return sender; }
    public String getContent() { return content; }
    public String getReceiver() { return receiver; }
    public Long getChannelId() { return channelId; }
    public Long getServerId() { return serverId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public MessageType getType() { return type; }
    public Boolean getIsEdited() { return isEdited; }
    public Boolean getIsDeleted() { return isDeleted; }
    public Long getReplyToMessageId() { return replyToMessageId; }
    public String getReplyToSender() { return replyToSender; }
    public String getReplyToContent() { return replyToContent; }
    public String getStatus() { return status; }
    public List<MessageReaction> getReactions() { return reactions; }
}
