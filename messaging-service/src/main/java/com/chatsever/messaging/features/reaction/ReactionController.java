package com.chatsever.messaging.features.reaction;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class ReactionController {

    private final ReactionCommandHandler commandHandler;

    public ReactionController(ReactionCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @PostMapping("/{messageId}/reactions/{emoji}")
    public ResponseEntity<Void> toggleReaction(
            @PathVariable Long messageId,
            @PathVariable String emoji,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        commandHandler.toggleReaction(messageId, userId, emoji);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{messageId}/reactions/{emoji}")
    public ResponseEntity<Void> removeReaction(
            @PathVariable Long messageId,
            @PathVariable String emoji,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        commandHandler.removeReaction(messageId, userId, emoji);
        return ResponseEntity.ok().build();
    }
}
