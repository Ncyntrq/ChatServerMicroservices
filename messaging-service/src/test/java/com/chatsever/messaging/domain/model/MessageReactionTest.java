package com.chatsever.messaging.domain.model;

import com.chatsever.messaging.domain.vo.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageReactionTest {

    @Test
    void shouldCreateReaction() {
        ChatMessage message = new ChatMessage();
        UserId user = new UserId("user1");
        String emoji = "👍";
        
        MessageReaction reaction = new MessageReaction(100L, user, emoji);
        
        assertEquals(100L, reaction.getMessageId());
        assertEquals("user1", reaction.getUserId());
        assertEquals("👍", reaction.getEmoji());
        assertEquals(1, reaction.getCount());
    }
}
