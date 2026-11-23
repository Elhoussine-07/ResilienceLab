package com.resiliencelab.notification.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMessagingConfig {
    @Bean
    TopicExchange ordersExchange(@Value("${messaging.orders.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }
    @Bean
    Queue notificationQueue(@Value("${messaging.notification.queue}") String name) {
        return QueueBuilder.durable(name).build();
    }
    @Bean
    Binding confirmedBinding(Queue notificationQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(notificationQueue).to(ordersExchange).with("order.confirmed");
    }
    @Bean
    Binding cancelledBinding(Queue notificationQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(notificationQueue).to(ordersExchange).with("order.cancelled");
    }
    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}