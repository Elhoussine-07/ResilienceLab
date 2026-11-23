package com.resiliencelab.inventory.stock;

import java.util.UUID;

public class StockItemNotFoundException extends RuntimeException {

    public StockItemNotFoundException(UUID productId) {
        super("No stock found for product " + productId);
    }
}
