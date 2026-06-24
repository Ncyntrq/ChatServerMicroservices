package com.chatsever.messaging.features.chat;

import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import com.chatsever.messaging.domain.model.ChatMessage;
import com.chatsever.messaging.infrastructure.event.MessageEventPublisher;
import com.chatsever.messaging.infrastructure.persistence.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatCommandHandlerTest {

    private MessageRepository messageRepository;
    private MessageEventPublisher eventPublisher;
    private ChatCommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        eventPublisher = mock(MessageEventPublisher.class);
        commandHandler = new ChatCommandHandler(messageRepository, eventPublisher);
    }

    @Test
    void shouldProcessChatMessage() {
        MessageDTO dto = new MessageDTO();
        dto.setSender("user1");
        dto.setContent("Hello");
        dto.setChannelId(10L);
        dto.setServerId(20L);
        dto.setType(MessageType.CHAT);

        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(i -> {
            ChatMessage msgParam = i.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(msgParam, "id", 100L);
            return msgParam;
        });

        commandHandler.processChatMessage(dto);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository).save(captor.capture());
        ChatMessage savedMsg = captor.getValue();

        assertEquals("user1", savedMsg.getSender());
        assertEquals("Hello", savedMsg.getContent());
        assertEquals(10L, savedMsg.getChannelId());
        
        verify(eventPublisher).broadcastToChannel(dto);
        verify(eventPublisher).publishNotificationEvent(dto);
        verify(eventPublisher).publishLogEvent(dto);
    }

    @Test
    void shouldProcessEditMessage() {
        ChatMessage existingMsg = ChatMessage.createChannelMessage(
                new com.chatsever.messaging.domain.vo.UserId("user1"), 
                new com.chatsever.messaging.domain.vo.MessageContent("Old"), 
                10L, 20L, MessageType.CHAT, null);
        org.springframework.test.util.ReflectionTestUtils.setField(existingMsg, "id", 100L);

        when(messageRepository.findById(100L)).thenReturn(Optional.of(existingMsg));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));

        MessageDTO dto = new MessageDTO();
        dto.setMessageId(100L);
        dto.setContent("New");

        commandHandler.processEditMessage(dto);

        assertEquals("New", existingMsg.getContent());
        assertTrue(existingMsg.getIsEdited());
        verify(eventPublisher).broadcastToChannel(dto);
        verify(eventPublisher).publishLogEvent(dto);
    }

    @Test
    void shouldProcessDeleteMessage() {
        ChatMessage existingMsg = ChatMessage.createChannelMessage(
                new com.chatsever.messaging.domain.vo.UserId("user1"), 
                new com.chatsever.messaging.domain.vo.MessageContent("Old"), 
                10L, 20L, MessageType.CHAT, null);
        org.springframework.test.util.ReflectionTestUtils.setField(existingMsg, "id", 100L);

        when(messageRepository.findById(100L)).thenReturn(Optional.of(existingMsg));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));

        MessageDTO dto = new MessageDTO();
        dto.setMessageId(100L);

        commandHandler.processDeleteMessage(dto);

        assertTrue(existingMsg.getIsDeleted());
        assertEquals("", existingMsg.getContent());
        verify(eventPublisher).unpinDeletedMessage(10L, 100L);
        verify(eventPublisher).broadcastToChannel(dto);
        verify(eventPublisher).publishLogEvent(dto);
    }
}
