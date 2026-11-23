package com.resiliencelab.notification.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id, UUID orderId, NotificationType type, String recipient, String subject, String body, Instant createdAt) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getOrderId(), notification.getNotificationType(),
                notification.getRecipient(), notification.getSubject(), notification.getBody(), notification.getCreatedAt());
    }
}