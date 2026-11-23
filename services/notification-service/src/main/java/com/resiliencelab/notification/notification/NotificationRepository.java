package com.resiliencelab.notification.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
    long countByOrderId(UUID orderId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO notifications (id, order_id, notification_type, recipient, subject, body, created_at)
            VALUES (:id, :orderId, :notificationType, :recipient, :subject, :body, now())
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("orderId") UUID orderId,
                       @Param("notificationType") String notificationType, @Param("recipient") String recipient,
                       @Param("subject") String subject, @Param("body") String body);
}