package com.resiliencelab.order.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Short database transactions only. The saga itself is NOT transactional:
 * no database transaction stays open while remote services are called.
 */
@Service
public class OrderPersistenceService {

    private final OrderRepository repository;

    public OrderPersistenceService(OrderRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UUID createPending(String customerEmail, String currency, List<PricedItem> items) {
        CustomerOrder order = new CustomerOrder(customerEmail, currency, PricedItem.totalOf(items));
        items.forEach(item -> order.addItem(item.productId(), item.quantity(), item.unitPrice()));
        return repository.save(order).getId();
    }

    @Transactional
    public void markConfirmed(UUID orderId) {
        findOrThrow(orderId).confirm();
    }

    @Transactional
    public void markCancelled(UUID orderId, CancellationReason reason) {
        findOrThrow(orderId).cancel(reason);
    }

    @Transactional(readOnly = true)
    public OrderResponse getResponse(UUID orderId) {
        return repository.findWithItemsById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private CustomerOrder findOrThrow(UUID orderId) {
        return repository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}