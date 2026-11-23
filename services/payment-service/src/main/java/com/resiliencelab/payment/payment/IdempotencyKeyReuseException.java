package com.resiliencelab.payment.payment;

/** The same idempotency key was reused with a different request content. */
public class IdempotencyKeyReuseException extends RuntimeException {

    public IdempotencyKeyReuseException(String idempotencyKey) {
        super("Idempotency key '" + idempotencyKey + "' was already used with a different request");
    }
}