package com.chatsever.messaging.infrastructure.event;

import com.chatsever.common.dto.LogEntry;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.messaging.client.ServerServiceClient;
import com.chatsever.messaging.domain.model.OutboxMessage;
import com.chatsever.messaging.infrastructure.persistence.OutboxMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(MessageEventPublisher.class);
    
    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final ServerServiceClient serverServiceClient;
    private final String presenceUrl;
    private final String channelUrl;

    public MessageEventPublisher(
            OutboxMessageRepository outboxMessageRepository, 
            ObjectMapper objectMapper,
            RestTemplate restTemplate,
            ServerServiceClient serverServiceClient,
            @Value("${services.presence-url}") String presenceUrl,
            @Value("${services.channel-url}") String channelUrl) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.serverServiceClient = serverServiceClient;
        this.presenceUrl = presenceUrl;
        this.channelUrl = channelUrl;
    }

    public void broadcastToChannel(MessageDTO msg) {
        saveOutboxEvent("chat.fanout", "", msg);
    }

    public void publishLogEvent(MessageDTO msg) {
        LogEntry log = new LogEntry(msg.getTimestamp(), msg.getType().name(),
                msg.getSender(), msg.getReceiver(), msg.getContent(),
                msg.getChannelId(), msg.getServerId());
        saveOutboxEvent("chat.exchange", "log." + msg.getType().name().toLowerCase(), log);
    }

    public void publishNotificationEvent(MessageDTO msg) {
        saveOutboxEvent("chat.exchange", "notify.message", msg);
    }

    private void saveOutboxEvent(String exchange, String routingKey, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            OutboxMessage outbox = new OutboxMessage(
                "MESSAGE_EVENT", 
                null, 
                exchange, 
                routingKey, 
                jsonPayload
            );
            outboxMessageRepository.save(outbox);
        } catch (Exception e) {
            logger.error("Error saving outbox event: {}", e.getMessage());
        }
    }

    public void notifyPresence(String username, String action) {
        try {
            String url = presenceUrl + "/api/presence/" + action + "?username=" + username;
            restTemplate.postForObject(url, null, Void.class);
        } catch (Exception e) {
            logger.error("Error notifying presence: {}", e.getMessage(), e);
        }
    }

    public void unpinDeletedMessage(Long channelId, Long messageId) {
        if (channelId != null) {
            try {
                String url = channelUrl + "/api/channels/" + channelId + "/pins/" + messageId;
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-User-Id", "system"); // Bypass quyền
                restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
            } catch (Exception e) {
                logger.error("Lỗi khi xóa ghim tin nhắn bị xóa: {}", e.getMessage());
            }
        }
    }

    public Set<String> getPendingOutboxMessageIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return Set.of();
        List<OutboxMessage> pending = outboxMessageRepository.findByAggregateTypeAndAggregateIdIn("MESSAGE_EVENT", ids);
        return pending.stream().map(OutboxMessage::getAggregateId).collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
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
            // User chưa là member → tự động thêm vào server
            logger.info("User {} chưa là member của server {} → tự động thêm", username, serverId);
            serverServiceClient.ensureMember(serverId, username);
            return true;
        } catch (Exception e) {
            logger.error("Error checking permission: {}", e.getMessage(), e);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getServerMembers(Long serverId) {
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
            logger.error("Error fetching server members: {}", e.getMessage(), e);
        }
        return Set.of();
    }
}
