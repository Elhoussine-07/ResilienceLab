package com.resiliencelab.order.messaging;

import com.resiliencelab.order.order.OrderResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Event published when an order reaches a final state.
 * {@code occurredAt} is an ISO-8601 UTC string so that consumers in any language can parse it.
 * {@code cancellationReason} is null for confirmed orders.
 */
public record OrderEvent(
        UUID eventId,
        String eventType,
        String occurredAt,
        UUID orderId,
        String customerEmail,
        String currency,
        BigDecimal totalAmount,
        String cancellationReason,
        List<Item> items) {

    public static final String ORDER_CONFIRMED = "OrderConfirmed";
    public static final String ORDER_CANCELLED = "OrderCancelled";

    public record Item(UUID productId, int quantity, BigDecimal unitPrice) {
    }

    public static OrderEvent from(OrderResponse order) {
        String eventType = switch (order.status()) {
            case CONFIRMED -> ORDER_CONFIRMED;
            case CANCELLED -> ORDER_CANCELLED;
            case PENDING -> throw new IllegalArgumentException(
                    "No event is published for the PENDING order " + order.id());
        };
        List<Item> items = order.items().stream()
                .map(item -> new Item(item.productId(), item.quantity(), item.unitPrice()))
                .toList();
        return new OrderEvent(
                UUID.randomUUID(),
                eventType,
                order.updatedAt().toString(),
                order.id(),
                order.customerEmail(),
                order.currency(),
                order.totalAmount(),
                order.cancellationReason() == null ? null : order.cancellationReason().name(),
                items);
    }
}