package com.chatsever.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Test serialize/deserialize LogEntry qua Jackson */
class LogEntryTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    // Kiểm tra: serialize → JSON → deserialize lại phải giữ nguyên tất cả field
    @Test
    void roundtripPreservesAllFields() throws Exception {
        LogEntry entry = new LogEntry(
                LocalDateTime.of(2026, 4, 22, 14, 35),
                "BROADCAST",
                "nguyen",
                null,
                "Hello everyone!"
        );

        String json = mapper.writeValueAsString(entry);
        LogEntry decoded = mapper.readValue(json, LogEntry.class);

        assertEquals(entry.getTimestamp(), decoded.getTimestamp());
        assertEquals("BROADCAST", decoded.getEventType());
        assertEquals("nguyen", decoded.getSender());
        assertNull(decoded.getReceiver());
        assertEquals("Hello everyone!", decoded.getContent());
    }
}
