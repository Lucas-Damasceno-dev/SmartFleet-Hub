package io.smartfleet.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smartfleet.order.entity.Order;
import io.smartfleet.order.entity.OrderStatus;
import io.smartfleet.order.repository.OrderRepository;
import io.smartfleet.order.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DispatchService.
 * Tests the Greedy-Proximity with Timeout Waterfall dispatch algorithm.
 */
@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private GeoServiceClient geoServiceClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DispatchService dispatchService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Set default configuration values
        ReflectionTestUtils.setField(dispatchService, "initialRadiusKm", 5.0);
        ReflectionTestUtils.setField(dispatchService, "maxRadiusKm", 15.0);
        ReflectionTestUtils.setField(dispatchService, "radiusIncrementKm", 5.0);
        ReflectionTestUtils.setField(dispatchService, "timeoutPerTierSeconds", 30);
        ReflectionTestUtils.setField(dispatchService, "maxAttempts", 3);

        // Create test order
        testOrder = Order.builder()
                .orderId(1L)
                .customerName("Test Customer")
                .pickupLatitude(new BigDecimal("-23.5505"))
                .pickupLongitude(new BigDecimal("-46.6333"))
                .deliveryLatitude(new BigDecimal("-23.5629"))
                .deliveryLongitude(new BigDecimal("-46.6544"))
                .pickupAddress("Test Pickup")
                .deliveryAddress("Test Delivery")
                .status(OrderStatus.PENDING.name())
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("startDispatch - State Transitions")
    class StateTransitionTests {

        @Test
        @DisplayName("should transition from PENDING to ASSIGNED when driver found")
        void shouldTransitionToAssignedWhenDriverFound() throws Exception {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
            when(outboxRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            AvailableDriver driver = createDriver(1L, "João", 2.5);
            when(geoServiceClient.findDriversWithinRadius(any(), any(), eq(5.0)))
                    .thenReturn(List.of(driver));

            // When
            CompletableFuture<DispatchService.DispatchResult> future = dispatchService.startDispatch(1L);
            DispatchService.DispatchResult result = future.get();

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.assignedDriver()).isNotNull();
            assertThat(result.assignedDriver().getDriverId()).isEqualTo(1L);

            verify(orderRepository, atLeast(2)).save(orderCaptor.capture());
            List<Order> savedOrders = orderCaptor.getAllValues();
            
            // Verify final state is ASSIGNED
            Order finalOrder = savedOrders.get(savedOrders.size() - 1);
            assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.ASSIGNED.name());
            assertThat(finalOrder.getAssignedDriverId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should return to PENDING when no driver found")
        void shouldReturnToPendingWhenNoDriverFound() throws Exception {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            // No drivers available at any radius
            when(geoServiceClient.findDriversWithinRadius(any(), any(), anyDouble()))
                    .thenReturn(List.of());

            // When
            CompletableFuture<DispatchService.DispatchResult> future = dispatchService.startDispatch(1L);
            DispatchService.DispatchResult result = future.get();

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("No available drivers");

            verify(orderRepository, atLeast(2)).save(orderCaptor.capture());
            Order finalOrder = orderCaptor.getAllValues().get(orderCaptor.getAllValues().size() - 1);
            assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.PENDING.name());
        }
    }

    @Nested
    @DisplayName("Greedy-Proximity Algorithm")
    class GreedyProximityTests {

        @Test
        @DisplayName("should assign closest driver when multiple drivers available")
        void shouldAssignClosestDriver() throws Exception {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
            when(outboxRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Multiple drivers, already sorted by distance
            List<AvailableDriver> drivers = List.of(
                    createDriver(1L, "Closest", 1.0),
                    createDriver(2L, "Medium", 3.0),
                    createDriver(3L, "Farthest", 4.5)
            );
            when(geoServiceClient.findDriversWithinRadius(any(), any(), eq(5.0)))
                    .thenReturn(drivers);

            // When
            CompletableFuture<DispatchService.DispatchResult> future = dispatchService.startDispatch(1L);
            DispatchService.DispatchResult result = future.get();

            // Then - should pick the closest (first in sorted list)
            assertThat(result.success()).isTrue();
            assertThat(result.assignedDriver().getDriverId()).isEqualTo(1L);
            assertThat(result.assignedDriver().getDriverName()).isEqualTo("Closest");
        }
    }

    @Nested
    @DisplayName("Timeout Waterfall")
    class TimeoutWaterfallTests {

        @Test
        @DisplayName("should expand radius and find driver in second tier")
        void shouldExpandRadiusWhenFirstTierEmpty() throws Exception {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
            when(outboxRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // First tier (5km): no drivers
            when(geoServiceClient.findDriversWithinRadius(any(), any(), eq(5.0)))
                    .thenReturn(List.of());
            
            // Second tier (10km): driver found
            AvailableDriver driver = createDriver(1L, "João", 8.0);
            when(geoServiceClient.findDriversWithinRadius(any(), any(), eq(10.0)))
                    .thenReturn(List.of(driver));

            // When
            CompletableFuture<DispatchService.DispatchResult> future = dispatchService.startDispatch(1L);
            DispatchService.DispatchResult result = future.get();

            // Then
            assertThat(result.success()).isTrue();
            verify(geoServiceClient).findDriversWithinRadius(any(), any(), eq(5.0));
            verify(geoServiceClient).findDriversWithinRadius(any(), any(), eq(10.0));
        }

        @Test
        @DisplayName("should try all tiers before failing")
        void shouldTryAllTiersBeforeFailing() throws Exception {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            // All tiers empty
            when(geoServiceClient.findDriversWithinRadius(any(), any(), anyDouble()))
                    .thenReturn(List.of());

            // When
            CompletableFuture<DispatchService.DispatchResult> future = dispatchService.startDispatch(1L);
            DispatchService.DispatchResult result = future.get();

            // Then
            assertThat(result.success()).isFalse();
            // Verify all 3 tiers were attempted (5km, 10km, 15km)
            verify(geoServiceClient, times(3)).findDriversWithinRadius(any(), any(), anyDouble());
        }
    }

    @Nested
    @DisplayName("tryAssignDriver")
    class TryAssignDriverTests {

        @Test
        @DisplayName("should save order with driver assignment")
        void shouldSaveOrderWithDriverAssignment() {
            // Given
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
            when(outboxRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            AvailableDriver driver = createDriver(5L, "Maria", 2.0);

            // When
            boolean result = dispatchService.tryAssignDriver(testOrder, driver);

            // Then
            assertThat(result).isTrue();
            verify(orderRepository).save(orderCaptor.capture());
            
            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getAssignedDriverId()).isEqualTo(5L);
            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.ASSIGNED.name());
        }

        @Test
        @DisplayName("should publish ORDER_ASSIGNED event to outbox")
        void shouldPublishOrderAssignedEvent() {
            // Given
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
            when(outboxRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            AvailableDriver driver = createDriver(5L, "Maria", 2.0);

            // When
            dispatchService.tryAssignDriver(testOrder, driver);

            // Then
            verify(outboxRepository).save(any());
        }

        @Test
        @DisplayName("should return false on exception")
        void shouldReturnFalseOnException() {
            // Given
            when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("DB error"));

            AvailableDriver driver = createDriver(5L, "Maria", 2.0);

            // When
            boolean result = dispatchService.tryAssignDriver(testOrder, driver);

            // Then
            assertThat(result).isFalse();
        }
    }

    private AvailableDriver createDriver(Long driverId, String name, double distanceKm) {
        return AvailableDriver.builder()
                .driverId(driverId)
                .driverName(name)
                .vehicleId(100L + driverId)
                .vehiclePlate("ABC-" + driverId)
                .latitude(new BigDecimal("-23.55"))
                .longitude(new BigDecimal("-46.64"))
                .distanceKm(distanceKm)
                .status("AVAILABLE")
                .build();
    }
}
