package com.chatsever.common.dto;

import com.chatsever.common.enums.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Test serialize/deserialize MessageDTO qua Jackson */
class MessageDTOTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    // Kiểm tra: tin broadcast → receiver null → JSON phải bỏ qua field "receiver"
    @Test
    void serializeBroadcastMessage_omitsNullReceiver() throws Exception {
        MessageDTO msg = new MessageDTO();
        msg.setType(MessageType.CHAT);
        msg.setSender("nguyen");
        msg.setContent("Hello");
        msg.setTimestamp(LocalDateTime.of(2026, 4, 22, 14, 35));

        String json = mapper.writeValueAsString(msg);

        assertTrue(json.contains("\"type\":\"CHAT\""));
        assertTrue(json.contains("\"sender\":\"nguyen\""));
        assertFalse(json.contains("\"receiver\""), "Null receiver phải bị bỏ qua khi serialize");
        assertFalse(json.contains("token"), "MessageDTO KHÔNG được có field token");
    }

    // Kiểm tra: deserialize tin PRIVATE → tất cả field phải có đầy đủ
    @Test
    void deserializePrivateMessage_populatesAllFields() throws Exception {
        String json = """
                {
                  "type": "PRIVATE",
                  "sender": "nguyen",
                  "receiver": "trang",
                  "content": "Hi Trang!",
                  "timestamp": "2026-04-22T14:36:00"
                }
                """;

        MessageDTO msg = mapper.readValue(json, MessageDTO.class);

        assertEquals(MessageType.PRIVATE, msg.getType());
        assertEquals("nguyen", msg.getSender());
        assertEquals("trang", msg.getReceiver());
        assertEquals("Hi Trang!", msg.getContent());
        assertEquals(LocalDateTime.of(2026, 4, 22, 14, 36), msg.getTimestamp());
    }
}
