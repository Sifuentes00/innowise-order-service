package com.matvey.innowiseorderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
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
import com.matvey.innowiseorderservice.mapper.OrderItemMapper;
import com.matvey.innowiseorderservice.mapper.OrderMapper;
import com.matvey.innowiseorderservice.repository.ItemRepository;
import com.matvey.innowiseorderservice.repository.OrderRepository;
import com.matvey.innowiseorderservice.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.matvey.innowiseorderservice.config.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    private UUID testUserId;
    private UUID testItemId;
    private UserDto testUserDto;
    private Item testItem;
    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
        wireMockServer = new WireMockServer(8081);
        wireMockServer.start();

        testUserId = UUID.randomUUID();

        testUserDto = new UserDto();
        testUserDto.setId(UUID.randomUUID());
        testUserDto.setUserId(testUserId);
        testUserDto.setName("Test");
        testUserDto.setSurname("User");
        testUserDto.setEmail("test@example.com");
        testUserDto.setActive(true);
        testUserDto.setCreatedAt(LocalDateTime.now());
        testUserDto.setUpdatedAt(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testCreateOrder_WithWireMockUser() {
        Item item = new Item();
        item.setName("Test Item");
        item.setPrice(new BigDecimal("50.00"));
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        Item savedItem = itemRepository.save(item);

        wireMockServer.stubFor(get(urlEqualTo("/internal/users/email/test@example.com"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(asJsonString(testUserDto))));

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setItemId(savedItem.getId());
        itemRequest.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setEmail("test@example.com");
        request.setItems(List.of(itemRequest));
        request.setTotalPrice(new BigDecimal("100.00"));

        OrderWithUserDto result = orderService.create(request);

        assertNotNull(result);
        verifyWireMockCall();
    }

    @Test
    void testFullOrderCreationFlow() {
        Item item = new Item();
        item.setName("Test Item");
        item.setPrice(new BigDecimal("50.00"));
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        Item savedItem = itemRepository.save(item);

        wireMockServer.stubFor(get(urlEqualTo("/internal/users/email/test@example.com"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(asJsonString(testUserDto))));

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setItemId(savedItem.getId());
        itemRequest.setQuantity(3);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setEmail("test@example.com");
        request.setItems(List.of(itemRequest));
        request.setTotalPrice(new BigDecimal("100.00"));

        OrderWithUserDto createdOrder = orderService.create(request);
        assertNotNull(createdOrder);

        UUID orderId = orderRepository.findAll().get(0).getId();
        Order savedOrder = orderRepository.findByIdWithItems(orderId).orElse(null);
        assertNotNull(savedOrder);
        assertEquals(testUserId, savedOrder.getUserId());
        assertEquals("test@example.com", savedOrder.getEmail());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
        assertFalse(savedOrder.getDeleted());
        assertEquals(1, savedOrder.getOrderItems().size());

        OrderItem savedOrderItem = savedOrder.getOrderItems().get(0);
        assertEquals(savedItem.getId(), savedOrderItem.getItem().getId());
        assertEquals(3, savedOrderItem.getQuantity());

        verifyWireMockCall();
    }

    @Test
    void testUpdateOrder() {
        Item item = new Item();
        item.setName("Test Item");
        item.setPrice(new BigDecimal("50.00"));
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        Item savedItem = itemRepository.save(item);

        wireMockServer.stubFor(get(urlEqualTo("/internal/users/email/test@example.com"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(asJsonString(testUserDto))));

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setItemId(savedItem.getId());
        itemRequest.setQuantity(2);

        CreateOrderRequest createRequest = new CreateOrderRequest();
        createRequest.setEmail("test@example.com");
        createRequest.setItems(List.of(itemRequest));
        createRequest.setTotalPrice(new BigDecimal("100.00"));

        orderService.create(createRequest);

        Order savedOrder = orderRepository.findAll().get(0);
        UUID orderId = savedOrder.getId();

        UpdateOrderItemRequest updateItemRequest = new UpdateOrderItemRequest();
        updateItemRequest.setItemId(savedItem.getId());
        updateItemRequest.setQuantity(5);

        UpdateOrderRequest updateRequest = new UpdateOrderRequest();
        updateRequest.setStatus(OrderStatus.PAID);
        updateRequest.setTotalPrice(new BigDecimal("250.00"));
        updateRequest.setItems(List.of(updateItemRequest));

        OrderWithUserDto updatedOrder = orderService.update(orderId, updateRequest);
        assertNotNull(updatedOrder);

        Order updatedOrderEntity = orderRepository.findByIdWithItems(orderId).orElse(null);
        assertNotNull(updatedOrderEntity);
        assertEquals(OrderStatus.PAID, updatedOrderEntity.getStatus());
        assertEquals(new BigDecimal("250.00"), updatedOrderEntity.getTotalPrice());
        assertEquals(5, updatedOrderEntity.getOrderItems().get(0).getQuantity());
    }

    @Test
    void testGetById() {
        Item item = new Item();
        item.setName("Test Item");
        item.setPrice(new BigDecimal("50.00"));
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        Item savedItem = itemRepository.save(item);

        wireMockServer.stubFor(get(urlEqualTo("/internal/users/email/test@example.com"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(asJsonString(testUserDto))));

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setItemId(savedItem.getId());
        itemRequest.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setEmail("test@example.com");
        request.setItems(List.of(itemRequest));
        request.setTotalPrice(new BigDecimal("100.00"));

        orderService.create(request);

        UUID orderId = orderRepository.findAll().get(0).getId();
        Order savedOrder = orderRepository.findByIdWithItems(orderId).orElse(null);
        OrderWithUserDto result = orderService.getById(savedOrder.getId());

        assertNotNull(result);
    }

    @Test
    void testSoftDelete() {
        Item item = new Item();
        item.setName("Test Item");
        item.setPrice(new BigDecimal("50.00"));
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        Item savedItem = itemRepository.save(item);

        wireMockServer.stubFor(get(urlEqualTo("/internal/users/email/test@example.com"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(asJsonString(testUserDto))));

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setItemId(savedItem.getId());
        itemRequest.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setEmail("test@example.com");
        request.setItems(List.of(itemRequest));
        request.setTotalPrice(new BigDecimal("100.00"));

        orderService.create(request);

        UUID orderId = orderRepository.findAll().get(0).getId();
        Order savedOrder = orderRepository.findByIdWithItems(orderId).orElse(null);
        assertFalse(savedOrder.getDeleted());

        orderService.softDelete(savedOrder.getId());

        Order deletedOrder = orderRepository.findById(savedOrder.getId()).orElse(null);
        assertNotNull(deletedOrder);
        assertTrue(deletedOrder.getDeleted());
    }

    private String asJsonString(Object obj) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyWireMockCall() {
        wireMockServer.verify(getRequestedFor(urlEqualTo("/internal/users/email/test@example.com")));
    }

}
