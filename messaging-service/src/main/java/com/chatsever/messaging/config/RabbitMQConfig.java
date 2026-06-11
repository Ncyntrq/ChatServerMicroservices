package com.chatsever.messaging.config;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange("chat.exchange");
    }

    @Bean
    public FanoutExchange chatFanout() {
        return new FanoutExchange("chat.fanout");
    }

    @Bean
    public Queue broadcastQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding broadcastBinding(FanoutExchange chatFanout, Queue broadcastQueue) {
        return BindingBuilder.bind(broadcastQueue).to(chatFanout);
    }

    /** Queue riêng cho sự kiện thay đổi trạng thái user từ presence-service. */
    @Bean
    public Queue presenceQueue() {
        return new AnonymousQueue();
    }

    /** Bind presenceQueue vào chat.exchange với routing key "presence.status". */
    @Bean
    public Binding presenceBinding(TopicExchange chatExchange, Queue presenceQueue) {
        return BindingBuilder.bind(presenceQueue).to(chatExchange).with("presence.status");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
