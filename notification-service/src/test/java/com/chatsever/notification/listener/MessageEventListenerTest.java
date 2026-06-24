package com.chatsever.notification.listener;

import com.chatsever.notification.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import com.chatsever.notification.model.Notification;
import com.chatsever.notification.model.NotificationType;
import com.chatsever.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho MessageEventListener — test parsing @mention và tạo notification.
 */
@ExtendWith(MockitoExtension.class)
class MessageEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MessageEventListener listener;

    @Test
    void parseMentions_singleUser() {
        List<String> mentions = listener.parseMentions("Hello @alice how are you?");
        assertEquals(1, mentions.size());
        assertEquals("alice", mentions.get(0));
    }

    @Test
    void parseMentions_multipleUsers() {
        List<String> mentions = listener.parseMentions("@alice @bob please check this");
        assertEquals(2, mentions.size());
        assertTrue(mentions.contains("alice"));
        assertTrue(mentions.contains("bob"));
    }

    @Test
    void parseMentions_everyone() {
        List<String> mentions = listener.parseMentions("@everyone meeting at 3pm");
        assertEquals(1, mentions.size());
        assertEquals("everyone", mentions.get(0));
    }

    @Test
    void parseMentions_noDuplicates() {
        List<String> mentions = listener.parseMentions("@alice said hi to @alice");
        assertEquals(1, mentions.size());
    }

    @Test
    void parseMentions_noMentions() {
        List<String> mentions = listener.parseMentions("Just a normal message");
        assertTrue(mentions.isEmpty());
    }

    @Test
    void parseMentions_nullContent() {
        List<String> mentions = listener.parseMentions(null);
        assertTrue(mentions.isEmpty());
    }

    @Test
    void onMessage_withMention_shouldCreateMentionNotification() {
        MessageDTO msg = new MessageDTO(MessageType.CHAT, "sender1", null,
                "Hello @user1", LocalDateTime.now());

        when(notificationService.createNotification(anyString(), any(), any(), anyString(),
                any(NotificationType.class), anyString()))
                .thenReturn(new Notification());

        listener.onMessage(msg);

        verify(notificationService).createNotification(
                eq("user1"), isNull(), isNull(), eq("sender1"),
                eq(NotificationType.MENTION), eq("Hello @user1")
        );
    }

    @Test
    void onMessage_withEveryone_shouldCreateMentionAllNotification() {
        MessageDTO msg = new MessageDTO(MessageType.CHAT, "sender1", null,
                "@everyone check this", LocalDateTime.now());

        when(notificationService.createNotification(anyString(), any(), any(), anyString(),
                any(NotificationType.class), anyString()))
                .thenReturn(new Notification());

        listener.onMessage(msg);

        verify(notificationService).createNotification(
                eq("ALL"), isNull(), isNull(), eq("sender1"),
                eq(NotificationType.MENTION_ALL), eq("@everyone check this")
        );
    }

    @Test
    void onMessage_withReceiver_shouldCreateDMNotification() {
        MessageDTO msg = new MessageDTO(MessageType.PRIVATE, "sender1", "receiver1",
                "Private message", LocalDateTime.now());

        when(notificationService.createNotification(anyString(), any(), any(), anyString(),
                any(NotificationType.class), anyString()))
                .thenReturn(new Notification());

        listener.onMessage(msg);

        verify(notificationService).createNotification(
                eq("receiver1"), isNull(), isNull(), eq("sender1"),
                eq(NotificationType.DM), eq("Private message")
        );
    }

    @Test
    void onMessage_senderMentionsSelf_shouldNotCreateSelfNotification() {
        MessageDTO msg = new MessageDTO(MessageType.CHAT, "alice", null,
                "I @alice am here", LocalDateTime.now());

        listener.onMessage(msg);

        // Should NOT create a mention notification for alice (self-mention)
        verify(notificationService, never()).createNotification(
                eq("alice"), any(), any(), anyString(),
                eq(NotificationType.MENTION), anyString()
        );
    }

    @Test
    void onMessage_nullContent_shouldSkip() {
        MessageDTO msg = new MessageDTO(MessageType.CHAT, "sender1", null,
                null, LocalDateTime.now());

        listener.onMessage(msg);

        verifyNoInteractions(notificationService);
    }
}


