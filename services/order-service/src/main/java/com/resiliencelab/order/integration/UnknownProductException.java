package com.resiliencelab.order.integration;

import java.util.UUID;

public class UnknownProductException extends RuntimeException {

    public UnknownProductException(UUID productId) {
        super("Product " + productId + " does not exist in the catalog");
    }
}