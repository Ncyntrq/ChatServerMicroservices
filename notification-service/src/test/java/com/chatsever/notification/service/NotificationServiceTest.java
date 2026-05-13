package com.chatsever.notification.service;

import com.chatsever.notification.dto.AckRequest;
import com.chatsever.notification.dto.NotificationDTO;
import com.chatsever.notification.dto.UnreadCountResponse;
import com.chatsever.notification.model.Notification;
import com.chatsever.notification.model.NotificationType;
import com.chatsever.notification.model.ReadStatus;
import com.chatsever.notification.repository.NotificationRepository;
import com.chatsever.notification.repository.ReadStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test cho NotificationService.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ReadStatusRepository readStatusRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = new Notification(
                "user1", 1L, 1L, "sender1",
                NotificationType.MENTION, "Hello @user1"
        );
        sampleNotification.setId(1L);
    }

    @Test
    void createNotification_shouldSaveAndReturn() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

        Notification result = notificationService.createNotification(
                "user1", 1L, 1L, "sender1", NotificationType.MENTION, "Hello @user1"
        );

        assertNotNull(result);
        assertEquals("user1", result.getUserId());
        assertEquals(NotificationType.MENTION, result.getType());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void getNotifications_unreadOnly_shouldReturnUnreadOnly() {
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc("user1"))
                .thenReturn(List.of(sampleNotification));

        List<NotificationDTO> result = notificationService.getNotifications("user1", true);

        assertEquals(1, result.size());
        assertEquals("user1", result.get(0).getUserId());
        assertFalse(result.get(0).isRead());
    }

    @Test
    void getNotifications_all_shouldReturnAll() {
        sampleNotification.setRead(true);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("user1"))
                .thenReturn(List.of(sampleNotification));

        List<NotificationDTO> result = notificationService.getNotifications("user1", false);

        assertEquals(1, result.size());
    }

    @Test
    void markAsRead_shouldSetReadTrue() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

        notificationService.markAsRead(1L);

        assertTrue(sampleNotification.isRead());
        verify(notificationRepository).save(sampleNotification);
    }

    @Test
    void acknowledgeChannel_shouldUpdateReadStatus() {
        AckRequest request = new AckRequest("user1", 100L);
        when(readStatusRepository.findByUserIdAndChannelId("user1", 1L))
                .thenReturn(Optional.empty());
        when(readStatusRepository.save(any(ReadStatus.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc("user1"))
                .thenReturn(List.of(sampleNotification));

        notificationService.acknowledgeChannel(1L, request);

        verify(readStatusRepository).save(any(ReadStatus.class));
        assertTrue(sampleNotification.isRead());
    }

    @Test
    void getUnreadCounts_shouldReturnMapOfCounts() {
        Notification n1 = new Notification("user1", 1L, null, "s1", NotificationType.MESSAGE, "msg1");
        Notification n2 = new Notification("user1", 1L, null, "s2", NotificationType.MESSAGE, "msg2");
        Notification n3 = new Notification("user1", 2L, null, "s3", NotificationType.MENTION, "msg3");

        when(notificationRepository.findByUserIdAndIsReadFalse("user1"))
                .thenReturn(List.of(n1, n2, n3));

        UnreadCountResponse response = notificationService.getUnreadCounts("user1");

        assertEquals("user1", response.getUserId());
        assertEquals(2L, response.getUnreadCounts().get(1L));
        assertEquals(1L, response.getUnreadCounts().get(2L));
    }
}
