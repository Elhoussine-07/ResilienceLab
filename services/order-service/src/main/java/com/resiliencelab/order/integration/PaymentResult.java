package com.resiliencelab.order.integration;

public record PaymentResult(boolean approved, String failureReason) {
}