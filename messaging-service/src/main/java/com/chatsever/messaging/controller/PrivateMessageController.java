package com.chatsever.messaging.controller;

import com.chatsever.messaging.entity.ChatMessage;
import com.chatsever.messaging.repository.MessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages/private")
public class PrivateMessageController {

    private final MessageRepository messageRepository;

    public PrivateMessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @GetMapping
    public ResponseEntity<List<ChatMessage>> getPrivateMessages(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String targetUser,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int limit) {

        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages;

        if (before != null) {
            messages = messageRepository.findPrivateMessagesBefore(userId, targetUser, before, pageable);
        } else {
            messages = messageRepository.findPrivateMessages(userId, targetUser, pageable);
        }

        return ResponseEntity.ok(messages);
    }
}
