package com.chatsever.messaging.entity;

import com.chatsever.common.enums.MessageType;
import jakarta.persistence.*;


import java.time.LocalDateTime;

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

    private String sender;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private String receiver; // For private messages
    
    private Long channelId;
    private Long serverId;
    
    private LocalDateTime timestamp;
    
    @Enumerated(EnumType.STRING)
    private MessageType type;
    
    private Boolean isEdited;
    
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    @Transient
    private String replyToSender;

    @Transient
    private String replyToContent;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "message_id", referencedColumnName = "id", insertable = false, updatable = false)
    private java.util.List<MessageReaction> reactions;

    public ChatMessage() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public Long getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(Long replyToMessageId) { this.replyToMessageId = replyToMessageId; }
    public String getReplyToSender() { return replyToSender; }
    public void setReplyToSender(String replyToSender) { this.replyToSender = replyToSender; }
    public String getReplyToContent() { return replyToContent; }
    public void setReplyToContent(String replyToContent) { this.replyToContent = replyToContent; }
    public java.util.List<MessageReaction> getReactions() { return reactions; }
    public void setReactions(java.util.List<MessageReaction> reactions) { this.reactions = reactions; }
}
