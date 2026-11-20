package com.resiliencelab.inventory.stock;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID orderId,
        UUID productId,
        int quantity,
        ReservationStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getProductId(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }
}
