package com.resiliencelab.shipping.shipment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findByOrderId(UUID orderId);
    long countByOrderId(UUID orderId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO shipments (id, order_id, customer_email, item_count, status,
                                   tracking_number, created_at, updated_at)
            VALUES (:id, :orderId, :customerEmail, :itemCount, 'CREATED',
                    :trackingNumber, now(), now())
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("orderId") UUID orderId,
                       @Param("customerEmail") String customerEmail,
                       @Param("itemCount") int itemCount,
                       @Param("trackingNumber") String trackingNumber);
}
