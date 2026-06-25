package com.chatsever.messaging.service;

import com.chatsever.messaging.entity.OutboxMessage;
import com.chatsever.messaging.repository.OutboxMessageRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxRelayWorker {

    private static final Logger logger = LoggerFactory.getLogger(OutboxRelayWorker.class);

    private final OutboxMessageRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxRelayWorker(OutboxMessageRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    // Vòng quét dự phòng (fallback): bắt các message bị sót nếu lần đẩy-ngay sau commit thất bại
    // hoặc tiến trình restart giữa chừng. Đẩy-ngay (MessageService.triggerImmediateRelayAfterCommit)
    // mới là đường giao tin realtime; vòng này chỉ là lưới an toàn nên 1s là đủ.
    // lockAtLeastFor = 0s để các lần đẩy-ngay liên tiếp (nhiều tin dồn) không bị ShedLock chặn.
    @Scheduled(fixedDelayString = "1000")
    @SchedulerLock(name = "outbox_relay_lock", lockAtMostFor = "2m", lockAtLeastFor = "0s")
    @Transactional
    public void processOutboxMessages() {
        List<OutboxMessage> messages = repository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        if (messages.isEmpty()) {
            return;
        }

        logger.debug("Found {} pending outbox messages", messages.size());

        for (OutboxMessage msg : messages) {
            try {
                // Construct raw AMQP message with JSON content type to avoid double-escaping
                Message rabbitMessage = MessageBuilder.withBody(msg.getPayload().getBytes())
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .build();

                String routingKey = msg.getRoutingKey() == null ? "" : msg.getRoutingKey();
                rabbitTemplate.convertAndSend(msg.getExchange(), routingKey, rabbitMessage);
                
                msg.setStatus("PROCESSED");
                logger.debug("Successfully relayed outbox message {}", msg.getId());
            } catch (Exception e) {
                logger.error("Failed to relay outbox message {}: {}", msg.getId(), e.getMessage());
                // Thêm cơ chế đếm số lần retry nếu cần, hiện tại cứ để PENDING để thử lại
            }
        }
        
        repository.saveAll(messages);
    }
}
