package com.resiliencelab.shipping.messaging;

import com.resiliencelab.shipping.shipment.Shipment;
import com.resiliencelab.shipping.shipment.ShipmentRepository;
import com.resiliencelab.shipping.shipment.ShipmentStatus;
import com.resiliencelab.shipping.shipment.ProcessedEventRepository;
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
class ShipmentMessagingIntegrationTest {

    private static final String EXCHANGE = "orders.events";
    private static final String ROUTING_KEY = "order.confirmed";

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
    @Container @ServiceConnection
    static final RabbitMQContainer RABBIT = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management"));

    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private RabbitAdmin rabbitAdmin;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void declareTopology() { rabbitAdmin.initialize(); }

    @Test
    void createsAShipmentWhenAnOrderIsConfirmed() {
        UUID orderId = UUID.randomUUID();
        sendOrderConfirmed(UUID.randomUUID(), orderId);
        awaitShipmentFor(orderId);
        Shipment shipment = shipmentRepository.findByOrderId(orderId).orElseThrow();
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(shipment.getItemCount()).isEqualTo(3);
        assertThat(shipment.getTrackingNumber()).startsWith("TRK-");
    }

    @Test
    void ignoresDuplicateDeliveriesOfTheSameEvent() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        for (int i = 0; i < 3; i++) { sendOrderConfirmed(eventId, orderId); }
        UUID sentinelOrderId = UUID.randomUUID();
        sendOrderConfirmed(UUID.randomUUID(), sentinelOrderId);
        awaitShipmentFor(sentinelOrderId);
        assertThat(shipmentRepository.countByOrderId(orderId)).isEqualTo(1);
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    void createsASingleShipmentWhenTheSameOrderArrivesWithDifferentEventIds() {
        UUID orderId = UUID.randomUUID();
        UUID firstEventId = UUID.randomUUID();
        UUID secondEventId = UUID.randomUUID();
        sendOrderConfirmed(firstEventId, orderId);
        sendOrderConfirmed(secondEventId, orderId);
        UUID sentinelOrderId = UUID.randomUUID();
        sendOrderConfirmed(UUID.randomUUID(), sentinelOrderId);
        awaitShipmentFor(sentinelOrderId);
        assertThat(shipmentRepository.countByOrderId(orderId)).isEqualTo(1);
        assertThat(processedEventRepository.existsById(firstEventId)).isTrue();
        assertThat(processedEventRepository.existsById(secondEventId)).isTrue();
    }

    @Test
    void keepsConsumingAfterInvalidMessages() {
        send("{\"eventType\": \"OrderConfirmed\"}", UUID.randomUUID().toString());
        send("this is not json", UUID.randomUUID().toString());
        UUID orderId = UUID.randomUUID();
        sendOrderConfirmed(UUID.randomUUID(), orderId);
        awaitShipmentFor(orderId);
    }

    private void awaitShipmentFor(UUID orderId) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(
                () -> assertThat(shipmentRepository.countByOrderId(orderId)).isEqualTo(1));
    }

    private void sendOrderConfirmed(UUID eventId, UUID orderId) {
        String json = """
                {"eventId": "%s", "eventType": "OrderConfirmed", "occurredAt": "2026-11-20T10:15:30.123456Z",
                 "orderId": "%s", "customerEmail": "student@example.com", "currency": "EUR",
                 "totalAmount": 239.70, "cancellationReason": null,
                 "items": [{"productId": "%s", "quantity": 2, "unitPrice": 79.90},
                           {"productId": "%s", "quantity": 1, "unitPrice": 79.90}]}
                """.formatted(eventId, orderId, UUID.randomUUID(), UUID.randomUUID());
        send(json, eventId.toString());
    }

    private void send(String json, String messageId) {
        MessageProperties properties = MessagePropertiesBuilder.newInstance()
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(messageId)
                .setHeader("__TypeId__", "com.resiliencelab.order.messaging.OrderEvent")
                .build();
        rabbitTemplate.send(EXCHANGE, ROUTING_KEY, new Message(json.getBytes(StandardCharsets.UTF_8), properties));
    }
}
