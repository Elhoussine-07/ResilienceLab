package com.resiliencelab.notification_service;

import com.resiliencelab.notification.NotificationServiceApplication; // <-- L'IMPORT IMPORTANT
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

// On force Spring à charger la bonne classe principale
@SpringBootTest(classes = NotificationServiceApplication.class, properties = {
		"spring.rabbitmq.listener.simple.auto-startup=false"
})
@Testcontainers
class NotificationServiceApplicationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

	@Container
	@ServiceConnection
	static final RabbitMQContainer RABBIT = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management"));

	@Test
	void contextLoads() {
	}
}