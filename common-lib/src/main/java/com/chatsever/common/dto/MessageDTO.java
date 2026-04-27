package com.chatsever.common.dto;

import com.chatsever.common.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * DTO tin nhắn WebSocket giữa client ↔ server.
 * Field null sẽ bị bỏ khỏi JSON nhờ @JsonInclude(NON_NULL).
 * KHÔNG chứa token — JWT chỉ truyền qua query param lúc WS handshake.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageDTO {

    private MessageType type;       // Loại tin: CHAT, PRIVATE, SYSTEM, ...
    private String sender;          // Người gửi
    private String receiver;        // Người nhận (null nếu broadcast)
    private String content;         // Nội dung tin nhắn

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
}
