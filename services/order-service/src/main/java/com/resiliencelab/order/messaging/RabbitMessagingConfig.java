package com.resiliencelab.order.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMessagingConfig {

    /** Durable topic exchange. Consumers declare their own queues and bindings. */
    @Bean
    TopicExchange ordersExchange(@Value("${messaging.orders.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }

    /** Messages are sent as JSON (Jackson 3). */
    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}