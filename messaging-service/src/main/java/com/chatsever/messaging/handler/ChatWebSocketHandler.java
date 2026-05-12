package com.chatsever.messaging.handler;

import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.dto.LogEntry;
import com.chatsever.common.enums.MessageType;
import com.chatsever.messaging.service.MessageService;
import com.chatsever.messaging.entity.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.chatsever.common.dto.AuthResponse;

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
       try {
           Map<String, String> request = Map.of("token", token);
           AuthResponse response = restTemplate.postForObject(authUrl + "/api/auth/validate", request, AuthResponse.class);
           if(response != null && response.getUsername() != null) {
               String username = response.getUsername();
               sessions.put(username, session);
               session.getAttributes().put("username", username);
               messageService.notifyPresence(username, "connect");
               messageService.broadcast(new MessageDTO(MessageType.JOIN, "SERVER", null, username + " đã vào!", LocalDateTime.now()), sessions);
           }
       } catch (Exception e) {
           session.close(new CloseStatus(4001, "Xác thực thất bại: " + e.getMessage()));
       }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        MessageDTO msg = new ObjectMapper().readValue(message.getPayload(), MessageDTO.class);
        msg.setTimestamp(LocalDateTime.now());
        String sender = (String) session.getAttributes().get("username");
        msg.setSender(sender);

        // Security check for channel/server specific actions
        if (msg.getType() == MessageType.CHAT || msg.getType() == MessageType.EDIT || msg.getType() == MessageType.DELETE) {
            if (msg.getServerId() != null && !messageService.hasPermission(msg.getServerId(), sender)) {
                messageService.sendError(session, "Không có quyền thực hiện thao tác trong server này");
                return;
            }
        }

        switch (msg.getType()) {
            case CHAT -> {
                ChatMessage saved = messageService.saveMessage(msg);
                msg.setMessageId(saved.getId()); // Cập nhật ID cho client biết
                messageService.broadcast(msg, sessions);
            }
            case EDIT -> {
                if (msg.getMessageId() != null) {
                    messageService.updateMessage(msg.getMessageId(), msg.getContent());
                    msg.setIsEdited(true);
                    messageService.broadcast(msg, sessions);
                }
            }
            case DELETE -> {
                if (msg.getMessageId() != null) {
                    messageService.deleteMessage(msg.getMessageId());
                    messageService.broadcast(msg, sessions);
                }
            }
            case TYPING -> messageService.broadcast(msg, sessions);
            case PRIVATE -> messageService.sendPrivate(msg, session, sessions);
            case PING -> session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
            default -> {}
        }
        
        // Log event
        if (msg.getType() != MessageType.TYPING && msg.getType() != MessageType.PING) {
            messageService.publishLogEvent(msg);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            sessions.remove(username);
            messageService.notifyPresence(username, "disconnect");
            messageService.broadcast(new MessageDTO(MessageType.LEAVE, "SERVER", null, username + " rời đi!", LocalDateTime.now()), sessions);
        }
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri().getQuery();
        return (query != null && query.startsWith("token=")) ? query.substring(6) : "";
    }
}