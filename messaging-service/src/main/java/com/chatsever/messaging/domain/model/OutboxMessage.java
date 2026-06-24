package com.chatsever.messaging.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType;
    private String aggregateId;

    private String exchange;
    private String routingKey;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private String status; // PENDING, PROCESSED, FAILED

    private LocalDateTime createdAt;

    protected OutboxMessage() {
    }

    public OutboxMessage(String aggregateType, String aggregateId, String exchange, String routingKey, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.payload = payload;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public void markProcessed() {
        this.status = "PROCESSED";
    }

    public void markFailed() {
        this.status = "FAILED";
    }

    public Long getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getExchange() { return exchange; }
    public String getRoutingKey() { return routingKey; }
    public String getPayload() { return payload; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
