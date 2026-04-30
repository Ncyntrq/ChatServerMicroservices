package com.chatsever.messaging.handler;

import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.dto.LogEntry; // Đảm bảo đã import cái này
import com.chatsever.common.enums.MessageType;
import com.chatsever.messaging.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final MessageService messageService;
    private final RestTemplate restTemplate;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    @Value("${services.auth-url}") private String authUrl;

    public ChatWebSocketHandler(MessageService messageService, RestTemplate restTemplate) {
        this.messageService = messageService;
        this.restTemplate = restTemplate;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session);
        // Xác thực qua Auth Service
        Map<String, Object> res = restTemplate.postForObject(authUrl + "/api/auth/validate", Map.of("token", token), Map.class);

        if (res != null && (Boolean) res.get("valid")) {
            String username = (String) res.get("username");
            sessions.put(username, session);
            session.getAttributes().put("username", username);

            messageService.notifyPresence(username, "connect"); // Gọi service báo online
            messageService.broadcast(new MessageDTO(MessageType.JOIN, "SERVER", null, username + " tham gia!", LocalDateTime.now()), sessions);
        } else {
            session.close(new CloseStatus(4001, "Invalid token"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        MessageDTO msg = new ObjectMapper().readValue(message.getPayload(), MessageDTO.class);
        msg.setTimestamp(LocalDateTime.now());
        msg.setSender((String) session.getAttributes().get("username"));

        // Dùng Service để xử lý logic
        switch (msg.getType()) {
            case CHAT -> messageService.broadcast(msg, sessions);
            case PRIVATE -> messageService.sendPrivate(msg, session, sessions);
            case PING -> session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
        }
        messageService.publishLogEvent(msg); // Bắn log bất đồng bộ
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            sessions.remove(username);
            messageService.notifyPresence(username, "disconnect"); // Gọi service báo offline
            messageService.broadcast(new MessageDTO(MessageType.LEAVE, "SERVER", null, username + " rời đi!", LocalDateTime.now()), sessions);
        }
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri().getQuery();
        return (query != null && query.startsWith("token=")) ? query.substring(6) : "";
    }
}