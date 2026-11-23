package com.resiliencelab.notification.messaging;

import com.resiliencelab.notification.notification.NotificationService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {
    private final NotificationService notificationService;

    public OrderEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${messaging.notification.queue}")
    public void onOrderEvent(OrderEventMessage message) {
        if (!message.isValid()) {
            throw new AmqpRejectAndDontRequeueException("Invalid order event: a known eventType and all required fields are needed");
        }
        notificationService.notifyOnce(message);
    }
}