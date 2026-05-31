package com.chatsever.messaging.handler;

import com.chatsever.common.dto.MessageDTO;

import com.chatsever.common.enums.MessageType;
import com.chatsever.messaging.service.MessageService;
import com.chatsever.messaging.entity.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
    private final String authUrl;
    private final ObjectMapper objectMapper;


    public ChatWebSocketHandler(MessageService messageService, RestTemplate restTemplate, @Value("${services.auth-url}") String authUrl) {
        this.messageService = messageService;
        this.restTemplate = restTemplate;
        this.authUrl = authUrl;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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
               messageService.broadcastToChannel(new MessageDTO(MessageType.JOIN, "SERVER", null, username + " đã vào!", LocalDateTime.now()), sessions);
           }
       } catch (Exception e) {
           session.close(new CloseStatus(4001, "Xác thực thất bại: " + e.getMessage()));
       }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        MessageDTO msg = objectMapper.readValue(message.getPayload(), MessageDTO.class);
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
                msg.setMessageId(saved.getId());
                messageService.broadcastToChannel(msg, sessions);
                // Publish notification event cho notification-service (N1, N2, N5)
                messageService.publishNotificationEvent(msg);
            }
            case EDIT -> {
                if (msg.getMessageId() != null) {
                    // M6: Kiểm tra người sửa có phải người gửi gốc không
                    if (!messageService.isMessageOwner(msg.getMessageId(), sender)) {
                        messageService.sendError(session, "Chỉ người gửi mới có thể sửa tin nhắn");
                        return;
                    }
                    messageService.updateMessage(msg.getMessageId(), msg.getContent());
                    msg.setIsEdited(true);
                    messageService.broadcastToChannel(msg, sessions);
                }
            }
            case DELETE -> {
                if (msg.getMessageId() != null) {
                    // M7: Người gửi hoặc Admin có thể xóa
                    if (!messageService.canDeleteMessage(msg.getMessageId(), sender, msg.getServerId())) {
                        messageService.sendError(session, "Không có quyền xóa tin nhắn này");
                        return;
                    }
                    messageService.deleteMessage(msg.getMessageId());
                    messageService.broadcastToChannel(msg, sessions);
                }
            }
            case TYPING -> messageService.broadcastToChannel(msg, sessions);
            case PRIVATE -> {
                ChatMessage saved = messageService.saveMessage(msg);
                msg.setMessageId(saved.getId());
                messageService.sendPrivate(msg, session, sessions);
                // Publish DM notification
                messageService.publishNotificationEvent(msg);
            }
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
            messageService.broadcastToChannel(new MessageDTO(MessageType.LEAVE, "SERVER", null, username + " rời đi!", LocalDateTime.now()), sessions);
        }
    }

    private String extractToken(WebSocketSession session) {
        if (session.getUri() == null) return "";
        String query = session.getUri().getQuery();
        return (query != null && query.startsWith("token=")) ? query.substring(6) : "";
    }
}