package com.resiliencelab.order.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class RestPaymentClient implements PaymentClient {

    private final RestClient client;

    public RestPaymentClient(@Qualifier("paymentRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    public PaymentResult pay(UUID orderId, BigDecimal amount, String currency, String idempotencyKey) {
        PaymentReply reply;
        try {
            reply = client.post()
                    .uri("/api/v1/payments")
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PaymentCall(orderId, amount, currency))
                    .retrieve()
                    .body(PaymentReply.class);
        } catch (RestClientException e) {
            throw new PaymentUnavailableException("Payment call failed: " + e.getMessage(), e);
        }

        if (reply == null || reply.status() == null) {
            throw new PaymentUnavailableException("The payment service returned an empty response");
        }
        return switch (reply.status()) {
            case "APPROVED" -> new PaymentResult(true, null);
            case "DECLINED" -> new PaymentResult(false, reply.failureReason());
            default -> throw new PaymentUnavailableException("Unknown payment status: " + reply.status());
        };
    }

    private record PaymentCall(UUID orderId, BigDecimal amount, String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaymentReply(String status, String failureReason) {
    }
}