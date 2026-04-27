package com.chatsever.log.listener;

import com.chatsever.common.dto.LogEntry;
import com.chatsever.log.config.RabbitMQConfig;
import com.chatsever.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer nhận LogEntry từ RabbitMQ queue "chat.log.queue".
 * Khi messaging-service publish event → listener này tự động nhận và ghi log.
 */
@Component
public class LogEventListener {

    private static final Logger log = LoggerFactory.getLogger(LogEventListener.class);

    private final LogService logService;

    public LogEventListener(LogService logService) {
        this.logService = logService;
    }

    // Spring AMQP tự gọi method này mỗi khi có message mới trong queue
    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void onLogEvent(LogEntry entry) {
        log.debug("Nhận log event: type={} sender={}", entry.getEventType(), entry.getSender());
        logService.log(entry);
    }
}
