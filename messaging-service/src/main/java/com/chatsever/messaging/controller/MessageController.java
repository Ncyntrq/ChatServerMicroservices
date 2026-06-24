package com.chatsever.messaging.controller;

import com.chatsever.messaging.entity.ChatMessage;
import com.chatsever.messaging.repository.MessageRepository;

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

    public MessageController(MessageRepository messageRepository, com.chatsever.messaging.service.MessageService messageService) {
        this.messageRepository = messageRepository;
        this.messageService = messageService;
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
        populateStatus(messages);
        
        return ResponseEntity.ok(messages);
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

    private void populateStatus(List<ChatMessage> messages) {
        java.util.List<String> ids = messages.stream()
                .map(m -> String.valueOf(m.getId()))
                .collect(java.util.stream.Collectors.toList());
        if (!ids.isEmpty()) {
            java.util.Set<String> pendingIds = messageService.getPendingOutboxMessageIds(ids);
            for (ChatMessage msg : messages) {
                if (pendingIds.contains(String.valueOf(msg.getId()))) {
                    msg.setStatus("PENDING");
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
        List<ChatMessage> results = messageRepository.findAllById(ids);
        populateStatus(results);
        return ResponseEntity.ok(results);
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
        populateStatus(results);
        
        return ResponseEntity.ok(results);
    }

}
