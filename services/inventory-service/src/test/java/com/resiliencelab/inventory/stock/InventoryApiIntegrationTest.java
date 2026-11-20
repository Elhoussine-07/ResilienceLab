package com.resiliencelab.inventory.stock;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.password=unused-in-tests")
@AutoConfigureMockMvc
@Testcontainers
class InventoryApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void setsAndReadsStock() throws Exception {
        UUID productId = UUID.randomUUID();
        setStock(productId, 5);

        mockMvc.perform(get("/api/v1/stock/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(5))
                .andExpect(jsonPath("$.reservedQuantity").value(0));
    }

    @Test
    void reservingMovesStockAndReplayIsIdempotent() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        setStock(productId, 5);

        String body = reserve(orderId, productId, 2)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reservationId = JsonPath.read(body, "$.id");

        reserve(orderId, productId, 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId));

        mockMvc.perform(get("/api/v1/stock/" + productId))
                .andExpect(jsonPath("$.availableQuantity").value(3))
                .andExpect(jsonPath("$.reservedQuantity").value(2));
    }

    @Test
    void rejectsAReservationWhenStockIsInsufficient() throws Exception {
        UUID productId = UUID.randomUUID();
        setStock(productId, 1);

        reserve(UUID.randomUUID(), productId, 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient stock"));

        mockMvc.perform(get("/api/v1/stock/" + productId))
                .andExpect(jsonPath("$.availableQuantity").value(1))
                .andExpect(jsonPath("$.reservedQuantity").value(0));
    }

    @Test
    void returnsNotFoundForAnUnknownProduct() throws Exception {
        reserve(UUID.randomUUID(), UUID.randomUUID(), 1)
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAnInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": null, "productId": null, "quantity": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.orderId").exists())
                .andExpect(jsonPath("$.errors.productId").exists())
                .andExpect(jsonPath("$.errors.quantity").exists());
    }

    @Test
    void releasingRestoresStockOnlyOnce() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        setStock(productId, 5);
        reserve(orderId, productId, 2).andExpect(status().isCreated());

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/v1/reservations/orders/" + orderId + "/release"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("RELEASED"));
        }

        mockMvc.perform(get("/api/v1/stock/" + productId))
                .andExpect(jsonPath("$.availableQuantity").value(5))
                .andExpect(jsonPath("$.reservedQuantity").value(0));
    }

    private void setStock(UUID productId, int quantity) throws Exception {
        mockMvc.perform(put("/api/v1/stock/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availableQuantity\": " + quantity + "}"))
                .andExpect(status().isOk());
    }

    private ResultActions reserve(UUID orderId, UUID productId, int quantity) throws Exception {
        return mockMvc.perform(post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"orderId": "%s", "productId": "%s", "quantity": %d}
                        """.formatted(orderId, productId, quantity)));
    }
}
