package com.matvey.innowiseorderservice.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matvey.innowiseorderservice.dto.CreateOrderItemRequest;
import com.matvey.innowiseorderservice.dto.CreateOrderRequest;
import com.matvey.innowiseorderservice.dto.UserDto;
import com.matvey.innowiseorderservice.entity.Item;
import com.matvey.innowiseorderservice.entity.Order;
import com.matvey.innowiseorderservice.entity.OrderItem;
import com.matvey.innowiseorderservice.enums.OrderStatus;
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
class OrderCreationIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
        wireMockServer = new WireMockServer(8081);
        wireMockServer.start();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testFullOrderCreationFlow() {
        UUID userId = UUID.randomUUID();

        UserDto userDto = new UserDto();
        userDto.setId(UUID.randomUUID());
        userDto.setUserId(userId);
        userDto.setName("Test");
        userDto.setSurname("User");
        userDto.setEmail("test@example.com");
        userDto.setActive(true);
        userDto.setCreatedAt(LocalDateTime.now());
        userDto.setUpdatedAt(LocalDateTime.now());

        wireMockServer.stubFor(get(urlEqualTo("/internal/users/email/test@example.com"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(asJsonString(userDto))));

        Item item = new Item();
        item.setName("Test Item");
        item.setPrice(new BigDecimal("50.00"));
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        Item savedItem = itemRepository.save(item);

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setItemId(savedItem.getId());
        itemRequest.setQuantity(3);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setEmail("test@example.com");
        request.setItems(List.of(itemRequest));
        request.setTotalPrice(new BigDecimal("150.00"));

        orderService.create(request);

        UUID orderId = orderRepository.findAll().get(0).getId();
        Order savedOrder = orderRepository.findByIdWithItems(orderId).orElse(null);
        assertNotNull(savedOrder);
        assertEquals(userId, savedOrder.getUserId());
        assertEquals("test@example.com", savedOrder.getEmail());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
        assertFalse(savedOrder.getDeleted());
        assertEquals(1, savedOrder.getOrderItems().size());

        OrderItem savedOrderItem = savedOrder.getOrderItems().get(0);
        assertEquals(savedItem.getId(), savedOrderItem.getItem().getId());
        assertEquals(3, savedOrderItem.getQuantity());

        wireMockServer.verify(getRequestedFor(urlEqualTo("/internal/users/email/test@example.com")));
    }

    @Test
    void testOrderCreationWithMultipleItems() {
        UUID userId = UUID.randomUUID();

        UserDto userDto = new UserDto();
        userDto.setId(UUID.randomUUID());
        userDto.setUserId(userId);
        userDto.setName("Test");
        userDto.setSurname("User");
        userDto.setEmail("test@example.com");
        userDto.setActive(true);
        userDto.setCreatedAt(LocalDateTime.now());
        userDto.setUpdatedAt(LocalDateTime.now());

        wireMockServer.stubFor(get(urlEqualTo("/internal/users/email/test@example.com"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(asJsonString(userDto))));

        Item item1 = new Item();
        item1.setName("Item 1");
        item1.setPrice(new BigDecimal("50.00"));
        item1.setCreatedAt(LocalDateTime.now());
        item1.setUpdatedAt(LocalDateTime.now());
        Item savedItem1 = itemRepository.save(item1);

        Item item2 = new Item();
        item2.setName("Item 2");
        item2.setPrice(new BigDecimal("75.00"));
        item2.setCreatedAt(LocalDateTime.now());
        item2.setUpdatedAt(LocalDateTime.now());
        Item savedItem2 = itemRepository.save(item2);

        CreateOrderItemRequest itemRequest1 = new CreateOrderItemRequest();
        itemRequest1.setItemId(savedItem1.getId());
        itemRequest1.setQuantity(2);

        CreateOrderItemRequest itemRequest2 = new CreateOrderItemRequest();
        itemRequest2.setItemId(savedItem2.getId());
        itemRequest2.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setEmail("test@example.com");
        request.setItems(List.of(itemRequest1, itemRequest2));
        request.setTotalPrice(new BigDecimal("175.00"));

        orderService.create(request);

        UUID orderId = orderRepository.findAll().get(0).getId();
        Order savedOrder = orderRepository.findByIdWithItems(orderId).orElse(null);
        assertEquals(2, savedOrder.getOrderItems().size());
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
}
