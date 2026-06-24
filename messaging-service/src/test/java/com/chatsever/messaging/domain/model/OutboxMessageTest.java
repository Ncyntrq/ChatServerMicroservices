package com.chatsever.messaging.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutboxMessageTest {

    @Test
    void shouldCreatePendingOutboxMessage() {
        String aggregateId = "100";
        String aggregateType = "ChatMessage";
        String type = "CHAT_CREATED";
        String payload = "{\"id\":100}";
        String exchange = "chat.exchange";
        String routingKey = "chat.routing.key";
        
        OutboxMessage outbox = new OutboxMessage(aggregateType, aggregateId, exchange, routingKey, payload);
        
        assertEquals("100", outbox.getAggregateId());
        assertEquals("ChatMessage", outbox.getAggregateType());
        assertEquals("{\"id\":100}", outbox.getPayload());
        assertEquals("chat.exchange", outbox.getExchange());
        assertEquals("chat.routing.key", outbox.getRoutingKey());
        assertEquals("PENDING", outbox.getStatus());
        assertNotNull(outbox.getCreatedAt());
    }

    @Test
    void shouldMarkProcessed() {
        OutboxMessage outbox = new OutboxMessage("ChatMessage", "100", "ex", "rk", "{}");
        outbox.markProcessed();
        
        assertEquals("PROCESSED", outbox.getStatus());
    }
}
