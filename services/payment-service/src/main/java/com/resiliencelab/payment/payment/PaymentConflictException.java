package com.resiliencelab.payment.payment;

import java.util.UUID;

/** The order already has a payment recorded under a different idempotency key. */
public class PaymentConflictException extends RuntimeException {

    public PaymentConflictException(UUID orderId) {
        super("Order " + orderId + " already has a payment recorded with another idempotency key");
    }
}