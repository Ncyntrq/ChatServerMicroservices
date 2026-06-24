package com.chatsever.messaging.domain.model;

import com.chatsever.messaging.domain.vo.UserId;
import jakarta.persistence.*;

@Entity
@Table(name = "message_reactions", indexes = {
    @Index(name = "idx_message_reactions_message_id", columnList = "message_id"),
    @Index(name = "idx_message_reactions_unique", columnList = "message_id, user_id, emoji", unique = true)
})
public class MessageReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String emoji;

    @Column(name = "reaction_count", nullable = false)
    private int count = 1;

    protected MessageReaction() {}

    public MessageReaction(Long messageId, UserId userId, String emoji) {
        this.messageId = messageId;
        this.userId = userId.value();
        this.emoji = emoji;
        this.count = 1;
    }

    public Long getId() { return id; }
    public Long getMessageId() { return messageId; }
    public String getUserId() { return userId; }
    public String getEmoji() { return emoji; }
    public int getCount() { return count; }
}
