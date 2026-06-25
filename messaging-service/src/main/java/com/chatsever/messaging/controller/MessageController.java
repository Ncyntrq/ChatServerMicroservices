package com.chatsever.messaging.controller;

import com.chatsever.messaging.entity.ChatMessage;
import com.chatsever.messaging.repository.MessageRepository;
import com.chatsever.messaging.repository.OutboxMessageRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
public class MessageController {
    
    private final MessageRepository messageRepository;
    private final com.chatsever.messaging.service.MessageService messageService;
    private final OutboxMessageRepository outboxRepository;

    public MessageController(MessageRepository messageRepository, com.chatsever.messaging.service.MessageService messageService, OutboxMessageRepository outboxRepository) {
        this.messageRepository = messageRepository;
        this.messageService = messageService;
        this.outboxRepository = outboxRepository;
    }
    
    @GetMapping("/{channelId}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @PathVariable Long channelId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int limit) {
            
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages;
        
        if (before != null) {
            messages = messageRepository.findByChannelIdAndIdLessThanOrderByIdDesc(channelId, before, pageable);
        } else {
            messages = messageRepository.findByChannelIdOrderByIdDesc(channelId, pageable);
        }
        
        populateReplyInfo(messages);
        populateOutboxStatus(messages);
        
        return ResponseEntity.ok(messages);
    }

    private void populateOutboxStatus(List<ChatMessage> messages) {
        if (messages.isEmpty()) return;
        List<String> msgIds = messages.stream().map(m -> String.valueOf(m.getId())).toList();
        List<com.chatsever.messaging.entity.OutboxMessage> pending = outboxRepository.findByAggregateIdInAndStatus(msgIds, "PENDING");
        java.util.Set<Long> pendingIds = pending.stream().map(o -> Long.parseLong(o.getAggregateId())).collect(java.util.stream.Collectors.toSet());
        for (ChatMessage msg : messages) {
            if (pendingIds.contains(msg.getId())) {
                msg.setStatus("SENDING");
            } else {
                msg.setStatus("SENT");
            }
        }
    }

    private void populateReplyInfo(List<ChatMessage> messages) {
        java.util.List<Long> replyIds = messages.stream()
                .map(ChatMessage::getReplyToMessageId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        if (!replyIds.isEmpty()) {
            java.util.Map<Long, ChatMessage> replyMap = messageRepository.findAllById(replyIds).stream()
                    .collect(java.util.stream.Collectors.toMap(ChatMessage::getId, m -> m));

            for (ChatMessage msg : messages) {
                if (msg.getReplyToMessageId() != null) {
                    ChatMessage original = replyMap.get(msg.getReplyToMessageId());
                    if (original != null) {
                        msg.setReplyToSender(original.getSender());
                        msg.setReplyToContent(original.getContent());
                    }
                }
            }
        }
    }

    // Lấy nhiều tin nhắn theo IDs (dùng cho tính năng ghim tin nhắn)
    @GetMapping("/bulk")
    public ResponseEntity<List<ChatMessage>> getMessagesBulk(@RequestParam List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<ChatMessage> messages = messageRepository.findAllById(ids);
        populateOutboxStatus(messages);
        return ResponseEntity.ok(messages);
    }

    // Tìm kiếm tin nhắn theo từ khóa
    @GetMapping("/search")
    public ResponseEntity<List<ChatMessage>> searchMessages(
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) Long serverId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "50") int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> results;
        if (channelId != null) {
            results = messageRepository.searchByChannel(channelId, keyword, pageable);
        } else if (serverId != null) {
            results = messageRepository.searchByServer(serverId, keyword, pageable);
        } else {
            results = List.of();
        }
        
        populateReplyInfo(results);
        populateOutboxStatus(results);
        
        return ResponseEntity.ok(results);
    }

}
