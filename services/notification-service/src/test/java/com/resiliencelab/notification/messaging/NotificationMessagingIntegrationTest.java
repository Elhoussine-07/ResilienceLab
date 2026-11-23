package com.resiliencelab.notification.messaging;

import com.resiliencelab.notification.notification.Notification;
import com.resiliencelab.notification.notification.NotificationRepository;
import com.resiliencelab.notification.notification.NotificationType;
import com.resiliencelab.notification.notification.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "spring.datasource.password=unused-in-tests",
        "spring.rabbitmq.username=unused-in-tests",
        "spring.rabbitmq.password=unused-in-tests"})
@Testcontainers
class NotificationMessagingIntegrationTest {
    private static final String EXCHANGE = "orders.events";
    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
    @Container @ServiceConnection
    static final RabbitMQContainer RABBIT = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management"));

    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private RabbitAdmin rabbitAdmin;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void declareTopology() { rabbitAdmin.initialize(); }

    @Test
    void recordsAConfirmationWhenAnOrderIsConfirmed() {
        UUID orderId = UUID.randomUUID();
        send("order.confirmed", event(UUID.randomUUID(), "OrderConfirmed", orderId, null));
        awaitNotificationFor(orderId);
        Notification notification = notificationRepository.findByOrderIdOrderByCreatedAtAsc(orderId).get(0);
        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.ORDER_CONFIRMED);
        assertThat(notification.getRecipient()).isEqualTo("student@example.com");
        assertThat(notification.getBody()).contains("239.70 EUR");
    }
    @Test
    void recordsACancellationWithItsReasonWhenAnOrderIsCancelled() {
        UUID orderId = UUID.randomUUID();
        send("order.cancelled", event(UUID.randomUUID(), "OrderCancelled", orderId, "STOCK_UNAVAILABLE"));
        awaitNotificationFor(orderId);
        Notification notification = notificationRepository.findByOrderIdOrderByCreatedAtAsc(orderId).get(0);
        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.ORDER_CANCELLED);
        assertThat(notification.getBody()).contains("some items are out of stock");
    }
    @Test
    void ignoresDuplicateDeliveriesOfTheSameEvent() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        for (int i = 0; i < 3; i++) { send("order.confirmed", event(eventId, "OrderConfirmed", orderId, null)); }
        UUID sentinelOrderId = UUID.randomUUID();
        send("order.confirmed", event(UUID.randomUUID(), "OrderConfirmed", sentinelOrderId, null));
        awaitNotificationFor(sentinelOrderId);
        assertThat(notificationRepository.countByOrderId(orderId)).isEqualTo(1);
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }
    @Test
    void keepsConsumingAfterInvalidMessages() {
        UUID eventId = UUID.randomUUID();
        send("order.confirmed", event(eventId, "OrderShipped", UUID.randomUUID(), null));
        send("order.cancelled", "this is not json");
        UUID orderId = UUID.randomUUID();
        send("order.confirmed", event(UUID.randomUUID(), "OrderConfirmed", orderId, null));
        awaitNotificationFor(orderId);
        assertThat(processedEventRepository.existsById(eventId)).isFalse();
    }
    private void awaitNotificationFor(UUID orderId) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(notificationRepository.countByOrderId(orderId)).isEqualTo(1));
    }
    private static String event(UUID eventId, String eventType, UUID orderId, String cancellationReason) {
        String reason = cancellationReason == null ? "null" : "\"" + cancellationReason + "\"";
        return """
                {"eventId": "%s", "eventType": "%s", "occurredAt": "2026-11-20T10:15:30.123456Z",
                 "orderId": "%s", "customerEmail": "student@example.com", "currency": "EUR",
                 "totalAmount": 239.70, "cancellationReason": %s,
                 "items": [{"productId": "%s", "quantity": 3, "unitPrice": 79.90}]}
                """.formatted(eventId, eventType, orderId, reason, UUID.randomUUID());
    }
    private void send(String routingKey, String json) {
        MessageProperties properties = MessagePropertiesBuilder.newInstance()
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(UUID.randomUUID().toString())
                .setHeader("__TypeId__", "com.resiliencelab.order.messaging.OrderEvent")
                .build();
        rabbitTemplate.send(EXCHANGE, routingKey, new Message(json.getBytes(StandardCharsets.UTF_8), properties));
    }
}