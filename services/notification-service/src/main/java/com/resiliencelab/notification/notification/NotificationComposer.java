package com.resiliencelab.notification.notification;

import com.resiliencelab.notification.messaging.OrderEventMessage;
import org.springframework.stereotype.Component;
import java.math.RoundingMode;

@Component
public class NotificationComposer {
    public ComposedNotification compose(OrderEventMessage message) {
        String amount = message.totalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString() + " " + message.currency();
        return switch (message.eventType()) {
            case OrderEventMessage.ORDER_CONFIRMED -> new ComposedNotification(
                    NotificationType.ORDER_CONFIRMED, "Your order is confirmed",
                    "Thank you! Your order %s for %s is confirmed and will be shipped soon.".formatted(message.orderId(), amount));
            case OrderEventMessage.ORDER_CANCELLED -> new ComposedNotification(
                    NotificationType.ORDER_CANCELLED, "Your order was cancelled",
                    "We are sorry, your order %s for %s was cancelled because %s.".formatted(message.orderId(), amount, describe(message.cancellationReason())));
            default -> throw new IllegalArgumentException("Unsupported event type: " + message.eventType());
        };
    }
    private static String describe(String cancellationReason) {
        if (cancellationReason == null) return "an unexpected problem occurred";
        return switch (cancellationReason) {
            case "STOCK_UNAVAILABLE" -> "some items are out of stock";
            case "PAYMENT_DECLINED" -> "the payment was declined";
            case "PAYMENT_UNAVAILABLE" -> "the payment could not be processed";
            case "INVENTORY_UNAVAILABLE" -> "stock availability could not be checked";
            default -> "an unexpected problem occurred";
        };
    }
}