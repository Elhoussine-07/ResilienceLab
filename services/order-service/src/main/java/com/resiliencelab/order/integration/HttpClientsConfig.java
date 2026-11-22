package com.resiliencelab.order.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/** One RestClient per downstream service, all with explicit connect and read timeouts. */
@Configuration
public class HttpClientsConfig {

    private final Duration connectTimeout;
    private final Duration readTimeout;

    public HttpClientsConfig(
            @Value("${services.http.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${services.http.read-timeout-ms}") long readTimeoutMs) {
        this.connectTimeout = Duration.ofMillis(connectTimeoutMs);
        this.readTimeout = Duration.ofMillis(readTimeoutMs);
    }

    @Bean
    RestClient catalogRestClient(@Value("${services.catalog.base-url}") String baseUrl) {
        return build(baseUrl);
    }

    @Bean
    RestClient inventoryRestClient(@Value("${services.inventory.base-url}") String baseUrl) {
        return build(baseUrl);
    }

    @Bean
    RestClient paymentRestClient(@Value("${services.payment.base-url}") String baseUrl) {
        return build(baseUrl);
    }

    private RestClient build(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}