package com.resiliencelab.payment.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByOrderId(UUID orderId);

    /**
     * Inserts the payment unless a unique constraint is hit, on the idempotency key OR on the order.
     * Returns 1 if inserted, 0 otherwise. The caller finds out which case it is
     * by looking the payment up by idempotency key.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO payments (id, order_id, idempotency_key, amount, currency, status,
                                  failure_reason, created_at, updated_at)
            VALUES (:id, :orderId, :idempotencyKey, :amount, :currency, :status,
                    CAST(:failureReason AS VARCHAR), now(), now())
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("orderId") UUID orderId,
                       @Param("idempotencyKey") String idempotencyKey,
                       @Param("amount") BigDecimal amount,
                       @Param("currency") String currency,
                       @Param("status") String status,
                       @Param("failureReason") String failureReason);
}