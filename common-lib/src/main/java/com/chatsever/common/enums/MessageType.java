package com.chatsever.common.enums;

/** Loại tin nhắn qua WebSocket / RabbitMQ */
public enum MessageType {
    CHAT,       // Tin broadcast cho tất cả
    PRIVATE,    // Tin riêng cho 1 người
    SYSTEM,     // Thông báo hệ thống
    ERROR,      // Trả lỗi cho client
    LIST,       // Danh sách user online
    JOIN,       // User vào phòng
    LEAVE,      // User rời phòng
    PING,       // Heartbeat từ client
    PONG        // Server trả lời heartbeat
}
