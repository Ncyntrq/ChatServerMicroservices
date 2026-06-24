package com.chatsever.messaging.domain.vo;

public record MessageContent(String value) {
    public MessageContent {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (value.length() > 4000) {
            throw new IllegalArgumentException("Message content exceeds maximum length of 4000 characters");
        }
    }
}
