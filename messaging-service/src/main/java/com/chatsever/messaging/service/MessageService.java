package com.chatsever.messaging.service;

import com.chatsever.common.dto.LogEntry;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class MessageService {
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.presence-url}") private String presenceUrl;

    public MessageService(RestTemplate restTemplate, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    // Gửi cho tất cả mọi người (Broadcast)
    public void broadcast(MessageDTO msg, Map<String, WebSocketSession> sessions) throws Exception {
        String payload = objectMapper.writeValueAsString(msg);
        for (WebSocketSession s : sessions.values()) {
            if (s.isOpen()) s.sendMessage(new TextMessage(payload));
        }
    }

    // Gửi tin nhắn riêng (Private)
    public void sendPrivate(MessageDTO msg, WebSocketSession senderSession, Map<String, WebSocketSession> sessions) throws Exception {
        WebSocketSession receiver = sessions.get(msg.getReceiver());
        if (receiver != null && receiver.isOpen()) {
            String payload = objectMapper.writeValueAsString(msg);
            receiver.sendMessage(new TextMessage(payload));
            senderSession.sendMessage(new TextMessage(payload)); // Xác nhận cho người gửi
        } else {
            sendError(senderSession, "User " + msg.getReceiver() + " không online!");
        }
    }

    // Gọi Presence Service báo trạng thái
    public void notifyPresence(String username, String action) {
        String url = presenceUrl + "/api/presence/" + action + "?username=" + username;
        restTemplate.postForObject(url, null, Void.class);
    }

    // Bắn Log sang RabbitMQ
    public void publishLogEvent(MessageDTO msg) {
        LogEntry log = new LogEntry(msg.getTimestamp(), msg.getType().name(), msg.getSender(), msg.getReceiver(), msg.getContent());
        rabbitTemplate.convertAndSend("chat.exchange", "log." + msg.getType().name().toLowerCase(), log);
    }

    public void sendError(WebSocketSession session, String errorMsg) throws Exception {
        MessageDTO error = new MessageDTO(MessageType.ERROR, "SERVER", null, errorMsg, LocalDateTime.now());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
    }
}