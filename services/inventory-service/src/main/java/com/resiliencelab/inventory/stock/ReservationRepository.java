package com.resiliencelab.inventory.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByOrderIdAndProductId(UUID orderId, UUID productId);

    List<Reservation> findByOrderId(UUID orderId);

    /**
     * Inserts the reservation unless (order, product) already exists.
     * Returns 1 if inserted, 0 if it was already there (idempotent replay).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO reservations (id, order_id, product_id, quantity, status, created_at, updated_at)
            VALUES (:id, :orderId, :productId, :quantity, 'RESERVED', now(), now())
            ON CONFLICT (order_id, product_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("orderId") UUID orderId,
                       @Param("productId") UUID productId,
                       @Param("quantity") int quantity);

    /** Flips RESERVED to RELEASED. Returns 1 only for the caller that actually did it. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE reservations
               SET status = 'RELEASED', updated_at = now()
             WHERE id = :id AND status = 'RESERVED'
            """, nativeQuery = true)
    int markReleased(@Param("id") UUID id);
}
