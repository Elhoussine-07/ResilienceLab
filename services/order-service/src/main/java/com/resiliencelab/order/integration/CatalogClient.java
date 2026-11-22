package com.resiliencelab.order.integration;

import java.util.UUID;

public interface CatalogClient {

    /**
     * @throws UnknownProductException       if the catalog does not know the product
     * @throws CatalogUnavailableException   if the catalog cannot be reached or answers unexpectedly
     */
    ProductInfo getProduct(UUID productId);
}