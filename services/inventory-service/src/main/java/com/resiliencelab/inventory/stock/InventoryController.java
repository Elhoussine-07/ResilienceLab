package com.resiliencelab.inventory.stock;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @PutMapping("/stock/{productId}")
    public StockResponse setStock(@PathVariable UUID productId, @Valid @RequestBody SetStockRequest request) {
        return service.setStock(productId, request.availableQuantity());
    }

    @GetMapping("/stock/{productId}")
    public StockResponse getStock(@PathVariable UUID productId) {
        return service.getStock(productId);
    }

    /** 201 when the reservation is created, 200 when an identical request is replayed. */
    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReserveStockRequest request) {
        ReservationOutcome outcome = service.reserve(request);
        HttpStatus status = outcome.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(outcome.reservation());
    }

    @GetMapping("/reservations/orders/{orderId}")
    public List<ReservationResponse> listReservations(@PathVariable UUID orderId) {
        return service.listReservations(orderId);
    }

    @PostMapping("/reservations/orders/{orderId}/release")
    public List<ReservationResponse> release(@PathVariable UUID orderId) {
        return service.release(orderId);
    }
}
