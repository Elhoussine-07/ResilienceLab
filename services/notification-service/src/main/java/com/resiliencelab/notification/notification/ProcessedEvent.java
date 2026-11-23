package com.resiliencelab.notification.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id @Column(name = "event_id") private UUID eventId;
    @Column(name = "event_type", nullable = false, length = 64) private String eventType;
    @Column(name = "processed_at", nullable = false) private Instant processedAt;

    protected ProcessedEvent() {}
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Instant getProcessedAt() { return processedAt; }
}