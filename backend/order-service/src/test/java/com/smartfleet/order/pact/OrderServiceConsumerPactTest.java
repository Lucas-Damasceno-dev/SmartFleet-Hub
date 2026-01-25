package com.smartfleet.order.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pact Consumer Contract Test
 * Tests the contract between mobile-driver (consumer) and order-service (provider)
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "order-service")
class OrderServiceConsumerPactTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Pact(consumer = "mobile-driver", provider = "order-service")
    public RequestResponsePact getOrderByIdPact(PactDslWithProvider builder) {
        return builder
                .given("order with id 12345 exists")
                .uponReceiving("a request to get order by id")
                .path("/api/v1/orders/12345")
                .method("GET")
                .headers("Authorization", "Bearer valid-token")
                .willRespondWith()
                .status(200)
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("id", "12345")
                        .stringType("status", "ASSIGNED")
                        .object("pickup")
                            .decimalType("latitude", -23.5505)
                            .decimalType("longitude", -46.6333)
                            .stringType("address", "Rua Augusta, 100")
                        .closeObject()
                        .object("dropoff")
                            .decimalType("latitude", -23.5617)
                            .decimalType("longitude", -46.6544)
                            .stringType("address", "Av. Paulista, 1000")
                        .closeObject()
                        .stringType("driverId", "driver-001")
                        .stringType("vehicleId", "vehicle-001")
                        .timestamp("createdAt", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                )
                .toPact();
    }

    @Pact(consumer = "mobile-driver", provider = "order-service")
    public RequestResponsePact acceptOrderPact(PactDslWithProvider builder) {
        return builder
                .given("order with id 12345 is pending assignment")
                .uponReceiving("a request to accept an order")
                .path("/api/v1/orders/12345/accept")
                .method("POST")
                .headers("Authorization", "Bearer valid-token")
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
                        .stringType("driverId", "driver-001")
                )
                .willRespondWith()
                .status(200)
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("id", "12345")
                        .stringType("status", "ASSIGNED")
                        .stringType("driverId", "driver-001")
                        .booleanType("accepted", true)
                )
                .toPact();
    }

    @Pact(consumer = "mobile-driver", provider = "order-service")
    public RequestResponsePact rejectOrderPact(PactDslWithProvider builder) {
        return builder
                .given("order with id 12345 is pending assignment")
                .uponReceiving("a request to reject an order")
                .path("/api/v1/orders/12345/reject")
                .method("POST")
                .headers("Authorization", "Bearer valid-token")
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
                        .stringType("driverId", "driver-001")
                        .stringType("reason", "Too far away")
                )
                .willRespondWith()
                .status(200)
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("id", "12345")
                        .stringType("status", "SEARCHING")
                        .booleanType("accepted", false)
                )
                .toPact();
    }

    @Pact(consumer = "mobile-driver", provider = "order-service")
    public RequestResponsePact getActiveOrderForDriverPact(PactDslWithProvider builder) {
        return builder
                .given("driver-001 has an active order")
                .uponReceiving("a request to get active order for driver")
                .path("/api/v1/orders/driver/driver-001/active")
                .method("GET")
                .headers("Authorization", "Bearer valid-token")
                .willRespondWith()
                .status(200)
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("id")
                        .stringType("status", "IN_PROGRESS")
                        .object("pickup")
                            .decimalType("latitude")
                            .decimalType("longitude")
                            .stringType("address")
                        .closeObject()
                        .object("dropoff")
                            .decimalType("latitude")
                            .decimalType("longitude")
                            .stringType("address")
                        .closeObject()
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getOrderByIdPact")
    void testGetOrderById(MockServer mockServer) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer valid-token");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/v1/orders/12345",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("12345");
        assertThat(response.getBody()).contains("ASSIGNED");
    }

    @Test
    @PactTestFor(pactMethod = "acceptOrderPact")
    void testAcceptOrder(MockServer mockServer) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer valid-token");
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"driverId\": \"driver-001\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/v1/orders/12345/accept",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("ASSIGNED");
        assertThat(response.getBody()).contains("\"accepted\":true");
    }

    @Test
    @PactTestFor(pactMethod = "rejectOrderPact")
    void testRejectOrder(MockServer mockServer) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer valid-token");
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"driverId\": \"driver-001\", \"reason\": \"Too far away\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/v1/orders/12345/reject",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("SEARCHING");
        assertThat(response.getBody()).contains("\"accepted\":false");
    }

    @Test
    @PactTestFor(pactMethod = "getActiveOrderForDriverPact")
    void testGetActiveOrderForDriver(MockServer mockServer) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer valid-token");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/v1/orders/driver/driver-001/active",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("IN_PROGRESS");
    }
}
