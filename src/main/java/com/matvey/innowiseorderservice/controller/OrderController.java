package com.matvey.innowiseorderservice.controller;

import com.matvey.innowiseorderservice.dto.CreateOrderRequest;
import com.matvey.innowiseorderservice.dto.OrderWithUserDto;
import com.matvey.innowiseorderservice.dto.UpdateOrderRequest;
import com.matvey.innowiseorderservice.enums.OrderStatus;
import com.matvey.innowiseorderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderWithUserDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderWithUserDto response = orderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderWithUserDto> getOrderById(@PathVariable UUID id) {
        OrderWithUserDto response = orderService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderWithUserDto>> getAllOrders(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String statuses,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            Pageable pageable) {
        List<OrderStatus> statusList = null;
        if (statuses != null && !statuses.isBlank()) {
            statusList = List.of(statuses.split(","))
                    .stream()
                    .map(String::trim)
                    .map(OrderStatus::valueOf)
                    .toList();
        }
        Page<OrderWithUserDto> response = orderService.getAll(userId, statusList, startDate, endDate, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderWithUserDto>> getOrdersByUserId(@PathVariable UUID userId) {
        List<OrderWithUserDto> response = orderService.getByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderWithUserDto> updateOrder(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderRequest request) {
        OrderWithUserDto response = orderService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteOrder(@PathVariable UUID id) {
        orderService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteOrder(@PathVariable UUID id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
