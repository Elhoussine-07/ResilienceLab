package com.resiliencelab.order.integration;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentClient {

    /**
     * Requests a payment. A declined payment is a normal result, not an exception.
     *
     * @throws PaymentUnavailableException if the payment service cannot be reached or answers unexpectedly
     */
    PaymentResult pay(UUID orderId, BigDecimal amount, String currency, String idempotencyKey);
}