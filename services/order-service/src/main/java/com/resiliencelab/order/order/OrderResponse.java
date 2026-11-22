package com.resiliencelab.order.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerEmail,
        OrderStatus status,
        CancellationReason cancellationReason,
        String currency,
        BigDecimal totalAmount,
        List<Item> items,
        Instant createdAt,
        Instant updatedAt) {

    public record Item(UUID productId, int quantity, BigDecimal unitPrice) {
    }

    public static OrderResponse from(CustomerOrder order) {
        List<Item> items = order.getItems().stream()
                .map(item -> new Item(item.getProductId(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getCancellationReason(),
                order.getCurrency(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}