package com.resiliencelab.order.integration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class RestInventoryClient implements InventoryClient {

    private final RestClient client;

    public RestInventoryClient(@Qualifier("inventoryRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    public void reserve(UUID orderId, UUID productId, int quantity) {
        try {
            client.post()
                    .uri("/api/v1/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ReserveCall(orderId, productId, quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            if (status == 404 || status == 409) {
                throw new StockUnavailableException(
                        "Inventory refused the reservation of product " + productId + " (HTTP " + status + ")");
            }
            throw new InventoryUnavailableException("Unexpected inventory response: HTTP " + status, e);
        } catch (RestClientException e) {
            throw new InventoryUnavailableException("Inventory call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void release(UUID orderId) {
        try {
            client.post()
                    .uri("/api/v1/reservations/orders/{orderId}/release", orderId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new InventoryUnavailableException("Inventory release failed: " + e.getMessage(), e);
        }
    }

    private record ReserveCall(UUID orderId, UUID productId, int quantity) {
    }
}