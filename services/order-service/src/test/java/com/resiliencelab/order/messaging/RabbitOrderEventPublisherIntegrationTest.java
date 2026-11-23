package com.resiliencelab.order.messaging;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.password=unused-in-tests",
        "spring.rabbitmq.username=unused-in-tests",
        "spring.rabbitmq.password=unused-in-tests" })
@Testcontainers
class RabbitOrderEventPublisherIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBIT = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management"));

    @Autowired
    private OrderEventPublisher publisher;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TopicExchange ordersExchange;

    @Test
    void deliversAConfirmedEventToABoundQueue() {
        Queue queue = declareBoundQueue();
        OrderEvent event = OrderEventFixtures.sampleEvent(OrderEvent.ORDER_CONFIRMED, null);

        publisher.publish(event);

        Message message = rabbitTemplate.receive(queue.getName(), 5000);
        assertThat(message).isNotNull();
        assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo("order.confirmed");
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(event.eventId().toString());
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(message.getMessageProperties().getReceivedDeliveryMode())
                .isEqualTo(MessageDeliveryMode.PERSISTENT);

        String json = new String(message.getBody(), StandardCharsets.UTF_8);
        assertThat(JsonPath.<String>read(json, "$.eventType")).isEqualTo("OrderConfirmed");
        assertThat(JsonPath.<String>read(json, "$.orderId")).isEqualTo(event.orderId().toString());
        assertThat(JsonPath.<String>read(json, "$.occurredAt")).isEqualTo(event.occurredAt());
        assertThat(JsonPath.<Integer>read(json, "$.items[0].quantity")).isEqualTo(2);
    }

    @Test
    void deliversACancelledEventWithItsReason() {
        Queue queue = declareBoundQueue();
        OrderEvent event = OrderEventFixtures.sampleEvent(OrderEvent.ORDER_CANCELLED, "PAYMENT_DECLINED");

        publisher.publish(event);

        Message message = rabbitTemplate.receive(queue.getName(), 5000);
        assertThat(message).isNotNull();
        assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo("order.cancelled");

        String json = new String(message.getBody(), StandardCharsets.UTF_8);
        assertThat(JsonPath.<String>read(json, "$.eventType")).isEqualTo("OrderCancelled");
        assertThat(JsonPath.<String>read(json, "$.cancellationReason")).isEqualTo("PAYMENT_DECLINED");
    }

    /**
     * What a consumer would do: declare its own queue and bind it to the exchange.
     */
    private Queue declareBoundQueue() {
        rabbitAdmin.initialize();
        Queue queue = new Queue("test-" + UUID.randomUUID(), true, false, true);
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(ordersExchange).with("order.#"));
        return queue;
    }
}