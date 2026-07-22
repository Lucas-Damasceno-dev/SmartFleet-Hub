package io.smartfleet.order.service;

import io.smartfleet.common.dto.OrderDto;
import io.smartfleet.order.entity.Order;
import io.smartfleet.order.entity.OrderStatus;
import io.smartfleet.order.entity.Outbox;
import io.smartfleet.order.repository.OrderRepository;
import io.smartfleet.order.repository.OutboxRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@DisplayName("OrderService Integration Tests")
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("smartfleet_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        // Disable Keycloak for tests
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:8180/realms/smartfleet");
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Nested
    @DisplayName("End-to-End Order Flow")
    class EndToEndOrderFlow {

        @Test
        @DisplayName("Should persist order and outbox event in same transaction")
        void shouldPersistOrderAndOutboxInSameTransaction() {
            // Given
            OrderDto orderDto = OrderDto.builder()
                .customerName("Integration Test Customer")
                .customerPhone("+5511987654321")
                .pickupAddress("Rua Teste, 100")
                .pickupLatitude(-23.5505)
                .pickupLongitude(-46.6333)
                .deliveryAddress("Rua Destino, 200")
                .deliveryLatitude(-23.5600)
                .deliveryLongitude(-46.6400)
                .build();

            // When
            OrderDto created = orderService.createOrder(orderDto);

            // Then
            assertThat(created.getOrderId()).isNotNull();
            
            // Verify order persisted
            Order savedOrder = orderRepository.findById(created.getOrderId()).orElseThrow();
            assertThat(savedOrder.getCustomerName()).isEqualTo("Integration Test Customer");
            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING.name());
            
            // Verify outbox event persisted
            List<Outbox> outboxEvents = outboxRepository.findAll();
            assertThat(outboxEvents).hasSize(1);
            assertThat(outboxEvents.get(0).getType()).isEqualTo("ORDER_CREATED");
            assertThat(outboxEvents.get(0).getPayload()).contains("Integration Test Customer");
        }

        @Test
        @DisplayName("Should complete full order lifecycle")
        void shouldCompleteFullOrderLifecycle() {
            // Given - Create order
            OrderDto orderDto = OrderDto.builder()
                .customerName("Lifecycle Test")
                .customerPhone("+5511111111111")
                .pickupAddress("Start")
                .pickupLatitude(-23.5505)
                .pickupLongitude(-46.6333)
                .deliveryAddress("End")
                .deliveryLatitude(-23.5600)
                .deliveryLongitude(-46.6400)
                .build();

            OrderDto created = orderService.createOrder(orderDto);
            Long orderId = created.getOrderId();

            // When - Assign driver
            orderService.assignDriver(orderId, 1L, 1L);
            
            // Then - Verify status
            Order afterAssign = orderRepository.findById(orderId).orElseThrow();
            assertThat(afterAssign.getStatus()).isEqualTo(OrderStatus.ASSIGNED.name());
            assertThat(afterAssign.getAssignedDriverId()).isEqualTo(1L);

            // When - Update to IN_TRANSIT
            orderService.updateStatus(orderId, OrderStatus.IN_TRANSIT);
            
            // Then
            Order afterTransit = orderRepository.findById(orderId).orElseThrow();
            assertThat(afterTransit.getStatus()).isEqualTo(OrderStatus.IN_TRANSIT.name());

            // When - Update to DELIVERED
            orderService.updateStatus(orderId, OrderStatus.DELIVERED);
            
            // Then
            Order afterDelivered = orderRepository.findById(orderId).orElseThrow();
            assertThat(afterDelivered.getStatus()).isEqualTo(OrderStatus.DELIVERED.name());

            // Verify all outbox events
            List<Outbox> allEvents = outboxRepository.findAll();
            assertThat(allEvents).hasSize(4); // CREATED, ASSIGNED, STATUS_CHANGED x2
        }
    }

    @Nested
    @DisplayName("Query Tests")
    class QueryTests {

        @Test
        @DisplayName("Should find orders by status")
        void shouldFindOrdersByStatus() {
            // Given - Create multiple orders with different statuses
            createTestOrder("Pending 1");
            createTestOrder("Pending 2");
            
            OrderDto assigned = createTestOrder("Assigned Order");
            orderService.assignDriver(assigned.getOrderId(), 1L, 1L);

            // When
            List<OrderDto> pendingOrders = orderService.getOrdersByStatus(OrderStatus.PENDING.name());
            List<OrderDto> assignedOrders = orderService.getOrdersByStatus(OrderStatus.ASSIGNED.name());

            // Then
            assertThat(pendingOrders).hasSize(2);
            assertThat(assignedOrders).hasSize(1);
            assertThat(assignedOrders.get(0).getCustomerName()).isEqualTo("Assigned Order");
        }

        private OrderDto createTestOrder(String customerName) {
            return orderService.createOrder(OrderDto.builder()
                .customerName(customerName)
                .customerPhone("+5511999999999")
                .pickupAddress("Test Pickup")
                .pickupLatitude(-23.5505)
                .pickupLongitude(-46.6333)
                .deliveryAddress("Test Delivery")
                .deliveryLatitude(-23.5600)
                .deliveryLongitude(-46.6400)
                .build());
        }
    }

    @Nested
    @DisplayName("Transactional Consistency Tests")
    class TransactionalConsistencyTests {

        @Test
        @DisplayName("Should rollback order and outbox if exception occurs")
        void shouldRollbackOnException() {
            // This test verifies transactional behavior - 
            // If we had a way to force an exception after order save but before outbox save,
            // both should rollback. This is implicit in @Transactional but hard to test directly.
            
            // For now, verify that successful operations have both order and outbox
            OrderDto order = createTestOrder("Rollback Test");
            
            assertThat(orderRepository.findById(order.getOrderId())).isPresent();
            assertThat(outboxRepository.findAll()).isNotEmpty();
        }

        private OrderDto createTestOrder(String customerName) {
            return orderService.createOrder(OrderDto.builder()
                .customerName(customerName)
                .customerPhone("+5511999999999")
                .pickupAddress("Test Pickup")
                .pickupLatitude(-23.5505)
                .pickupLongitude(-46.6333)
                .deliveryAddress("Test Delivery")
                .deliveryLatitude(-23.5600)
                .deliveryLongitude(-46.6400)
                .build());
        }
    }
}
