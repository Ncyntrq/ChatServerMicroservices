package com.chatsever.messaging.service;

import com.chatsever.common.dto.LogEntry;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import com.chatsever.messaging.client.ServerServiceClient;
import com.chatsever.messaging.entity.ChatMessage;
import com.chatsever.messaging.repository.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageService {
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final ServerServiceClient serverServiceClient;
    private final MessageRepository messageRepository;

    @Value("${services.presence-url}") private String presenceUrl;

    public MessageService(RestTemplate restTemplate, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, ServerServiceClient serverServiceClient, MessageRepository messageRepository) {
        this.restTemplate = restTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.serverServiceClient = serverServiceClient;
        this.messageRepository = messageRepository;
    }

    // Kiểm tra quyền của User
    public boolean hasPermission(Long serverId, String username) {
        try {
            Map<String, Object> details = serverServiceClient.getServerDetails(serverId);
            if (details != null && details.containsKey("members")) {
                List<Map<String, Object>> members = (List<Map<String, Object>>) details.get("members");
                for (Map<String, Object> member : members) {
                    if (username.equals(member.get("userId"))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking permission: " + e.getMessage());
        }
        return false;
    }

    // Gửi tin nhắn Broadcast theo Channel hoặc Global nếu serverId null
    public void broadcast(MessageDTO msg, Map<String, WebSocketSession> sessions) throws Exception {
        Set<String> channelMembers = null;
        if (msg.getServerId() != null) {
            channelMembers = getChannelMembers(msg.getServerId());
        }
        
        String payload = objectMapper.writeValueAsString(msg);
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String username = entry.getKey();
            WebSocketSession s = entry.getValue();
            if (s.isOpen()) {
                if (channelMembers == null || channelMembers.contains(username)) {
                    s.sendMessage(new TextMessage(payload));
                }
            }
        }
    }

    private Set<String> getChannelMembers(Long serverId) {
        try {
            if (serverId == null) return Set.of();
            Map<String, Object> details = serverServiceClient.getServerDetails(serverId);
            if (details != null && details.containsKey("members")) {
                List<Map<String, Object>> members = (List<Map<String, Object>>) details.get("members");
                return members.stream()
                        .map(m -> (String) m.get("userId"))
                        .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            System.err.println("Error fetching server members: " + e.getMessage());
        }
        return Set.of();
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
        try {
            String url = presenceUrl + "/api/presence/" + action + "?username=" + username;
            restTemplate.postForObject(url, null, Void.class);
        } catch (Exception e) {
            System.err.println("Error notifying presence: " + e.getMessage());
        }
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

    public ChatMessage saveMessage(MessageDTO msg) {
        ChatMessage entity = new ChatMessage();
        entity.setSender(msg.getSender());
        entity.setContent(msg.getContent());
        entity.setChannelId(msg.getChannelId());
        entity.setServerId(msg.getServerId());
        entity.setTimestamp(msg.getTimestamp() != null ? msg.getTimestamp() : LocalDateTime.now());
        entity.setType(msg.getType());
        entity.setIsEdited(msg.getIsEdited() != null ? msg.getIsEdited() : false);
        return messageRepository.save(entity);
    }

    public ChatMessage updateMessage(Long messageId, String newContent) {
        ChatMessage entity = messageRepository.findById(messageId).orElse(null);
        if (entity != null) {
            entity.setContent(newContent);
            entity.setIsEdited(true);
            return messageRepository.save(entity);
        }
        return null;
    }

    public void deleteMessage(Long messageId) {
        messageRepository.deleteById(messageId);
    }
}