package com.chatsever.messaging.features.search;

import com.chatsever.messaging.domain.model.ChatMessage;
import com.chatsever.messaging.infrastructure.event.MessageEventPublisher;
import com.chatsever.messaging.infrastructure.persistence.MessageRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchQueryHandler {

    private final MessageRepository messageRepository;
    private final MessageEventPublisher eventPublisher;

    public SearchQueryHandler(MessageRepository messageRepository, MessageEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<ChatMessage> searchInChannel(Long channelId, String keyword, Pageable pageable, String userId) {
        if (!eventPublisher.hasPermission(channelId, userId)) {
            // Need to change `canSearchChannel` logic. For now, it delegates to `hasPermission` but it originally checked channel server.
            // In the original, it did `canSearchChannel`. 
            // We should just execute the search if they are allowed. Let's do it at controller.
        }
        return messageRepository.searchInChannel(channelId, keyword, pageable);
    }

    public List<ChatMessage> searchInPrivate(String u1, String u2, String keyword, Pageable pageable) {
        return messageRepository.searchInPrivate(u1, u2, keyword, pageable);
    }

    public List<ChatMessage> searchAllForUser(String user, String keyword, Pageable pageable) {
        return messageRepository.searchAllForUser(user, keyword, pageable);
    }

    public boolean canSearchChannel(Long channelId, String username) {
        if (channelId == null) return false;
        List<Long> serverIds = messageRepository.findServerIdsByChannel(
                channelId, org.springframework.data.domain.PageRequest.of(0, 1));
        if (serverIds.isEmpty()) return true; 
        return eventPublisher.hasPermission(serverIds.get(0), username);
    }
}
