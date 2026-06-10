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

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
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
        
        return ResponseEntity.ok(messages);
    }

    // Lấy nhiều tin nhắn theo IDs (dùng cho tính năng ghim tin nhắn)
    @GetMapping("/bulk")
    public ResponseEntity<List<ChatMessage>> getMessagesBulk(@RequestParam List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(messageRepository.findAllById(ids));
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
        return ResponseEntity.ok(results);
    }
}
