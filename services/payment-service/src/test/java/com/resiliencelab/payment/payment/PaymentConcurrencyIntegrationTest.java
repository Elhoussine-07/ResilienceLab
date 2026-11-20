package com.resiliencelab.payment.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
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

@SpringBootTest(properties = "spring.datasource.password=unused-in-tests")
@Testcontainers
class PaymentConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

    @Autowired
    private PaymentService service;

    @Test
    void concurrentDuplicateRequestsRecordASinglePayment() throws Exception {
        UUID orderId = UUID.randomUUID();
        String key = "key-" + UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(orderId, new BigDecimal("49.90"), "EUR");

        List<PaymentOutcome> outcomes = runConcurrently(10, () -> service.process(key, request));

        assertThat(outcomes).hasSize(10);
        assertThat(outcomes.stream().filter(PaymentOutcome::created).count()).isEqualTo(1);
        assertThat(outcomes.stream().map(outcome -> outcome.payment().id()).distinct().count()).isEqualTo(1);
        assertThat(service.getByOrderId(orderId).id()).isEqualTo(outcomes.get(0).payment().id());
    }

    @Test
    void concurrentRequestsWithDifferentKeysForTheSameOrderChargeOnlyOnce() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(orderId, new BigDecimal("49.90"), "EUR");

        List<Boolean> results = runConcurrently(10, () -> {
            try {
                service.process("key-" + UUID.randomUUID(), request);
                return true;
            } catch (PaymentConflictException e) {
                return false;
            }
        });

        assertThat(results.stream().filter(success -> success).count()).isEqualTo(1);
        assertThat(service.getByOrderId(orderId).orderId()).isEqualTo(orderId);
    }

    /** Runs the same task on several threads released at the same moment. */
    private static <T> List<T> runConcurrently(int threads, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}