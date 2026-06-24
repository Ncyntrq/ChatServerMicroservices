package com.chatsever.messaging.features.chat;

import com.chatsever.messaging.domain.model.ChatMessage;
import com.chatsever.messaging.infrastructure.event.MessageEventPublisher;
import com.chatsever.messaging.infrastructure.persistence.MessageRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatQueryHandler {

    private final MessageRepository messageRepository;
    private final MessageEventPublisher eventPublisher; // We need this to check outbox status

    public ChatQueryHandler(MessageRepository messageRepository, MessageEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<ChatMessage> getChannelMessages(Long channelId, Long before, Pageable pageable) {
        List<ChatMessage> messages;
        if (before != null) {
            messages = messageRepository.findByChannelIdAndIdLessThanOrderByIdDesc(channelId, before, pageable);
        } else {
            messages = messageRepository.findByChannelIdOrderByIdDesc(channelId, pageable);
        }
        
        populateReplyInfo(messages);
        populateStatus(messages);
        return messages;
    }

    public List<ChatMessage> getPrivateMessages(String userId, String targetUser, Long before, Pageable pageable) {
        List<ChatMessage> messages;
        if (before != null) {
            messages = messageRepository.findPrivateMessagesBefore(userId, targetUser, before, pageable);
        } else {
            messages = messageRepository.findPrivateMessages(userId, targetUser, pageable);
        }
        
        populateReplyInfo(messages);
        populateStatus(messages);
        return messages;
    }

    public List<ChatMessage> getMessagesBulk(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<ChatMessage> results = messageRepository.findAllById(ids);
        populateStatus(results);
        return results;
    }

    public void populateReplyInfo(List<ChatMessage> messages) {
        List<Long> replyIds = messages.stream()
                .map(ChatMessage::getReplyToMessageId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (!replyIds.isEmpty()) {
            Map<Long, ChatMessage> replyMap = messageRepository.findAllById(replyIds).stream()
                    .collect(Collectors.toMap(ChatMessage::getId, m -> m));

            for (ChatMessage msg : messages) {
                if (msg.getReplyToMessageId() != null) {
                    ChatMessage original = replyMap.get(msg.getReplyToMessageId());
                    if (original != null) {
                        msg.setReplyInfo(original.getSender(), original.getContent());
                    }
                }
            }
        }
    }

    public void populateStatus(List<ChatMessage> messages) {
        List<String> ids = messages.stream()
                .map(m -> String.valueOf(m.getId()))
                .collect(Collectors.toList());
        if (!ids.isEmpty()) {
            Set<String> pendingIds = eventPublisher.getPendingOutboxMessageIds(ids);
            for (ChatMessage msg : messages) {
                if (pendingIds.contains(String.valueOf(msg.getId()))) {
                    msg.markAsPending();
                }
            }
        }
    }
}
