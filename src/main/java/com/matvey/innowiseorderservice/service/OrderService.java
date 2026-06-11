package com.matvey.innowiseorderservice.service;

import com.matvey.innowiseorderservice.dto.OrderDto;
import com.matvey.innowiseorderservice.dto.OrderItemDto;
import com.matvey.innowiseorderservice.entity.Order;
import com.matvey.innowiseorderservice.entity.OrderItem;
import com.matvey.innowiseorderservice.enums.OrderStatus;
import com.matvey.innowiseorderservice.mapper.OrderItemMapper;
import com.matvey.innowiseorderservice.mapper.OrderMapper;
import com.matvey.innowiseorderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.matvey.innowiseorderservice.specification.OrderSpecification.byCreatedAtBetween;
import static com.matvey.innowiseorderservice.specification.OrderSpecification.byDeletedFalse;
import static com.matvey.innowiseorderservice.specification.OrderSpecification.byStatuses;
import static com.matvey.innowiseorderservice.specification.OrderSpecification.byUserId;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public OrderDto create(OrderDto orderDto) {
        Order order = orderMapper.toEntity(orderDto);
        order.setDeleted(false);
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.PENDING);
        }

        if (order.getOrderItems() != null) {
            for (OrderItem orderItem : order.getOrderItems()) {
                orderItem.setOrder(order);
            }
        }

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toDtoWithItems(savedOrder, orderItemMapper);
    }

    public OrderDto getById(UUID id) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return orderMapper.toDtoWithItems(order, orderItemMapper);
    }

    public Page<OrderDto> getAll(UUID userId, List<OrderStatus> statuses, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Pageable pageable) {
        Specification<Order> spec = Specification.where(byDeletedFalse());

        if (userId != null) {
            spec = spec.and(byUserId(userId));
        }
        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and(byStatuses(statuses));
        }
        if (startDate != null || endDate != null) {
            spec = spec.and(byCreatedAtBetween(startDate, endDate));
        }

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        return orderPage.map(order -> orderMapper.toDtoWithItems(order, orderItemMapper));
    }

    public List<OrderDto> getByUserId(UUID userId) {
        List<Order> orders = orderRepository.findByUserIdAndDeletedFalse(userId);
        return orders.stream()
                .map(order -> orderMapper.toDtoWithItems(order, orderItemMapper))
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDto update(UUID id, OrderDto orderDto) {
        Order existingOrder = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        orderMapper.updateEntityFromDto(orderDto, existingOrder);

        if (orderDto.getOrderItems() != null) {
            existingOrder.getOrderItems().clear();
            for (OrderItemDto orderItemDto : orderDto.getOrderItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(existingOrder);
                existingOrder.getOrderItems().add(orderItem);
            }
        }

        Order updatedOrder = orderRepository.save(existingOrder);
        return orderMapper.toDtoWithItems(updatedOrder, orderItemMapper);
    }

    @Transactional
    public void softDelete(UUID id) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        order.setDeleted(true);
        orderRepository.save(order);
    }

    @Transactional
    public void delete(UUID id) {
        orderRepository.deleteById(id);
    }
}
