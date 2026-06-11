package com.chatsever.presence.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình RabbitMQ cho presence-service.
 * Sử dụng lại TopicExchange "chat.exchange" (đã khai báo trong messaging-service).
 * Presence chỉ publish (không consume), nên chỉ cần khai báo exchange + converter.
 */
@Configuration
public class RabbitConfig {

    /** Khai báo lại exchange "chat.exchange" để Spring AMQP biết cấu hình.
     *  Nếu exchange đã tồn tại trên RabbitMQ thì không tạo mới (idempotent). */
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange("chat.exchange");
    }

    /** JSON converter để serialize MessageDTO sang JSON khi publish. */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
