package com.chatsever.log.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình RabbitMQ phía consumer (log-service).
 * Exchange: chat.exchange (topic) → Queue: chat.log.queue → Routing: log.#
 * Khai báo idempotent với phía producer (messaging-service).
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "chat.exchange";
    public static final String QUEUE = "chat.log.queue";
    public static final String ROUTING_KEY_PATTERN = "log.#";   // Nhận mọi message có routing key bắt đầu bằng "log."

    // Khai báo Topic Exchange — durable, không auto-delete
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // Queue lưu log — durable (không mất khi RabbitMQ restart)
    @Bean
    public Queue logQueue() {
        return new Queue(QUEUE, true);
    }

    // Bind queue vào exchange theo pattern "log.#"
    @Bean
    public Binding logBinding(Queue logQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(logQueue).to(chatExchange).with(ROUTING_KEY_PATTERN);
    }

    // Converter JSON ↔ Object cho RabbitMQ, hỗ trợ LocalDateTime
    @Bean
    public MessageConverter jacksonConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());                    // Hỗ trợ Java 8 Date/Time
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Ghi date dạng string, không phải số
        return new Jackson2JsonMessageConverter(mapper);
    }
}
