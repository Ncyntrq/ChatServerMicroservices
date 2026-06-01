package com.chatsever.notification.listener;

import com.chatsever.common.dto.MessageDTO;
import com.chatsever.notification.config.RabbitMQConfig;
import com.chatsever.notification.model.NotificationType;
import com.chatsever.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consumer nhận MessageDTO từ RabbitMQ queue "chat.notification.queue".
 * - Parse tin nhắn → phát hiện @username hoặc @everyone
 * - Tạo notification tương ứng
 */
@Component
public class MessageEventListener {

    private static final Logger log = LoggerFactory.getLogger(MessageEventListener.class);
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    private final NotificationService notificationService;

    public MessageEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Core consumer — nhận message event từ RabbitMQ.
     * Routing key: notify.message, notify.mention, notify.dm
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void onMessage(MessageDTO message) {
        log.info("Nhận message event: sender={}, channelId={}, serverId={}, content={}",
                message.getSender(), message.getChannelId(), message.getServerId(), message.getContent());

        String content = message.getContent();
        String sender = message.getSender();
        Long channelId = message.getChannelId();
        Long serverId = message.getServerId();

        if (content == null || sender == null) {
            log.warn("Message event thiếu content hoặc sender, bỏ qua");
            return;
        }

        // 1. Parse nội dung → tìm @username hoặc @everyone
        List<String> mentions = parseMentions(content);

        // 2. Nếu có @everyone → tạo notification loại MENTION_ALL
        //    (Trong thực tế cần lấy danh sách members của channel,
        //     ở đây tạo 1 notification tổng thể)
        if (mentions.contains("everyone")) {
            notificationService.createNotification(
                    "ALL",          // userId = ALL (broadcast)
                    channelId,
                    serverId,
                    sender,
                    NotificationType.MENTION_ALL,
                    content
            );
            log.info("Tạo MENTION_ALL notification từ sender={} channel={}", sender, channelId);
        }

        // 3. Nếu có @user cụ thể → tạo notification cho từng user
        for (String mentionedUser : mentions) {
            if (!"everyone".equals(mentionedUser) && !mentionedUser.equals(sender)) {
                notificationService.createNotification(
                        mentionedUser,
                        channelId,
                        serverId,
                        sender,
                        NotificationType.MENTION,
                        content
                );
                log.info("Tạo MENTION notification cho user={} từ sender={} channel={}", mentionedUser, sender, channelId);
            }
        }

        // 4. Nếu là tin nhắn riêng (receiver != null) → tạo DM notification
        if (message.getReceiver() != null && !message.getReceiver().isEmpty()) {
            notificationService.createNotification(
                    message.getReceiver(),
                    channelId,
                    serverId,
                    sender,
                    NotificationType.DM,
                    content
            );
            log.info("Tạo DM notification cho user={} từ sender={}", message.getReceiver(), sender);
            
            // Tăng biến đếm unread count cho tin nhắn DM
            notificationService.incrementUnreadCount(message.getReceiver(), null, sender);
        } else if (channelId != null && serverId != null) {
            // Tăng biến đếm unread count cho tin nhắn Channel
            List<String> members = notificationService.getServerMembers(serverId);
            for (String member : members) {
                if (!member.equals(sender)) {
                    notificationService.incrementUnreadCount(member, channelId, null);
                }
            }
            log.info("Đã tăng unread count cho channel={} ({} members)", channelId, members.size());
        }
    }

    /**
     * Parse nội dung tin nhắn → tìm @username.
     * Regex: @(\w+) → extract username sau dấu @
     */
    List<String> parseMentions(String content) {
        List<String> mentions = new ArrayList<>();
        if (content == null) return mentions;

        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String username = matcher.group(1);
            if (!mentions.contains(username)) {
                mentions.add(username);
            }
        }
        return mentions;
    }
}
