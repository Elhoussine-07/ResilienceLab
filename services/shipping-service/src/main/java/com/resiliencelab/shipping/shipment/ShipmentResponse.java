package com.resiliencelab.shipping.shipment;

import java.time.Instant;
import java.util.UUID;

public record ShipmentResponse(
        UUID id, UUID orderId, String customerEmail, int itemCount,
        ShipmentStatus status, String trackingNumber, Instant createdAt) {

    public static ShipmentResponse from(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(), shipment.getOrderId(), shipment.getCustomerEmail(),
                shipment.getItemCount(), shipment.getStatus(), shipment.getTrackingNumber(),
                shipment.getCreatedAt());
    }
}
