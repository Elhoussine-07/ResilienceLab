package com.resiliencelab.order.messaging;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

final class OrderEventFixtures {

    private OrderEventFixtures() {
    }

    static OrderEvent sampleEvent(String eventType, String cancellationReason) {
        return new OrderEvent(
                UUID.randomUUID(),
                eventType,
                "2026-11-20T10:15:30.123456Z",
                UUID.randomUUID(),
                "student@example.com",
                "EUR",
                new BigDecimal("159.80"),
                cancellationReason,
                List.of(new OrderEvent.Item(UUID.randomUUID(), 2, new BigDecimal("79.90"))));
    }
}