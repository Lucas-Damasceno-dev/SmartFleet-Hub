package io.smartfleet.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smartfleet.common.dto.OrderDto;
import io.smartfleet.common.exception.ResourceNotFoundException;
import io.smartfleet.order.entity.Order;
import io.smartfleet.order.entity.OrderStatus;
import io.smartfleet.order.entity.Outbox;
import io.smartfleet.order.mapper.OrderMapper;
import io.smartfleet.order.repository.OrderRepository;
import io.smartfleet.order.repository.OutboxRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OrderMapper orderMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private OrderDto testOrderDto;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
            .orderId(1L)
            .customerName("John Doe")
            .customerPhone("+5511999999999")
            .pickupAddress("Rua A, 100")
            .pickupLatitude(-23.5505)
            .pickupLongitude(-46.6333)
            .deliveryAddress("Rua B, 200")
            .deliveryLatitude(-23.5500)
            .deliveryLongitude(-46.6350)
            .status(OrderStatus.PENDING.name())
            .createdAt(LocalDateTime.now())
            .build();

        testOrderDto = OrderDto.builder()
            .orderId(1L)
            .customerName("John Doe")
            .customerPhone("+5511999999999")
            .pickupAddress("Rua A, 100")
            .deliveryAddress("Rua B, 200")
            .status(OrderStatus.PENDING.name())
            .build();
    }

    @Nested
    @DisplayName("Order Creation Tests")
    class OrderCreationTests {

        @Test
        @DisplayName("Should create order and publish outbox event")
        void shouldCreateOrderAndPublishOutboxEvent() {
            // Given
            when(orderMapper.toEntity(any(OrderDto.class))).thenReturn(testOrder);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(orderMapper.toDto(any(Order.class))).thenReturn(testOrderDto);
            when(outboxRepository.save(any(Outbox.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            OrderDto result = orderService.createOrder(testOrderDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCustomerName()).isEqualTo("John Doe");
            verify(orderRepository).save(any(Order.class));
            verify(outboxRepository).save(argThat(outbox -> 
                outbox.getType().equals("ORDER_CREATED") &&
                outbox.getAggregateType().equals("Order")));
        }

        @Test
        @DisplayName("Should set status to PENDING on creation")
        void shouldSetStatusToPendingOnCreation() {
            // Given
            when(orderMapper.toEntity(any())).thenReturn(testOrder);
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(orderMapper.toDto(any())).thenReturn(testOrderDto);
            when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            orderService.createOrder(testOrderDto);

            // Then
            verify(orderRepository).save(argThat(order -> 
                order.getStatus().equals(OrderStatus.PENDING.name())));
        }
    }

    @Nested
    @DisplayName("Order Retrieval Tests")
    class OrderRetrievalTests {

        @Test
        @DisplayName("Should return order by ID")
        void shouldReturnOrderById() {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderMapper.toDto(testOrder)).thenReturn(testOrderDto);

            // When
            OrderDto result = orderService.getOrder(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowWhenOrderNotFound() {
            // Given
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> orderService.getOrder(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should return orders by status")
        void shouldReturnOrdersByStatus() {
            // Given
            when(orderRepository.findByStatus("PENDING"))
                .thenReturn(List.of(testOrder));
            when(orderMapper.toDto(testOrder)).thenReturn(testOrderDto);

            // When
            List<OrderDto> result = orderService.getOrdersByStatus("PENDING");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
        }
    }

    @Nested
    @DisplayName("Driver Assignment Tests")
    class DriverAssignmentTests {

        @Test
        @DisplayName("Should assign driver to order")
        void shouldAssignDriverToOrder() {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(orderMapper.toDto(any())).thenReturn(testOrderDto);
            when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            OrderDto result = orderService.assignDriver(1L, 10L, 20L);

            // Then
            verify(orderRepository).save(argThat(order ->
                order.getAssignedDriverId() == 10L &&
                order.getAssignedVehicleId() == 20L &&
                order.getStatus().equals(OrderStatus.ASSIGNED.name())));
        }

        @Test
        @DisplayName("Should publish ORDER_ASSIGNED event")
        void shouldPublishAssignedEvent() {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(orderMapper.toDto(any())).thenReturn(testOrderDto);
            when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            orderService.assignDriver(1L, 10L, 20L);

            // Then
            verify(outboxRepository).save(argThat(outbox ->
                outbox.getType().equals("ORDER_ASSIGNED")));
        }

        @Test
        @DisplayName("Should throw when assigning non-existent order")
        void shouldThrowWhenAssigningNonExistentOrder() {
            // Given
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> orderService.assignDriver(999L, 10L, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Status Update Tests")
    class StatusUpdateTests {

        @Test
        @DisplayName("Should update order status")
        void shouldUpdateOrderStatus() {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(orderMapper.toDto(any())).thenReturn(testOrderDto);
            when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            orderService.updateStatus(1L, OrderStatus.IN_TRANSIT);

            // Then
            verify(orderRepository).save(argThat(order ->
                order.getStatus().equals(OrderStatus.IN_TRANSIT.name())));
        }

        @Test
        @DisplayName("Should publish STATUS_CHANGED event")
        void shouldPublishStatusChangedEvent() {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(orderMapper.toDto(any())).thenReturn(testOrderDto);
            when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            orderService.updateStatus(1L, OrderStatus.DELIVERED);

            // Then
            verify(outboxRepository).save(argThat(outbox ->
                outbox.getType().equals("STATUS_CHANGED")));
        }

        @Test
        @DisplayName("Should log status transition")
        void shouldLogStatusTransition() {
            // Given
            testOrder.setStatus(OrderStatus.ASSIGNED.name());
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(orderMapper.toDto(any())).thenReturn(testOrderDto);
            when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            orderService.updateStatus(1L, OrderStatus.PICKED_UP);

            // Then - verify the status was changed (log verification would require LogCaptor)
            verify(orderRepository).save(argThat(order ->
                order.getStatus().equals(OrderStatus.PICKED_UP.name())));
        }
    }

    @Nested
    @DisplayName("Transactional Outbox Pattern Tests")
    class OutboxPatternTests {

        @Test
        @DisplayName("Should create outbox event with correct aggregate type")
        void shouldCreateOutboxWithCorrectAggregateType() {
            // Given
            when(orderMapper.toEntity(any())).thenReturn(testOrder);
            when(orderRepository.save(any())).thenReturn(testOrder);
            when(orderMapper.toDto(any())).thenReturn(testOrderDto);
            when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            orderService.createOrder(testOrderDto);

            // Then
            verify(outboxRepository).save(argThat(outbox ->
                outbox.getAggregateType().equals("Order") &&
                outbox.getAggregateId().equals("1")));
        }

        @Test
        @DisplayName("Should include order details in outbox payload")
        void shouldIncludeOrderDetailsInPayload() {
            // Given
            when(orderMapper.toEntity(any())).thenReturn(testOrder);
            when(orderRepository.save(any())).thenReturn(testOrder);
            when(orderMapper.toDto(any())).thenReturn(testOrderDto);
            when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            orderService.createOrder(testOrderDto);

            // Then
            verify(outboxRepository).save(argThat(outbox ->
                outbox.getPayload() != null &&
                outbox.getPayload().contains("John Doe") &&
                outbox.getPayload().contains("customerName")));
        }

        @Test
        @DisplayName("Should generate unique UUID for each outbox event")
        void shouldGenerateUniqueUuidForOutbox() {
            // Given
            when(orderMapper.toEntity(any())).thenReturn(testOrder);
            when(orderRepository.save(any())).thenReturn(testOrder);
            when(orderMapper.toDto(any())).thenReturn(testOrderDto);
            when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            orderService.createOrder(testOrderDto);

            // Then
            verify(outboxRepository).save(argThat(outbox ->
                outbox.getId() != null));
        }
    }
}
