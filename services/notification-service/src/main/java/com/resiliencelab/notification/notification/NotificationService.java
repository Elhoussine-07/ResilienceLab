package com.resiliencelab.notification.notification;

import com.resiliencelab.notification.messaging.OrderEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notifications;
    private final ProcessedEventRepository processedEvents;
    private final NotificationComposer composer;

    public NotificationService(NotificationRepository notifications, ProcessedEventRepository processedEvents, NotificationComposer composer) {
        this.notifications = notifications;
        this.processedEvents = processedEvents;
        this.composer = composer;
    }

    @Transactional
    public boolean notifyOnce(OrderEventMessage message) {
        int firstDelivery = processedEvents.insertIfAbsent(message.eventId(), message.eventType());
        if (firstDelivery == 0) { log.info("Event {} was already processed, ignoring the duplicate", message.eventId()); return false; }

        ComposedNotification composed = composer.compose(message);
        int created = notifications.insertIfAbsent(UUID.randomUUID(), message.orderId(), composed.type().name(), message.customerEmail(), composed.subject(), composed.body());
        if (created == 0) { log.warn("Order {} already has a {} notification, event {} creates nothing", message.orderId(), composed.type(), message.eventId()); return false; }

        log.info("Notification {} sent for order {} (event {})", composed.type(), message.orderId(), message.eventId());
        return true;
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(UUID id) {
        return notifications.findById(id).map(NotificationResponse::from).orElseThrow(() -> new NotificationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getByOrderId(UUID orderId) {
        return notifications.findByOrderIdOrderByCreatedAtAsc(orderId).stream().map(NotificationResponse::from).toList();
    }
}