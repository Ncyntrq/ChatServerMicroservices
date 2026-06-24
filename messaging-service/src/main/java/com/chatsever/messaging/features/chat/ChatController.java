package com.chatsever.messaging.features.chat;

import com.chatsever.messaging.domain.model.ChatMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
public class ChatController {
    
    private final ChatQueryHandler queryHandler;

    public ChatController(ChatQueryHandler queryHandler) {
        this.queryHandler = queryHandler;
    }
    
    @GetMapping("/{channelId}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @PathVariable Long channelId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int limit) {
            
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages = queryHandler.getChannelMessages(channelId, before, pageable);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/bulk")
    public ResponseEntity<List<ChatMessage>> getMessagesBulk(@RequestParam List<Long> ids) {
        List<ChatMessage> results = queryHandler.getMessagesBulk(ids);
        return ResponseEntity.ok(results);
    }
}
