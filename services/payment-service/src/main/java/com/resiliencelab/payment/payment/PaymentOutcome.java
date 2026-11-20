package com.resiliencelab.payment.payment;

/** Result of a payment request: {@code created} is false when an identical request was replayed. */
public record PaymentOutcome(PaymentResponse payment, boolean created) {
}