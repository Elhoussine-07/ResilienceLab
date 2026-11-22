package com.resiliencelab.shipping.shipment;

import com.resiliencelab.shipping.messaging.OrderConfirmedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);
    private final ShipmentRepository shipments;
    private final ProcessedEventRepository processedEvents;

    public ShipmentService(ShipmentRepository shipments, ProcessedEventRepository processedEvents) {
        this.shipments = shipments;
        this.processedEvents = processedEvents;
    }

    @Transactional
    public boolean createShipmentIfNew(OrderConfirmedMessage message) {
        int firstDelivery = processedEvents.insertIfAbsent(message.eventId(), message.eventType());
        if (firstDelivery == 0) {
            log.info("Event {} was already processed, ignoring the duplicate", message.eventId());
            return false;
        }

        int created = shipments.insertIfAbsent(
                UUID.randomUUID(), message.orderId(), message.customerEmail(),
                message.totalQuantity(), newTrackingNumber());
        if (created == 0) {
            log.warn("Order {} already has a shipment, event {} creates nothing", message.orderId(), message.eventId());
            return false;
        }

        log.info("Shipment created for order {} (event {})", message.orderId(), message.eventId());
        return true;
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getById(UUID id) {
        return shipments.findById(id).map(ShipmentResponse::from)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getByOrderId(UUID orderId) {
        return shipments.findByOrderId(orderId).map(ShipmentResponse::from)
                .orElseThrow(() -> new ShipmentNotFoundException("No shipment found for order " + orderId));
    }

    private static String newTrackingNumber() {
        return "TRK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
