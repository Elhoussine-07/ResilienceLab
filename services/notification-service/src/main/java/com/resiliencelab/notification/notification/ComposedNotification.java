package com.resiliencelab.notification.notification;

public record ComposedNotification(NotificationType type, String subject, String body) {}