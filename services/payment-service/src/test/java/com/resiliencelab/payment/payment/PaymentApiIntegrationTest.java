package com.resiliencelab.payment.payment;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.password=unused-in-tests")
@AutoConfigureMockMvc
@Testcontainers
class PaymentApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void approvesAPaymentAndReplayReturnsTheSameOne() throws Exception {
        UUID orderId = UUID.randomUUID();
        String key = newKey();

        String body = pay(key, orderId, "49.90", "EUR")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn().getResponse().getContentAsString();
        String paymentId = JsonPath.read(body, "$.id");

        pay(key, orderId, "49.90", "EUR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId));
    }

    @Test
    void declinesAnAmountAboveTheLimit() throws Exception {
        pay(newKey(), UUID.randomUUID(), "10000.01", "EUR")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.failureReason").exists());
    }

    @Test
    void rejectsReuseOfAKeyWithADifferentRequest() throws Exception {
        UUID orderId = UUID.randomUUID();
        String key = newKey();
        pay(key, orderId, "49.90", "EUR").andExpect(status().isCreated());

        pay(key, orderId, "10.00", "EUR")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void rejectsASecondPaymentForTheSameOrderWithAnotherKey() throws Exception {
        UUID orderId = UUID.randomUUID();
        pay(newKey(), orderId, "49.90", "EUR").andExpect(status().isCreated());

        pay(newKey(), orderId, "49.90", "EUR")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Payment conflict"));
    }

    @Test
    void rejectsAMissingOrBlankIdempotencyKey() throws Exception {
        String json = paymentJson(UUID.randomUUID(), "49.90", "EUR");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        pay("   ", UUID.randomUUID(), "49.90", "EUR")
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAnInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", newKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": null, "amount": 0, "currency": "eur"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.orderId").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.currency").exists());
    }

    @Test
    void readsAPaymentByOrderAndReturnsNotFoundWhenUnknown() throws Exception {
        UUID orderId = UUID.randomUUID();
        pay(newKey(), orderId, "49.90", "EUR").andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/payments/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()));

        mockMvc.perform(get("/api/v1/payments/orders/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private ResultActions pay(String key, UUID orderId, String amount, String currency) throws Exception {
        return mockMvc.perform(post("/api/v1/payments")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson(orderId, amount, currency)));
    }

    private static String paymentJson(UUID orderId, String amount, String currency) {
        return """
                {"orderId": "%s", "amount": %s, "currency": "%s"}
                """.formatted(orderId, amount, currency);
    }

    private static String newKey() {
        return "key-" + UUID.randomUUID();
    }
}