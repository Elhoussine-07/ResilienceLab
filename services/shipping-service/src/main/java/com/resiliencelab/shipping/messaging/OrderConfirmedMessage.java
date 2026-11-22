package com.resiliencelab.shipping.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderConfirmedMessage(
        UUID eventId, String eventType, UUID orderId, String customerEmail, List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(UUID productId, int quantity) {}

    public boolean isValid() {
        return eventId != null && eventType != null && orderId != null
                && customerEmail != null && !customerEmail.isBlank()
                && items != null && !items.isEmpty();
    }

    public int totalQuantity() {
        return items.stream().mapToInt(Item::quantity).sum();
    }
}
