package com.resiliencelab.notification.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 32) private NotificationType notificationType;
    @Column(nullable = false, length = 254) private String recipient;
    @Column(nullable = false, length = 200) private String subject;
    @Column(nullable = false, length = 1000) private String body;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected Notification() {}
    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public NotificationType getNotificationType() { return notificationType; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}