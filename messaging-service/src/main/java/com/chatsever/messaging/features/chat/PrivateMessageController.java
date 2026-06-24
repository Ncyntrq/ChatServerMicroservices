package com.chatsever.messaging.features.chat;

import com.chatsever.messaging.domain.model.ChatMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages/private")
public class PrivateMessageController {

    private final ChatQueryHandler queryHandler;

    public PrivateMessageController(ChatQueryHandler queryHandler) {
        this.queryHandler = queryHandler;
    }

    @GetMapping
    public ResponseEntity<List<ChatMessage>> getPrivateMessages(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String targetUser,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int limit) {

        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages = queryHandler.getPrivateMessages(userId, targetUser, before, pageable);
        return ResponseEntity.ok(messages);
    }
}
