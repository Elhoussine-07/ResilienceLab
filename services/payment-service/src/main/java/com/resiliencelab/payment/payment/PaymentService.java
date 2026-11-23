package com.resiliencelab.payment.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository repository;
    private final BigDecimal maxApprovedAmount;

    public PaymentService(
            PaymentRepository repository,
            @Value("${payment.simulation.max-approved-amount:10000.00}") BigDecimal maxApprovedAmount) {
        this.repository = repository;
        this.maxApprovedAmount = maxApprovedAmount;
    }

    /**
     * Processes a payment exactly once per idempotency key and per order.
     * The row is inserted first with its final status: concurrent duplicates
     * collapse into one row, and every caller then reads that single row.
     */
    @Transactional
    public PaymentOutcome process(String idempotencyKey, PaymentRequest request) {
        boolean approved = request.amount().compareTo(maxApprovedAmount) <= 0;
        PaymentStatus status = approved ? PaymentStatus.APPROVED : PaymentStatus.DECLINED;
        String failureReason = approved ? null : "Amount exceeds the approval limit";

        int inserted = repository.insertIfAbsent(
                UUID.randomUUID(), request.orderId(), idempotencyKey,
                request.amount(), request.currency(), status.name(), failureReason);

        Payment payment = repository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new PaymentConflictException(request.orderId()));

        if (inserted == 1) {
            return new PaymentOutcome(PaymentResponse.from(payment), true);
        }

        ensureSameRequest(payment, idempotencyKey, request);
        return new PaymentOutcome(PaymentResponse.from(payment), false);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(UUID id) {
        return repository.findById(id)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new PaymentNotFoundException("Payment " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new PaymentNotFoundException("No payment found for order " + orderId));
    }

    private void ensureSameRequest(Payment existing, String idempotencyKey, PaymentRequest request) {
        boolean same = existing.getOrderId().equals(request.orderId())
                && existing.getAmount().compareTo(request.amount()) == 0
                && existing.getCurrency().equals(request.currency());
        if (!same) {
            throw new IdempotencyKeyReuseException(idempotencyKey);
        }
    }
}