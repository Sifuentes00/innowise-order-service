package com.matvey.innowiseorderservice.service;

import com.matvey.innowiseorderservice.client.UserClient;
import com.matvey.innowiseorderservice.dto.CreateOrderItemRequest;
import com.matvey.innowiseorderservice.dto.CreateOrderRequest;
import com.matvey.innowiseorderservice.dto.OrderWithUserDto;
import com.matvey.innowiseorderservice.dto.UpdateOrderItemRequest;
import com.matvey.innowiseorderservice.dto.UpdateOrderRequest;
import com.matvey.innowiseorderservice.dto.UserDto;
import com.matvey.innowiseorderservice.entity.Item;
import com.matvey.innowiseorderservice.entity.Order;
import com.matvey.innowiseorderservice.entity.OrderItem;
import com.matvey.innowiseorderservice.enums.OrderStatus;
import com.matvey.innowiseorderservice.exception.ItemNotFoundException;
import com.matvey.innowiseorderservice.exception.OrderNotFoundException;
import com.matvey.innowiseorderservice.exception.UserNotActiveException;
import com.matvey.innowiseorderservice.exception.UserNotFoundException;
import com.matvey.innowiseorderservice.mapper.OrderItemMapper;
import com.matvey.innowiseorderservice.mapper.OrderMapper;
import com.matvey.innowiseorderservice.repository.ItemRepository;
import com.matvey.innowiseorderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private UserClient userClient;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private OrderService orderService;

    private UUID testUserId;
    private UUID testOrderId;
    private UUID testItemId;
    private UserDto testUserDto;
    private CreateOrderRequest createOrderRequest;
    private UpdateOrderRequest updateOrderRequest;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testOrderId = UUID.randomUUID();
        testItemId = UUID.randomUUID();

        testUserDto = new UserDto();
        testUserDto.setId(UUID.randomUUID());
        testUserDto.setUserId(testUserId);
        testUserDto.setName("Test");
        testUserDto.setSurname("User");
        testUserDto.setEmail("test@example.com");
        testUserDto.setActive(true);
        testUserDto.setCreatedAt(LocalDateTime.now());
        testUserDto.setUpdatedAt(LocalDateTime.now());

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setItemId(testItemId);
        itemRequest.setQuantity(2);

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setEmail("test@example.com");
        createOrderRequest.setItems(List.of(itemRequest));

        UpdateOrderItemRequest updateItemRequest = new UpdateOrderItemRequest();
        updateItemRequest.setItemId(testItemId);
        updateItemRequest.setQuantity(3);

        updateOrderRequest = new UpdateOrderRequest();
        updateOrderRequest.setStatus(OrderStatus.PAID);
        updateOrderRequest.setTotalPrice(new BigDecimal("100.00"));
        updateOrderRequest.setItems(List.of(updateItemRequest));
    }

    @Test
    void create_Success() {
        Item testItem = new Item();
        testItem.setId(testItemId);

        when(userClient.getUserByEmail("test@example.com")).thenReturn(testUserDto);
        when(itemRepository.findById(testItemId)).thenReturn(java.util.Optional.of(testItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(testOrderId);
            return order;
        });
        when(orderMapper.toOrderWithUserDto(any(Order.class), any(OrderItemMapper.class), any(UserClient.class)))
                .thenReturn(new OrderWithUserDto());

        OrderWithUserDto result = orderService.create(createOrderRequest);

        assertNotNull(result);
        verify(userClient, times(1)).getUserByEmail("test@example.com");
        verify(itemRepository, times(1)).findById(testItemId);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void create_UserNotFound() {
        when(userClient.getUserByEmail("test@example.com")).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> orderService.create(createOrderRequest));
        verify(userClient, times(1)).getUserByEmail("test@example.com");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void create_UserNotActive() {
        testUserDto.setActive(false);
        when(userClient.getUserByEmail("test@example.com")).thenReturn(testUserDto);

        assertThrows(UserNotActiveException.class, () -> orderService.create(createOrderRequest));
        verify(userClient, times(1)).getUserByEmail("test@example.com");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void create_ItemNotFound() {
        when(userClient.getUserByEmail("test@example.com")).thenReturn(testUserDto);
        when(itemRepository.findById(testItemId)).thenReturn(java.util.Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> orderService.create(createOrderRequest));
        verify(itemRepository, times(1)).findById(testItemId);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getById_Success() {
        Order order = new Order();
        order.setId(testOrderId);
        order.setUserId(testUserId);
        order.setEmail("test@example.com");

        when(orderRepository.findByIdWithItemsAndDeletedFalse(testOrderId)).thenReturn(Optional.of(order));
        when(orderMapper.toOrderWithUserDto(any(Order.class), any(OrderItemMapper.class), any(UserClient.class)))
                .thenReturn(new OrderWithUserDto());

        OrderWithUserDto result = orderService.getById(testOrderId);

        assertNotNull(result);
        verify(orderRepository, times(1)).findByIdWithItemsAndDeletedFalse(testOrderId);
    }

    @Test
    void getById_NotFound() {
        when(orderRepository.findByIdWithItemsAndDeletedFalse(testOrderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getById(testOrderId));
        verify(orderRepository, times(1)).findByIdWithItemsAndDeletedFalse(testOrderId);
    }

    @Test
    void getAll_Success() {
        Order order = new Order();
        order.setId(testOrderId);

        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(orderRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderMapper.toOrderWithUserDto(any(Order.class), any(OrderItemMapper.class), any(UserClient.class)))
                .thenReturn(new OrderWithUserDto());

        Page<OrderWithUserDto> result = orderService.getAll(null, null, null, null, Pageable.unpaged());

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(orderRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
    }

    @Test
    void getByUserId_Success() {
        Order order = new Order();
        order.setId(testOrderId);
        order.setUserId(testUserId);

        when(orderRepository.findByUserIdAndDeletedFalse(testUserId)).thenReturn(List.of(order));
        when(orderMapper.toOrderWithUserDto(any(Order.class), any(OrderItemMapper.class), any(UserClient.class)))
                .thenReturn(new OrderWithUserDto());

        List<OrderWithUserDto> result = orderService.getByUserId(testUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository, times(1)).findByUserIdAndDeletedFalse(testUserId);
    }

    @Test
    void update_Success() {
        Order existingOrder = new Order();
        existingOrder.setId(testOrderId);
        existingOrder.setUserId(testUserId);

        OrderItem existingOrderItem = new OrderItem();
        Item item = new Item();
        item.setId(testItemId);
        existingOrderItem.setItem(item);
        existingOrderItem.setQuantity(2);
        existingOrder.setOrderItems(new ArrayList<>(List.of(existingOrderItem)));

        when(orderRepository.findByIdWithItemsAndDeletedFalse(testOrderId)).thenReturn(Optional.of(existingOrder));
        when(itemRepository.findById(testItemId)).thenReturn(java.util.Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(existingOrder);
        when(orderMapper.toOrderWithUserDto(any(Order.class), any(OrderItemMapper.class), any(UserClient.class)))
                .thenReturn(new OrderWithUserDto());

        OrderWithUserDto result = orderService.update(testOrderId, updateOrderRequest);

        assertNotNull(result);
        verify(orderRepository, times(1)).findByIdWithItemsAndDeletedFalse(testOrderId);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void update_OrderNotFound() {
        when(orderRepository.findByIdWithItemsAndDeletedFalse(testOrderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.update(testOrderId, updateOrderRequest));
        verify(orderRepository, times(1)).findByIdWithItemsAndDeletedFalse(testOrderId);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void update_ItemNotFound() {
        Order existingOrder = new Order();
        existingOrder.setId(testOrderId);

        UpdateOrderItemRequest itemRequest = new UpdateOrderItemRequest();
        itemRequest.setItemId(testItemId);
        itemRequest.setQuantity(3);
        updateOrderRequest.setItems(List.of(itemRequest));

        when(orderRepository.findByIdWithItemsAndDeletedFalse(testOrderId)).thenReturn(Optional.of(existingOrder));
        when(itemRepository.findById(testItemId)).thenReturn(java.util.Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> orderService.update(testOrderId, updateOrderRequest));
        verify(itemRepository, times(1)).findById(testItemId);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void softDelete_Success() {
        Order order = new Order();
        order.setId(testOrderId);

        when(orderRepository.findByIdWithItemsAndDeletedFalse(testOrderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.softDelete(testOrderId);

        assertTrue(order.getDeleted());
        verify(orderRepository, times(1)).findByIdWithItemsAndDeletedFalse(testOrderId);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void softDelete_NotFound() {
        when(orderRepository.findByIdWithItemsAndDeletedFalse(testOrderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.softDelete(testOrderId));
        verify(orderRepository, times(1)).findByIdWithItemsAndDeletedFalse(testOrderId);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void delete_Success() {
        orderService.delete(testOrderId);

        verify(orderRepository, times(1)).deleteById(testOrderId);
    }
}
