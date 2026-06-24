package com.chatsever.messaging.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageContentTest {

    @Test
    void shouldCreateValidContent() {
        String validText = "Hello, world!";
        MessageContent content = new MessageContent(validText);
        assertEquals(validText, content.value());
    }

    @Test
    void shouldThrowExceptionWhenNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new MessageContent(null));
        assertEquals("Message content cannot be empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new MessageContent(""));
        assertEquals("Message content cannot be empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new MessageContent("   "));
        assertEquals("Message content cannot be empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTooLong() {
        String longText = "a".repeat(4001);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new MessageContent(longText));
        assertEquals("Message content exceeds maximum length of 4000 characters", exception.getMessage());
    }

    @Test
    void shouldCreateContentWithMaxLength() {
        String maxLengthText = "a".repeat(4000);
        MessageContent content = new MessageContent(maxLengthText);
        assertEquals(maxLengthText, content.value());
    }
}
