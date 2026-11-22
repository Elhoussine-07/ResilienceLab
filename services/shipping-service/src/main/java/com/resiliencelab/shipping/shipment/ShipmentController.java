package com.resiliencelab.shipping.shipment;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private final ShipmentService service;
    public ShipmentController(ShipmentService service) { this.service = service; }

    @GetMapping("/{id}")
    public ShipmentResponse getById(@PathVariable UUID id) { return service.getById(id); }

    @GetMapping("/orders/{orderId}")
    public ShipmentResponse getByOrderId(@PathVariable UUID orderId) { return service.getByOrderId(orderId); }
}
