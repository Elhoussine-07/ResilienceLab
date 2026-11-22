package com.resiliencelab.order.order;

import com.resiliencelab.order.integration.CatalogClient;
import com.resiliencelab.order.integration.InventoryClient;
import com.resiliencelab.order.integration.InventoryUnavailableException;
import com.resiliencelab.order.integration.PaymentClient;
import com.resiliencelab.order.integration.PaymentResult;
import com.resiliencelab.order.integration.PaymentUnavailableException;
import com.resiliencelab.order.integration.ProductInfo;
import com.resiliencelab.order.integration.StockUnavailableException;
import com.resiliencelab.order.messaging.OrderEvent;
import com.resiliencelab.order.messaging.OrderEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates the order saga:
 * price items, persist PENDING, reserve stock, pay, then confirm or cancel with compensation,
 * and finally publish the outcome as an event.
 *
 * Deliberately NOT annotated with @Transactional: each state change is its own short
 * transaction (see {@link OrderPersistenceService}) so that no database connection is held
 * while waiting for other services or the broker.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final CatalogClient catalogClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final OrderPersistenceService persistence;
    private final OrderEventPublisher eventPublisher;
    private final String currency;

    public OrderService(
            CatalogClient catalogClient,
            InventoryClient inventoryClient,
            PaymentClient paymentClient,
            OrderPersistenceService persistence,
            OrderEventPublisher eventPublisher,
            @Value("${order.currency:EUR}") String currency) {
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.persistence = persistence;
        this.eventPublisher = eventPublisher;
        this.currency = currency;
    }

    public OrderResponse create(CreateOrderRequest request) {
        List<PricedItem> items = priceItems(request);
        BigDecimal total = PricedItem.totalOf(items);
        UUID orderId = persistence.createPending(request.customerEmail(), currency, items);
        log.info("Order {} created (total {} {}), starting saga", orderId, total, currency);

        // Step 1: reserve stock
        try {
            for (PricedItem item : items) {
                inventoryClient.reserve(orderId, item.productId(), item.quantity());
            }
        } catch (StockUnavailableException e) {
            return cancel(orderId, CancellationReason.STOCK_UNAVAILABLE);
        } catch (InventoryUnavailableException e) {
            return cancel(orderId, CancellationReason.INVENTORY_UNAVAILABLE);
        }

        // Step 2: pay (the key is derived from the order, so any retry is safe)
        PaymentResult payment;
        try {
            payment = paymentClient.pay(orderId, total, currency, paymentKey(orderId));
        } catch (PaymentUnavailableException e) {
            return cancel(orderId, CancellationReason.PAYMENT_UNAVAILABLE);
        }

        if (!payment.approved()) {
            return cancel(orderId, CancellationReason.PAYMENT_DECLINED);
        }

        // Step 3: confirm, then announce
        persistence.markConfirmed(orderId);
        OrderResponse confirmed = persistence.getResponse(orderId);
        log.info("Order {} confirmed", orderId);
        eventPublisher.publish(OrderEvent.from(confirmed));
        return confirmed;
    }

    public OrderResponse getById(UUID orderId) {
        return persistence.getResponse(orderId);
    }

    private List<PricedItem> priceItems(CreateOrderRequest request) {
        Set<UUID> seen = new HashSet<>();
        for (CreateOrderRequest.Item item : request.items()) {
            if (!seen.add(item.productId())) {
                throw new InvalidOrderException(
                        "Product " + item.productId() + " appears more than once; merge the quantities");
            }
        }

        List<PricedItem> priced = new ArrayList<>();
        for (CreateOrderRequest.Item item : request.items()) {
            ProductInfo product = catalogClient.getProduct(item.productId());
            priced.add(new PricedItem(
                    item.productId(),
                    item.quantity(),
                    product.price().setScale(2, RoundingMode.HALF_UP)));
        }
        return priced;
    }

    /** Compensation: give the stock back (best effort), record the cancellation, then announce it. */
    private OrderResponse cancel(UUID orderId, CancellationReason reason) {
        log.warn("Order {} cancelled: {}", orderId, reason);
        try {
            inventoryClient.release(orderId);
        } catch (InventoryUnavailableException e) {
            log.warn("Could not release the stock of order {}: {}", orderId, e.getMessage());
        }
        persistence.markCancelled(orderId, reason);
        OrderResponse cancelled = persistence.getResponse(orderId);
        eventPublisher.publish(OrderEvent.from(cancelled));
        return cancelled;
    }

    private static String paymentKey(UUID orderId) {
        return "order-" + orderId + "-payment";
    }
}