package com.chatsever.messaging.domain.vo;

public record UserId(String value) {
    public UserId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId cannot be empty");
        }
    }
}
