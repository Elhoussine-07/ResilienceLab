package com.resiliencelab.order.messaging;

public interface OrderEventPublisher {

    /**
     * Publishes an order event. Best effort: this method never throws because of a broker problem,
     * so a committed order is never turned into a failure by messaging.
     */
    void publish(OrderEvent event);
}