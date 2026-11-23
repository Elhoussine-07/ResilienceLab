package com.resiliencelab.notification.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderEventMessage(
        UUID eventId, String eventType, UUID orderId, String customerEmail,
        String currency, BigDecimal totalAmount, String cancellationReason) {

    public static final String ORDER_CONFIRMED = "OrderConfirmed";
    public static final String ORDER_CANCELLED = "OrderCancelled";

    public boolean isValid() {
        return eventId != null && orderId != null
                && customerEmail != null && !customerEmail.isBlank()
                && currency != null && !currency.isBlank()
                && totalAmount != null
                && (ORDER_CONFIRMED.equals(eventType) || ORDER_CANCELLED.equals(eventType));
    }
}