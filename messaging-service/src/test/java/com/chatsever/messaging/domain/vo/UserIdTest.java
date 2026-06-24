package com.chatsever.messaging.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserIdTest {

    @Test
    void shouldCreateValidUserId() {
        String validUsername = "user123";
        UserId userId = new UserId(validUsername);
        assertEquals(validUsername, userId.value());
    }

    @Test
    void shouldThrowExceptionWhenNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new UserId(null));
        assertEquals("UserId cannot be empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new UserId(""));
        assertEquals("UserId cannot be empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new UserId("   "));
        assertEquals("UserId cannot be empty", exception.getMessage());
    }
}
