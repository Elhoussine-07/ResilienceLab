package com.resiliencelab.order.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitOrderEventPublisherTest {

    private static final String EXCHANGE = "orders.events";

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitOrderEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RabbitOrderEventPublisher(rabbitTemplate, EXCHANGE);
    }

    @Test
    void usesTheConfirmedRoutingKeyForAConfirmedEvent() {
        OrderEvent event = OrderEventFixtures.sampleEvent(OrderEvent.ORDER_CONFIRMED, null);

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend(
                eq(EXCHANGE), eq("order.confirmed"), eq(event), any(MessagePostProcessor.class));
    }

    @Test
    void usesTheCancelledRoutingKeyForACancelledEvent() {
        OrderEvent event = OrderEventFixtures.sampleEvent(OrderEvent.ORDER_CANCELLED, "PAYMENT_DECLINED");

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend(
                eq(EXCHANGE), eq("order.cancelled"), eq(event), any(MessagePostProcessor.class));
    }

    @Test
    void neverThrowsWhenTheBrokerFails() {
        OrderEvent event = OrderEventFixtures.sampleEvent(OrderEvent.ORDER_CONFIRMED, null);
        doThrow(new AmqpException("broker is down"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }
}