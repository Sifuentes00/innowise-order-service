package com.matvey.innowiseorderservice.service;

import com.matvey.innowiseorderservice.client.UserClient;
import com.matvey.innowiseorderservice.dto.CreateOrderItemRequest;
import com.matvey.innowiseorderservice.dto.CreateOrderRequest;
import com.matvey.innowiseorderservice.dto.OrderDto;
import com.matvey.innowiseorderservice.dto.OrderItemDto;
import com.matvey.innowiseorderservice.dto.OrderWithUserDto;
import com.matvey.innowiseorderservice.dto.UpdateOrderItemRequest;
import com.matvey.innowiseorderservice.dto.UpdateOrderRequest;
import com.matvey.innowiseorderservice.dto.UserDto;
import com.matvey.innowiseorderservice.entity.Item;
import com.matvey.innowiseorderservice.entity.Order;
import com.matvey.innowiseorderservice.entity.OrderItem;
import com.matvey.innowiseorderservice.enums.OrderStatus;
import com.matvey.innowiseorderservice.mapper.OrderItemMapper;
import com.matvey.innowiseorderservice.mapper.OrderMapper;
import com.matvey.innowiseorderservice.exception.ItemNotFoundException;
import com.matvey.innowiseorderservice.exception.OrderNotFoundException;
import com.matvey.innowiseorderservice.exception.UserNotActiveException;
import com.matvey.innowiseorderservice.exception.UserNotFoundException;
import com.matvey.innowiseorderservice.repository.ItemRepository;
import com.matvey.innowiseorderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
    private final UserClient userClient;
    private final ItemRepository itemRepository;

    @Transactional
    public OrderWithUserDto create(CreateOrderRequest createOrderRequest) {
        UserDto userDto = userClient.getUserByEmail(createOrderRequest.getEmail());
        if (userDto == null) {
            throw new RuntimeException("User not found with email: " + createOrderRequest.getEmail());
        }
        if (Boolean.FALSE.equals(userDto.getActive())) {
            throw new RuntimeException("User is not active: " + createOrderRequest.getEmail());
        }

        Order order = new Order();
        order.setUserId(userDto.getUserId());
        order.setEmail(createOrderRequest.getEmail());
        order.setStatus(OrderStatus.PENDING);
        order.setDeleted(false);

        if (createOrderRequest.getItems() != null) {
            for (CreateOrderItemRequest itemRequest : createOrderRequest.getItems()) {
                if (!itemRepository.existsById(itemRequest.getItemId())) {
                    throw new RuntimeException("Item not found with id: " + itemRequest.getItemId());
                }
                OrderItem orderItem = new OrderItem();
                Item item = new Item();
                item.setId(itemRequest.getItemId());
                orderItem.setItem(item);
                orderItem.setQuantity(itemRequest.getQuantity());
                orderItem.setOrder(order);
                order.getOrderItems().add(orderItem);
            }
        }

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toOrderWithUserDto(savedOrder, orderItemMapper, userClient);
    }

    public OrderWithUserDto getById(UUID id) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return orderMapper.toOrderWithUserDto(order, orderItemMapper, userClient);
    }

    public Page<OrderWithUserDto> getAll(UUID userId, List<OrderStatus> statuses, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Pageable pageable) {
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
        return orderPage.map(order -> orderMapper.toOrderWithUserDto(order, orderItemMapper, userClient));
    }

    public List<OrderWithUserDto> getByUserId(UUID userId) {
        List<Order> orders = orderRepository.findByUserIdAndDeletedFalse(userId);
        return orders.stream()
                .map(order -> orderMapper.toOrderWithUserDto(order, orderItemMapper, userClient))
                .toList();
    }

    @Transactional
    public OrderWithUserDto update(UUID id, UpdateOrderRequest updateOrderRequest) {
        Order existingOrder = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        if (updateOrderRequest.getStatus() != null) {
            existingOrder.setStatus(updateOrderRequest.getStatus());
        }
        if (updateOrderRequest.getTotalPrice() != null) {
            existingOrder.setTotalPrice(updateOrderRequest.getTotalPrice());
        }

        if (updateOrderRequest.getItems() != null) {
            existingOrder.getOrderItems().removeIf(orderItem -> {
                UUID itemId = orderItem.getItem().getId();
                return updateOrderRequest.getItems().stream()
                        .noneMatch(req -> req.getItemId().equals(itemId));
            });

            for (UpdateOrderItemRequest itemRequest : updateOrderRequest.getItems()) {
                if (!itemRepository.existsById(itemRequest.getItemId())) {
                    throw new RuntimeException("Item not found with id: " + itemRequest.getItemId());
                }

                OrderItem existingOrderItem = existingOrder.getOrderItems().stream()
                        .filter(oi -> oi.getItem().getId().equals(itemRequest.getItemId()))
                        .findFirst()
                        .orElse(null);

                if (existingOrderItem != null) {
                    existingOrderItem.setQuantity(itemRequest.getQuantity());
                } else {
                    OrderItem newOrderItem = new OrderItem();
                    Item item = new Item();
                    item.setId(itemRequest.getItemId());
                    newOrderItem.setItem(item);
                    newOrderItem.setQuantity(itemRequest.getQuantity());
                    newOrderItem.setOrder(existingOrder);
                    existingOrder.getOrderItems().add(newOrderItem);
                }
            }
        }

        Order updatedOrder = orderRepository.save(existingOrder);
        return orderMapper.toOrderWithUserDto(updatedOrder, orderItemMapper, userClient);
    }

    @Transactional
    public void softDelete(UUID id) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        order.setDeleted(true);
        orderRepository.save(order);
    }

    @Transactional
    public void delete(UUID id) {
        orderRepository.deleteById(id);
    }
}
