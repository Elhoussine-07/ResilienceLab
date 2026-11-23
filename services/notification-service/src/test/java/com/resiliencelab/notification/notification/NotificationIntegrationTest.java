package com.resiliencelab.notification.notification;

import com.resiliencelab.notification.messaging.OrderEventMessage;
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
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
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
class NotificationIntegrationTest {
    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

    @Autowired private NotificationService service;
    @Autowired private NotificationRepository repository;
    @Autowired private MockMvc mockMvc;

    @Test
    void concurrentDeliveriesOfTheSameEventRecordASingleNotification() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderEventMessage message = confirmed(UUID.randomUUID(), orderId);
        List<Boolean> results = runConcurrently(10, () -> service.notifyOnce(message));
        assertThat(results.stream().filter(recorded -> recorded).count()).isEqualTo(1);
        assertThat(repository.countByOrderId(orderId)).isEqualTo(1);
    }
    @Test
    void concurrentEventsOfTheSameOrderAndTypeRecordASingleNotification() throws Exception {
        UUID orderId = UUID.randomUUID();
        List<Boolean> results = runConcurrently(10, () -> service.notifyOnce(confirmed(UUID.randomUUID(), orderId)));
        assertThat(results.stream().filter(recorded -> recorded).count()).isEqualTo(1);
        assertThat(repository.countByOrderId(orderId)).isEqualTo(1);
    }
    @Test
    void exposesTheNotificationsOfAnOrderAndReadsOneById() throws Exception {
        UUID orderId = UUID.randomUUID();
        service.notifyOnce(confirmed(UUID.randomUUID(), orderId));
        UUID notificationId = repository.findByOrderIdOrderByCreatedAtAsc(orderId).get(0).getId();

        mockMvc.perform(get("/api/v1/notifications/orders/" + orderId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("ORDER_CONFIRMED"))
                .andExpect(jsonPath("$[0].recipient").value("student@example.com"))
                .andExpect(jsonPath("$[0].subject").value("Your order is confirmed"));

        mockMvc.perform(get("/api/v1/notifications/" + notificationId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orderId").value(orderId.toString()));
    }
    @Test
    void returnsAnEmptyListForAnOrderWithoutNotification() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/orders/" + UUID.randomUUID()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$").isEmpty());
    }
    @Test
    void returnsNotFoundForAnUnknownNotification() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/" + UUID.randomUUID()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }
    @Test
    void rejectsAMalformedIdentifier() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/not-a-uuid")).andExpect(status().isBadRequest());
    }
    private static OrderEventMessage confirmed(UUID eventId, UUID orderId) {
        return new OrderEventMessage(eventId, "OrderConfirmed", orderId, "student@example.com", "EUR", new BigDecimal("159.80"), null);
    }
    private static <T> List<T> runConcurrently(int threads, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) { futures.add(pool.submit(() -> { start.await(); return task.call(); })); }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) { results.add(future.get(30, TimeUnit.SECONDS)); }
            return results;
        } finally { pool.shutdownNow(); }
    }
}