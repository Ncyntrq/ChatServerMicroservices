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

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}