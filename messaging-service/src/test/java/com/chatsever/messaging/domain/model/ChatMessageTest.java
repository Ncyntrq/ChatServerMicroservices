package com.chatsever.messaging.domain.model;

import com.chatsever.common.enums.MessageType;
import com.chatsever.messaging.domain.vo.MessageContent;
import com.chatsever.messaging.domain.vo.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageTest {

    @Test
    void shouldCreateChannelMessage() {
        UserId sender = new UserId("user1");
        MessageContent content = new MessageContent("Hello channel!");
        Long channelId = 100L;
        Long serverId = 200L;
        
        ChatMessage message = ChatMessage.createChannelMessage(sender, content, channelId, serverId, MessageType.CHAT, null);

        assertEquals("user1", message.getSender());
        assertEquals("Hello channel!", message.getContent());
        assertEquals(channelId, message.getChannelId());
        assertEquals(serverId, message.getServerId());
        assertEquals(MessageType.CHAT, message.getType());
        assertFalse(message.getIsEdited());
        assertFalse(message.getIsDeleted());
        assertNull(message.getReceiver());
    }

    @Test
    void shouldCreatePrivateMessage() {
        UserId sender = new UserId("user1");
        UserId receiver = new UserId("user2");
        MessageContent content = new MessageContent("Hello privately!");
        
        ChatMessage message = ChatMessage.createPrivateMessage(sender, receiver, content, MessageType.PRIVATE, null);

        assertEquals("user1", message.getSender());
        assertEquals("user2", message.getReceiver());
        assertEquals("Hello privately!", message.getContent());
        assertEquals(MessageType.PRIVATE, message.getType());
        assertNull(message.getChannelId());
        assertNull(message.getServerId());
    }

    @Test
    void shouldEditMessageSuccessfully() {
        ChatMessage message = ChatMessage.createChannelMessage(
                new UserId("user1"), new MessageContent("Original"), 100L, 200L, MessageType.CHAT, null);
        
        message.edit(new MessageContent("Edited text"));
        
        assertEquals("Edited text", message.getContent());
        assertTrue(message.getIsEdited());
    }

    @Test
    void shouldThrowExceptionWhenEditingDeletedMessage() {
        ChatMessage message = ChatMessage.createChannelMessage(
                new UserId("user1"), new MessageContent("Original"), 100L, 200L, MessageType.CHAT, null);
        
        message.delete();
        
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> message.edit(new MessageContent("Edited text")));
        assertEquals("Cannot edit a deleted message.", exception.getMessage());
    }

    @Test
    void shouldDeleteMessageSuccessfully() {
        ChatMessage message = ChatMessage.createChannelMessage(
                new UserId("user1"), new MessageContent("Original"), 100L, 200L, MessageType.CHAT, null);
        
        message.delete();
        
        assertTrue(message.getIsDeleted());
        assertEquals("", message.getContent());
    }
}
