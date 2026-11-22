package com.resiliencelab.order.integration;

import java.util.UUID;

public interface InventoryClient {

    /**
     * Reserves stock for one order line (idempotent on order + product).
     *
     * @throws StockUnavailableException     if the product has no stock line or not enough stock
     * @throws InventoryUnavailableException if the inventory cannot be reached or answers unexpectedly
     */
    void reserve(UUID orderId, UUID productId, int quantity);

    /**
     * Releases every reservation of the order (idempotent compensation).
     *
     * @throws InventoryUnavailableException if the inventory cannot be reached or answers unexpectedly
     */
    void release(UUID orderId);
}