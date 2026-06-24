package com.chatsever.messaging.features.reaction;

import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import com.chatsever.messaging.domain.model.MessageReaction;
import com.chatsever.messaging.domain.vo.UserId;
import com.chatsever.messaging.infrastructure.event.MessageEventPublisher;
import com.chatsever.messaging.infrastructure.persistence.MessageReactionRepository;
import com.chatsever.messaging.infrastructure.persistence.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReactionCommandHandler {

    private final MessageReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final MessageEventPublisher eventPublisher;

    public ReactionCommandHandler(MessageReactionRepository reactionRepository, 
                                  MessageRepository messageRepository, 
                                  MessageEventPublisher eventPublisher) {
        this.reactionRepository = reactionRepository;
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void toggleReaction(Long messageId, String userIdStr, String emoji) {
        UserId userId = new UserId(userIdStr);
        java.util.Optional<MessageReaction> opt = reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId.value(), emoji);
        
        if (opt.isPresent()) {
            reactionRepository.delete(opt.get());
            broadcastReactionEvent(messageId, emoji, "REMOVE", userId.value());
        } else {
            MessageReaction reaction = new MessageReaction(messageId, userId, emoji);
            reactionRepository.save(reaction);
            broadcastReactionEvent(messageId, emoji, "ADD", userId.value());
        }
    }

    @Transactional
    public void removeReaction(Long messageId, String userIdStr, String emoji) {
        UserId userId = new UserId(userIdStr);
        reactionRepository.deleteByMessageIdAndUserIdAndEmoji(messageId, userId.value(), emoji);
        broadcastReactionEvent(messageId, emoji, "REMOVE", userId.value());
    }

    private void broadcastReactionEvent(Long messageId, String emoji, String action, String userId) {
        messageRepository.findById(messageId).ifPresent(msg -> {
            MessageDTO event = new MessageDTO(MessageType.REACT, userId, null, action + ":" + emoji, LocalDateTime.now());
            event.setMessageId(messageId);
            event.setChannelId(msg.getChannelId());
            event.setServerId(msg.getServerId());
            eventPublisher.broadcastToChannel(event);
        });
    }
}
