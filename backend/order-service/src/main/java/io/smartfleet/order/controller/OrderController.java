package io.smartfleet.order.controller;

import io.smartfleet.common.dto.ApiResponse;
import io.smartfleet.common.dto.OrderDto;
import io.smartfleet.order.entity.OrderStatus;
import io.smartfleet.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for order management.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderDto> createOrder(@Valid @RequestBody OrderDto orderDto) {
        OrderDto created = orderService.createOrder(orderDto);
        return ApiResponse.of(created);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDto> getOrder(@PathVariable Long orderId) {
        OrderDto order = orderService.getOrder(orderId);
        return ApiResponse.of(order);
    }

    @GetMapping
    public ApiResponse<List<OrderDto>> getOrdersByStatus(
            @RequestParam(required = false, defaultValue = "PENDING") String status) {
        List<OrderDto> orders = orderService.getOrdersByStatus(status);
        return ApiResponse.of(orders);
    }

    @PutMapping("/{orderId}/assign")
    public ApiResponse<OrderDto> assignDriver(
            @PathVariable Long orderId,
            @RequestParam Long driverId,
            @RequestParam Long vehicleId) {
        OrderDto updated = orderService.assignDriver(orderId, driverId, vehicleId);
        return ApiResponse.of(updated);
    }

    @PutMapping("/{orderId}/status")
    public ApiResponse<OrderDto> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {
        OrderDto updated = orderService.updateStatus(orderId, status);
        return ApiResponse.of(updated);
    }
}
