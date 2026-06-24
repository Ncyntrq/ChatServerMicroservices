package com.chatsever.messaging.infrastructure.persistence;

import com.chatsever.messaging.domain.model.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, String userId, String emoji);
    void deleteByMessageIdAndUserIdAndEmoji(Long messageId, String userId, String emoji);
}
