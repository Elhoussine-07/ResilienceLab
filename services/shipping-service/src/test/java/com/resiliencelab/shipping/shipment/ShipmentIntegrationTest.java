package com.resiliencelab.shipping.shipment;

import com.resiliencelab.shipping.messaging.OrderConfirmedMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.password=unused-in-tests",
        "spring.rabbitmq.username=unused-in-tests",
        "spring.rabbitmq.password=unused-in-tests",
        "spring.rabbitmq.listener.simple.auto-startup=false"})
@AutoConfigureMockMvc
@Testcontainers
class ShipmentIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

    @Autowired private ShipmentService service;
    @Autowired private ShipmentRepository repository;
    @Autowired private MockMvc mockMvc;

    @Test
    void concurrentDeliveriesOfTheSameEventCreateASingleShipment() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderConfirmedMessage message = message(UUID.randomUUID(), orderId);
        List<Boolean> results = runConcurrently(10, () -> service.createShipmentIfNew(message));
        assertThat(results.stream().filter(created -> created).count()).isEqualTo(1);
        assertThat(repository.countByOrderId(orderId)).isEqualTo(1);
    }

    @Test
    void concurrentEventsForTheSameOrderCreateASingleShipment() throws Exception {
        UUID orderId = UUID.randomUUID();
        List<Boolean> results = runConcurrently(10, () -> service.createShipmentIfNew(message(UUID.randomUUID(), orderId)));
        assertThat(results.stream().filter(created -> created).count()).isEqualTo(1);
        assertThat(repository.countByOrderId(orderId)).isEqualTo(1);
    }

    @Test
    void exposesAShipmentByOrderId() throws Exception {
        UUID orderId = UUID.randomUUID();
        service.createShipmentIfNew(message(UUID.randomUUID(), orderId));
        mockMvc.perform(get("/api/v1/shipments/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.itemCount").value(3))
                .andExpect(jsonPath("$.trackingNumber").value(org.hamcrest.Matchers.startsWith("TRK-")));
    }

    @Test
    void returnsNotFoundForAnOrderWithoutShipment() throws Exception {
        mockMvc.perform(get("/api/v1/shipments/orders/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void rejectsAMalformedIdentifier() throws Exception {
        mockMvc.perform(get("/api/v1/shipments/not-a-uuid")).andExpect(status().isBadRequest());
    }

    private static OrderConfirmedMessage message(UUID eventId, UUID orderId) {
        return new OrderConfirmedMessage(eventId, "OrderConfirmed", orderId, "student@example.com",
                List.of(new OrderConfirmedMessage.Item(UUID.randomUUID(), 2), new OrderConfirmedMessage.Item(UUID.randomUUID(), 1)));
    }

    private static <T> List<T> runConcurrently(int threads, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> { start.await(); return task.call(); }));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) { results.add(future.get(30, TimeUnit.SECONDS)); }
            return results;
        } finally { pool.shutdownNow(); }
    }
}
