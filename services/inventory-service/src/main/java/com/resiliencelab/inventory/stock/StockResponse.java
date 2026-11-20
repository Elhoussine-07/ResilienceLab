package com.resiliencelab.inventory.stock;

import java.time.Instant;
import java.util.UUID;

public record StockResponse(
        UUID productId,
        int availableQuantity,
        int reservedQuantity,
        Instant updatedAt) {

    public static StockResponse from(StockItem item) {
        return new StockResponse(
                item.getProductId(),
                item.getAvailableQuantity(),
                item.getReservedQuantity(),
                item.getUpdatedAt());
    }
}
