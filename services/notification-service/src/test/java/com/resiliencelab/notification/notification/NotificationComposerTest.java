package com.resiliencelab.notification.notification;

import com.resiliencelab.notification.messaging.OrderEventMessage;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationComposerTest {
    private final NotificationComposer composer = new NotificationComposer();

    @Test
    void composesAConfirmationWithTheOrderAndTheAmount() {
        UUID orderId = UUID.randomUUID();
        OrderEventMessage message = message("OrderConfirmed", orderId, null);
        ComposedNotification composed = composer.compose(message);
        assertThat(composed.type()).isEqualTo(NotificationType.ORDER_CONFIRMED);
        assertThat(composed.subject()).isEqualTo("Your order is confirmed");
        assertThat(composed.body()).contains(orderId.toString()).contains("159.80 EUR");
    }
    @Test
    void composesACancellationWithAReadableReason() {
        OrderEventMessage message = message("OrderCancelled", UUID.randomUUID(), "STOCK_UNAVAILABLE");
        ComposedNotification composed = composer.compose(message);
        assertThat(composed.type()).isEqualTo(NotificationType.ORDER_CANCELLED);
        assertThat(composed.body()).contains("some items are out of stock");
    }
    @Test
    void fallsBackToAGenericReasonWhenTheReasonIsUnknown() {
        ComposedNotification unknown = composer.compose(message("OrderCancelled", UUID.randomUUID(), "SOMETHING_NEW"));
        ComposedNotification missing = composer.compose(message("OrderCancelled", UUID.randomUUID(), null));
        assertThat(unknown.body()).contains("an unexpected problem occurred");
        assertThat(missing.body()).contains("an unexpected problem occurred");
    }
    private static OrderEventMessage message(String eventType, UUID orderId, String cancellationReason) {
        return new OrderEventMessage(UUID.randomUUID(), eventType, orderId, "student@example.com", "EUR", new BigDecimal("159.8"), cancellationReason);
    }
}