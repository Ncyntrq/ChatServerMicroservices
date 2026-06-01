package com.chatsever.notification.service;

import com.chatsever.notification.dto.AckRequest;
import com.chatsever.notification.dto.NotificationDTO;
import com.chatsever.notification.dto.UnreadCountResponse;
import com.chatsever.notification.model.Notification;
import com.chatsever.notification.model.NotificationType;
import com.chatsever.notification.model.ReadStatus;
import com.chatsever.notification.repository.NotificationRepository;
import com.chatsever.notification.repository.ReadStatusRepository;
import com.chatsever.notification.repository.UnreadCounterRepository;
import com.chatsever.notification.model.UnreadCounter;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business logic cho notification:
 * - Tạo notification (mention, message, DM)
 * - Đếm unread count
 * - Đánh dấu đã đọc
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final ReadStatusRepository readStatusRepository;
    private final UnreadCounterRepository unreadCounterRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public NotificationService(NotificationRepository notificationRepository,
                               ReadStatusRepository readStatusRepository,
                               UnreadCounterRepository unreadCounterRepository) {
        this.notificationRepository = notificationRepository;
        this.readStatusRepository = readStatusRepository;
        this.unreadCounterRepository = unreadCounterRepository;
    }

    /**
     * Tạo 1 notification cho user cụ thể.
     */
    @Transactional
    public Notification createNotification(String userId, Long channelId, Long serverId,
                                           String sender, NotificationType type, String content) {
        Notification notification = new Notification(userId, channelId, serverId, sender, type, content);
        Notification saved = notificationRepository.save(notification);
        log.info("Tạo notification: userId={}, type={}, sender={}", userId, type, sender);
        return saved;
    }

    /**
     * Lấy danh sách notification theo userId.
     * Nếu unreadOnly=true → chỉ lấy chưa đọc.
     */
    public List<NotificationDTO> getNotifications(String userId, boolean unreadOnly) {
        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        } else {
            notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return notifications.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // NF13 — Paginated version
    public Page<NotificationDTO> getNotifications(String userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page;
        if (unreadOnly) {
            page = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        } else {
            page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return page.map(this::toDTO);
    }

    /**
     * Đánh dấu 1 notification đã đọc.
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
            log.debug("Marked notification {} as read", notificationId);
        });
    }

    /**
     * Đánh dấu đã đọc cho channel — cập nhật read_status + mark notifications.
     * POST /api/channels/{channelId}/ack
     */
    @Transactional
    public void acknowledgeChannel(Long channelId, AckRequest request) {
        String userId = request.getUserId();
        Long lastMessageId = request.getLastMessageId();

        // Cập nhật hoặc tạo read_status
        ReadStatus readStatus = readStatusRepository
                .findByUserIdAndChannelId(userId, channelId)
                .orElse(new ReadStatus(userId, channelId, 0L));

        readStatus.setLastReadMsgId(lastMessageId);
        readStatus.setLastReadAt(LocalDateTime.now());
        readStatusRepository.save(readStatus);

        // Đánh dấu tất cả notification chưa đọc trong channel này là đã đọc
        List<Notification> unread = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .filter(n -> channelId.equals(n.getChannelId()))
                .toList();

        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);

        log.info("Ack channel={} userId={} lastMsgId={}, marked {} notifications as read",
                channelId, userId, lastMessageId, unread.size());
    }

    /**
     * Đếm số notification và tin nhắn chưa đọc.
     */
    public UnreadCountResponse getUnreadCounts(String userId) {
        List<UnreadCounter> unreadCounters = unreadCounterRepository.findByUserId(userId);

        Map<Long, Long> channelCounts = new HashMap<>();
        Map<String, Long> privateCounts = new HashMap<>();
        Map<Long, Long> serverCounts = new HashMap<>();

        for (UnreadCounter uc : unreadCounters) {
            if (uc.getUnreadCount() <= 0) continue;
            
            if (uc.getChannelId() != null) {
                channelCounts.put(uc.getChannelId(), (long) uc.getUnreadCount());
            } else if (uc.getSenderUsername() != null) {
                privateCounts.put(uc.getSenderUsername(), (long) uc.getUnreadCount());
            }

            if (uc.getServerId() != null) {
                serverCounts.put(uc.getServerId(), serverCounts.getOrDefault(uc.getServerId(), 0L) + uc.getUnreadCount());
            }
        }

        return new UnreadCountResponse(userId, channelCounts, privateCounts, serverCounts);
    }
    
    @Transactional
    public void ackChannelUnread(String userId, Long channelId) {
        unreadCounterRepository.findByUserIdAndChannelId(userId, channelId)
            .ifPresent(uc -> {
                uc.setUnreadCount(0);
                unreadCounterRepository.save(uc);
            });
    }

    @Transactional
    public void ackDmUnread(String userId, String senderUsername) {
        unreadCounterRepository.findByUserIdAndSenderUsername(userId, senderUsername)
            .ifPresent(uc -> {
                uc.setUnreadCount(0);
                unreadCounterRepository.save(uc);
            });
    }

    @Transactional
    public void incrementUnreadCount(String userId, Long channelId, Long serverId, String senderUsername) {
        UnreadCounter uc;
        if (channelId != null) {
            uc = unreadCounterRepository.findByUserIdAndChannelId(userId, channelId)
                    .orElse(new UnreadCounter(userId, channelId, serverId, null, 0));
        } else {
            uc = unreadCounterRepository.findByUserIdAndSenderUsername(userId, senderUsername)
                    .orElse(new UnreadCounter(userId, null, null, senderUsername, 0));
        }
        uc.setUnreadCount(uc.getUnreadCount() + 1);
        if (uc.getServerId() == null && serverId != null) {
            uc.setServerId(serverId);
        }
        unreadCounterRepository.save(uc);
    }

    /**
     * Lấy danh sách thành viên của server từ server-service (chạy ngầm).
     */
    @SuppressWarnings("unchecked")
    public List<String> getServerMembers(Long serverId) {
        try {
            // Sử dụng port 8085 của server-service thay vì 8081
            String url = "http://localhost:8085/api/servers/" + serverId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("members")) {
                List<Map<String, Object>> members = (List<Map<String, Object>>) response.get("members");
                return members.stream()
                        .map(m -> (String) m.get("userId"))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to fetch server members for serverId={}", serverId, e);
        }
        return List.of();
    }

    /** Convert Entity → DTO */
    private NotificationDTO toDTO(Notification n) {
        return new NotificationDTO(
                n.getId(), n.getUserId(), n.getChannelId(), n.getServerId(),
                n.getSender(), n.getType(), n.getContent(),
                n.isRead(), n.getCreatedAt()
        );
    }
}
