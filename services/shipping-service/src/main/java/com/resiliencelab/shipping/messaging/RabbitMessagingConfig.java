package com.resiliencelab.shipping.messaging;

import org.springframework.amqp.core.*;
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
    Queue shippingQueue(@Value("${messaging.shipping.queue}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    Binding shippingBinding(Queue shippingQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(shippingQueue).to(ordersExchange).with("order.confirmed");
    }

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
