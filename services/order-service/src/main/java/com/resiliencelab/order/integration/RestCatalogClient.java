package com.resiliencelab.order.integration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class RestCatalogClient implements CatalogClient {

    private final RestClient client;

    public RestCatalogClient(@Qualifier("catalogRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    public ProductInfo getProduct(UUID productId) {
        try {
            ProductInfo product = client.get()
                    .uri("/api/v1/products/{id}", productId)
                    .retrieve()
                    .body(ProductInfo.class);
            if (product == null || product.price() == null) {
                throw new CatalogUnavailableException("The catalog returned an empty or incomplete product");
            }
            return product;
        } catch (HttpClientErrorException.NotFound e) {
            throw new UnknownProductException(productId);
        } catch (RestClientException e) {
            throw new CatalogUnavailableException("Catalog call failed: " + e.getMessage(), e);
        }
    }
}