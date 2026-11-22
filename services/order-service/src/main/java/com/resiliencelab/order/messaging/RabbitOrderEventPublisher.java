package com.resiliencelab.order.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitOrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public RabbitOrderEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${messaging.orders.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publish(OrderEvent event) {
        try {
            String routingKey = routingKeyFor(event);
            rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> {
                message.getMessageProperties().setMessageId(event.eventId().toString());
                return message;
            });
            log.info("Published {} for order {} (routing key {})", event.eventType(), event.orderId(), routingKey);
        } catch (RuntimeException e) {
            log.error("Could not publish {} for order {}: {}", event.eventType(), event.orderId(), e.getMessage());
        }
    }

    private static String routingKeyFor(OrderEvent event) {
        return switch (event.eventType()) {
            case OrderEvent.ORDER_CONFIRMED -> "order.confirmed";
            case OrderEvent.ORDER_CANCELLED -> "order.cancelled";
            default -> throw new IllegalArgumentException("Unknown event type: " + event.eventType());
        };
    }
}