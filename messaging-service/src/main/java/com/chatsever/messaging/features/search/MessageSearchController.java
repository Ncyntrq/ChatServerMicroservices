package com.chatsever.messaging.features.search;

import com.chatsever.messaging.domain.model.ChatMessage;
import com.chatsever.messaging.features.chat.ChatQueryHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages/search")
public class MessageSearchController {

    private static final int MAX_LIMIT = 100;
    private static final int MIN_QUERY_LENGTH = 2;

    private final SearchQueryHandler searchHandler;
    private final ChatQueryHandler chatQueryHandler;

    public MessageSearchController(SearchQueryHandler searchHandler, ChatQueryHandler chatQueryHandler) {
        this.searchHandler = searchHandler;
        this.chatQueryHandler = chatQueryHandler;
    }

    @GetMapping
    public ResponseEntity<List<ChatMessage>> search(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String targetUser,
            @RequestParam(defaultValue = "50") int limit) {

        String keyword = q == null ? "" : q.trim();
        if (keyword.length() < MIN_QUERY_LENGTH) {
            return ResponseEntity.badRequest().build();
        }

        Pageable pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), MAX_LIMIT));
        List<ChatMessage> results;

        switch (scope) {
            case "channel":
                if (channelId == null) return ResponseEntity.badRequest().build();
                if (!searchHandler.canSearchChannel(channelId, userId)) {
                    return ResponseEntity.ok(List.of());
                }
                results = searchHandler.searchInChannel(channelId, keyword, pageable, userId);
                break;
            case "private":
                if (targetUser == null || targetUser.isBlank()) return ResponseEntity.badRequest().build();
                results = searchHandler.searchInPrivate(userId, targetUser, keyword, pageable);
                break;
            default: // "all"
                results = searchHandler.searchAllForUser(userId, keyword, pageable);
                break;
        }

        chatQueryHandler.populateReplyInfo(results);
        chatQueryHandler.populateStatus(results);

        return ResponseEntity.ok(results);
    }
}
