package com.resiliencelab.payment.payment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final int MAX_KEY_LENGTH = 100;

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    /** 201 when the payment is processed now, 200 when an identical request is replayed. */
    @PostMapping
    public ResponseEntity<PaymentResponse> process(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        if (idempotencyKey.isBlank() || idempotencyKey.length() > MAX_KEY_LENGTH) {
            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key must not be blank and must have at most " + MAX_KEY_LENGTH + " characters");
        }
        PaymentOutcome outcome = service.process(idempotencyKey, request);
        HttpStatus status = outcome.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(outcome.payment());
    }

    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping("/orders/{orderId}")
    public PaymentResponse getByOrderId(@PathVariable UUID orderId) {
        return service.getByOrderId(orderId);
    }
}