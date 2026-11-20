package com.resiliencelab.inventory.stock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.resiliencelab.inventory.stock.InsufficientStockException;
import com.resiliencelab.inventory.stock.InventoryService;
import com.resiliencelab.inventory.stock.ReservationOutcome;
import com.resiliencelab.inventory.stock.ReserveStockRequest;
import com.resiliencelab.inventory.stock.StockResponse;

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
class StockConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

    @Autowired
    private InventoryService service;

    @Test
    void onlyOneBuyerGetsTheLastItem() throws Exception {
        UUID productId = UUID.randomUUID();
        service.setStock(productId, 1);

        List<Boolean> results = runConcurrently(20, () -> {
            try {
                service.reserve(new ReserveStockRequest(UUID.randomUUID(), productId, 1));
                return true;
            } catch (InsufficientStockException e) {
                return false;
            }
        });

        assertThat(results.stream().filter(success -> success).count()).isEqualTo(1);
        StockResponse stock = service.getStock(productId);
        assertThat(stock.availableQuantity()).isZero();
        assertThat(stock.reservedQuantity()).isEqualTo(1);
    }

    @Test
    void concurrentDuplicateReservationsCreateASingleReservation() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        service.setStock(productId, 10);

        List<ReservationOutcome> outcomes = runConcurrently(10,
                () -> service.reserve(new ReserveStockRequest(orderId, productId, 3)));

        assertThat(outcomes).hasSize(10);
        assertThat(outcomes.stream().filter(ReservationOutcome::created).count()).isEqualTo(1);
        StockResponse stock = service.getStock(productId);
        assertThat(stock.availableQuantity()).isEqualTo(7);
        assertThat(stock.reservedQuantity()).isEqualTo(3);
        assertThat(service.listReservations(orderId)).hasSize(1);
    }

    @Test
    void concurrentReleasesRestoreStockOnlyOnce() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        service.setStock(productId, 5);
        service.reserve(new ReserveStockRequest(orderId, productId, 2));

        runConcurrently(5, () -> service.release(orderId));

        StockResponse stock = service.getStock(productId);
        assertThat(stock.availableQuantity()).isEqualTo(5);
        assertThat(stock.reservedQuantity()).isZero();
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
