package com.resiliencelab.order.order;

import com.jayway.jsonpath.JsonPath;
import com.resiliencelab.order.integration.CatalogClient;
import com.resiliencelab.order.integration.CatalogUnavailableException;
import com.resiliencelab.order.integration.InventoryClient;
import com.resiliencelab.order.integration.InventoryUnavailableException;
import com.resiliencelab.order.integration.PaymentClient;
import com.resiliencelab.order.integration.PaymentResult;
import com.resiliencelab.order.integration.PaymentUnavailableException;
import com.resiliencelab.order.integration.ProductInfo;
import com.resiliencelab.order.integration.StockUnavailableException;
import com.resiliencelab.order.integration.UnknownProductException;
import com.resiliencelab.order.messaging.OrderEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.password=unused-in-tests",
        "spring.rabbitmq.username=unused-in-tests",
        "spring.rabbitmq.password=unused-in-tests"})
@AutoConfigureMockMvc
@Testcontainers
class OrderApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogClient catalogClient;

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private PaymentClient paymentClient;

    
    @MockitoBean
    private OrderEventPublisher eventPublisher;

    @Test
    void confirmsAnOrderWhenStockIsReservedAndPaymentApproved() throws Exception {
        UUID productId = stubProduct("79.90");
        when(paymentClient.pay(any(), any(), any(), any())).thenReturn(new PaymentResult(true, null));

        String body = placeOrder(productId, 2)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(159.80))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andReturn().getResponse().getContentAsString();
        UUID orderId = UUID.fromString(JsonPath.read(body, "$.id"));

        verify(inventoryClient).reserve(orderId, productId, 2);
        verify(paymentClient).pay(orderId, new BigDecimal("159.80"), "EUR", "order-" + orderId + "-payment");
        verify(inventoryClient, never()).release(any(UUID.class));
    }

    @Test
    void cancelsAndReleasesStockWhenPaymentIsDeclined() throws Exception {
        UUID productId = stubProduct("79.90");
        when(paymentClient.pay(any(), any(), any(), any()))
                .thenReturn(new PaymentResult(false, "Amount exceeds the approval limit"));

        String body = placeOrder(productId, 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("PAYMENT_DECLINED"))
                .andReturn().getResponse().getContentAsString();
        UUID orderId = UUID.fromString(JsonPath.read(body, "$.id"));

        verify(inventoryClient).release(orderId);
    }

    @Test
    void cancelsAndReleasesStockWhenThePaymentServiceIsUnavailable() throws Exception {
        UUID productId = stubProduct("79.90");
        when(paymentClient.pay(any(), any(), any(), any()))
                .thenThrow(new PaymentUnavailableException("payment service is down"));

        String body = placeOrder(productId, 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("PAYMENT_UNAVAILABLE"))
                .andReturn().getResponse().getContentAsString();
        UUID orderId = UUID.fromString(JsonPath.read(body, "$.id"));

        verify(inventoryClient).release(orderId);
    }

    @Test
    void cancelsWithoutPayingWhenStockIsUnavailable() throws Exception {
        UUID productId = stubProduct("79.90");
        doThrow(new StockUnavailableException("not enough stock"))
                .when(inventoryClient).reserve(any(UUID.class), any(UUID.class), anyInt());

        String body = placeOrder(productId, 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("STOCK_UNAVAILABLE"))
                .andReturn().getResponse().getContentAsString();
        UUID orderId = UUID.fromString(JsonPath.read(body, "$.id"));

        verify(inventoryClient).release(orderId);
        verifyNoInteractions(paymentClient);
    }

    @Test
    void stillCancelsWhenTheInventoryIsDownAndTheCompensationFails() throws Exception {
        UUID productId = stubProduct("79.90");
        doThrow(new InventoryUnavailableException("inventory is down"))
                .when(inventoryClient).reserve(any(UUID.class), any(UUID.class), anyInt());
        doThrow(new InventoryUnavailableException("inventory is down"))
                .when(inventoryClient).release(any(UUID.class));

        placeOrder(productId, 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("INVENTORY_UNAVAILABLE"));

        verifyNoInteractions(paymentClient);
    }

    @Test
    void rejectsAnOrderForAnUnknownProductWithoutTouchingOtherServices() throws Exception {
        UUID productId = UUID.randomUUID();
        when(catalogClient.getProduct(productId)).thenThrow(new UnknownProductException(productId));

        placeOrder(productId, 1)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Unknown product"));

        verifyNoInteractions(inventoryClient, paymentClient);
    }

    @Test
    void answers503WhenTheCatalogIsUnavailable() throws Exception {
        UUID productId = UUID.randomUUID();
        when(catalogClient.getProduct(productId)).thenThrow(new CatalogUnavailableException("catalog is down"));

        placeOrder(productId, 1)
                .andExpect(status().isServiceUnavailable());

        verifyNoInteractions(inventoryClient, paymentClient);
    }

    @Test
    void rejectsTheSameProductTwiceInOneOrder() throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail": "student@example.com",
                                 "items": [{"productId": "%s", "quantity": 1},
                                           {"productId": "%s", "quantity": 2}]}
                                """.formatted(productId, productId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid order"));

        verifyNoInteractions(catalogClient, inventoryClient, paymentClient);
    }

    @Test
    void rejectsAnInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail": "not-an-email", "items": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerEmail").exists())
                .andExpect(jsonPath("$.errors.items").exists());
    }

    @Test
    void readsAnOrderAndReturnsNotFoundWhenUnknown() throws Exception {
        UUID productId = stubProduct("10.00");
        when(paymentClient.pay(any(), any(), any(), any())).thenReturn(new PaymentResult(true, null));
        String body = placeOrder(productId, 1)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/v1/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(get("/api/v1/orders/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
    
        @Test
    void publishesAnOrderConfirmedEvent() throws Exception {
        UUID productId = stubProduct("79.90");
        when(paymentClient.pay(any(), any(), any(), any())).thenReturn(new PaymentResult(true, null));

        String body = placeOrder(productId, 1)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID orderId = UUID.fromString(JsonPath.read(body, "$.id"));

        verify(eventPublisher).publish(argThat(event ->
                event.eventType().equals("OrderConfirmed")
                        && event.orderId().equals(orderId)
                        && event.cancellationReason() == null));
    }

    @Test
    void publishesAnOrderCancelledEventWhenPaymentIsDeclined() throws Exception {
        UUID productId = stubProduct("79.90");
        when(paymentClient.pay(any(), any(), any(), any()))
                .thenReturn(new PaymentResult(false, "Amount exceeds the approval limit"));

        String body = placeOrder(productId, 1)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID orderId = UUID.fromString(JsonPath.read(body, "$.id"));

        verify(eventPublisher).publish(argThat(event ->
                event.eventType().equals("OrderCancelled")
                        && event.orderId().equals(orderId)
                        && "PAYMENT_DECLINED".equals(event.cancellationReason())));
    }

    @Test
    void publishesNothingWhenTheOrderIsRejectedBeforeCreation() throws Exception {
        UUID productId = UUID.randomUUID();
        when(catalogClient.getProduct(productId)).thenThrow(new UnknownProductException(productId));

        placeOrder(productId, 1).andExpect(status().isUnprocessableEntity());

        verifyNoInteractions(eventPublisher);
    }

    private UUID stubProduct(String price) {
        UUID productId = UUID.randomUUID();
        when(catalogClient.getProduct(productId))
                .thenReturn(new ProductInfo(productId, "Mechanical keyboard", new BigDecimal(price)));
        return productId;
    }

    private ResultActions placeOrder(UUID productId, int quantity) throws Exception {
        return mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"customerEmail": "student@example.com",
                         "items": [{"productId": "%s", "quantity": %d}]}
                        """.formatted(productId, quantity)));
    }
}