package com.resiliencelab.shipping.messaging;

import com.resiliencelab.shipping.shipment.ShipmentService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConfirmedListener {

    private final ShipmentService shipmentService;
    public OrderConfirmedListener(ShipmentService shipmentService) { this.shipmentService = shipmentService; }

    @RabbitListener(queues = "${messaging.shipping.queue}")
    public void onOrderConfirmed(OrderConfirmedMessage message) {
        if (!message.isValid()) {
            throw new AmqpRejectAndDontRequeueException(
                    "Invalid OrderConfirmed message: eventId, eventType, orderId, customerEmail and items are required");
        }
        shipmentService.createShipmentIfNew(message);
    }
}
