package com.chatsever.messaging.infrastructure.websocket;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import com.chatsever.messaging.client.RoleClient;
import com.chatsever.messaging.domain.model.ChatMessage;
import com.chatsever.messaging.features.chat.ChatCommandHandler;
import com.chatsever.messaging.infrastructure.event.MessageEventPublisher;
import com.chatsever.messaging.infrastructure.persistence.MessageRepository;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    private final ChatCommandHandler commandHandler;
    private final MessageEventPublisher eventPublisher;
    private final MessageRepository messageRepository;
    private final RoleClient roleClient;
    private final RestTemplate restTemplate;
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final String authUrl;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(
            ChatCommandHandler commandHandler, 
            MessageEventPublisher eventPublisher,
            MessageRepository messageRepository,
            RoleClient roleClient,
            RestTemplate restTemplate, 
            @Value("${services.auth-url}") String authUrl) {
        this.commandHandler = commandHandler;
        this.eventPublisher = eventPublisher;
        this.messageRepository = messageRepository;
        this.roleClient = roleClient;
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
               eventPublisher.notifyPresence(username, "connect");
               eventPublisher.broadcastToChannel(new MessageDTO(MessageType.JOIN, "SERVER", null, username + " đã vào!", LocalDateTime.now()));
           }
       } catch (Exception e) {
           session.close(new CloseStatus(4001, "Xác thực thất bại: " + e.getMessage()));
       }
    }

    @RabbitListener(queues = "#{broadcastQueue.name}")
    public void handleBroadcastMessage(MessageDTO msg) {
        try {
            sendToLocalSessions(msg);
        } catch (Exception e) {
            logger.error("Error in handleBroadcastMessage", e);
        }
    }

    @RabbitListener(queues = "#{presenceQueue.name}")
    public void handlePresenceStatusEvent(MessageDTO msg) {
        try {
            eventPublisher.broadcastToChannel(msg);
        } catch (Exception e) {
            logger.error("Error in handlePresenceStatusEvent", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        MessageDTO msg = objectMapper.readValue(message.getPayload(), MessageDTO.class);
        msg.setTimestamp(LocalDateTime.now());
        String sender = (String) session.getAttributes().get("username");
        msg.setSender(sender);

        if (msg.getType() == MessageType.CHAT || msg.getType() == MessageType.EDIT || msg.getType() == MessageType.DELETE) {
            if (msg.getServerId() != null && !eventPublisher.hasPermission(msg.getServerId(), sender)) {
                sendError(session, "Không có quyền thực hiện thao tác trong server này");
                return;
            }
        }

        try {
            switch (msg.getType()) {
                case CHAT -> commandHandler.processChatMessage(msg);
                case EDIT -> {
                    if (msg.getMessageId() != null) {
                        if (!isMessageOwner(msg.getMessageId(), sender)) {
                            sendError(session, "Chỉ người gửi mới có thể sửa tin nhắn");
                            return;
                        }
                        commandHandler.processEditMessage(msg);
                    }
                }
                case DELETE -> {
                    if (msg.getMessageId() != null) {
                        if (!canDeleteMessage(msg.getMessageId(), sender, msg.getServerId())) {
                            sendError(session, "Không có quyền xóa tin nhắn này");
                            return;
                        }
                        commandHandler.processDeleteMessage(msg);
                    }
                }
                case TYPING -> eventPublisher.broadcastToChannel(msg);
                case STATUS -> {} 
                case PRIVATE -> {
                    commandHandler.processPrivateMessage(msg);
                    sendPrivate(msg, session);
                }
                case PING -> session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
                default -> {}
            }
        } catch (Exception e) {
            logger.error("Error processing message", e);
            sendError(session, "Lỗi xử lý: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            sessions.remove(username);
            eventPublisher.notifyPresence(username, "disconnect");
            eventPublisher.broadcastToChannel(new MessageDTO(MessageType.LEAVE, "SERVER", null, username + " rời đi!", LocalDateTime.now()));
        }
    }

    private String extractToken(WebSocketSession session) {
        if (session.getUri() == null) return "";
        String query = session.getUri().getQuery();
        return (query != null && query.startsWith("token=")) ? query.substring(6) : "";
    }

    private void sendToLocalSessions(MessageDTO msg) throws Exception {
        Set<String> serverMembers = null;
        if (msg.getServerId() != null) {
            serverMembers = eventPublisher.getServerMembers(msg.getServerId());
        }
        
        String payload = objectMapper.writeValueAsString(msg);
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String username = entry.getKey();
            WebSocketSession s = entry.getValue();
            if (s.isOpen()) {
                if (serverMembers == null || serverMembers.contains(username)) {
                    s.sendMessage(new TextMessage(payload));
                }
            }
        }
    }

    private void sendPrivate(MessageDTO msg, WebSocketSession senderSession) throws Exception {
        String payload = objectMapper.writeValueAsString(msg);
        WebSocketSession receiver = sessions.get(msg.getReceiver());
        if (receiver != null && receiver.isOpen()) {
            receiver.sendMessage(new TextMessage(payload));
        } 
        if (senderSession != null && senderSession.isOpen()) {
            senderSession.sendMessage(new TextMessage(payload));
        }
    }

    private void sendError(WebSocketSession session, String errorMsg) throws Exception {
        MessageDTO error = new MessageDTO(MessageType.ERROR, "SERVER", null, errorMsg, LocalDateTime.now());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
    }

    private boolean isMessageOwner(Long messageId, String username) {
        ChatMessage entity = messageRepository.findById(messageId).orElse(null);
        return entity != null && username.equals(entity.getSender());
    }

    private boolean canDeleteMessage(Long messageId, String username, Long serverId) {
        if (isMessageOwner(messageId, username)) {
            return true;
        }
        if (serverId != null) {
            try {
                Map<String, Object> perms = roleClient.getPermissions(serverId, username);
                if (perms != null && perms.containsKey("permissionBitmask")) {
                    int bitmask = (int) perms.get("permissionBitmask");
                    return (bitmask & 4) != 0 || (bitmask & 128) != 0 || bitmask == 255;
                }
            } catch (Exception e) {
                logger.error("Error checking delete permission: {}", e.getMessage());
            }
        }
        return false;
    }
}
