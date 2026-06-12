package com.matvey.innowiseorderservice.integration;

import com.matvey.innowiseorderservice.dto.ItemDto;
import com.matvey.innowiseorderservice.entity.Item;
import com.matvey.innowiseorderservice.service.ItemService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.matvey.innowiseorderservice.config.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ItemIntegrationTest {

    @Autowired
    private ItemService itemService;

    private ItemDto testItemDto;

    @BeforeEach
    void setUp() {
        testItemDto = new ItemDto();
        testItemDto.setName("Test Item");
        testItemDto.setPrice(new BigDecimal("50.00"));
    }

    @AfterEach
    void tearDown() {
        // Clean up is handled by H2 in-memory database
    }

    @Test
    void testCreateItem() {
        ItemDto result = itemService.create(testItemDto);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Test Item", result.getName());
        assertEquals(new BigDecimal("50.00"), result.getPrice());
    }

    @Test
    void testGetById() {
        ItemDto created = itemService.create(testItemDto);

        ItemDto result = itemService.getById(created.getId());

        assertNotNull(result);
        assertEquals(created.getId(), result.getId());
        assertEquals("Test Item", result.getName());
    }

    @Test
    void testGetById_NotFound() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> itemService.getById(nonExistentId));
    }

    @Test
    void testGetAll() {
        itemService.create(testItemDto);

        ItemDto item2 = new ItemDto();
        item2.setName("Second Item");
        item2.setPrice(new BigDecimal("75.00"));
        itemService.create(item2);

        List<ItemDto> result = itemService.getAll();

        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    void testUpdateItem() {
        ItemDto created = itemService.create(testItemDto);

        ItemDto updateDto = new ItemDto();
        updateDto.setName("Updated Item");
        updateDto.setPrice(new BigDecimal("100.00"));

        ItemDto result = itemService.update(created.getId(), updateDto);

        assertNotNull(result);
        assertEquals("Updated Item", result.getName());
        assertEquals(new BigDecimal("100.00"), result.getPrice());
    }

    @Test
    void testUpdate_NotFound() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> itemService.update(nonExistentId, testItemDto));
    }

    @Test
    void testDeleteItem() {
        ItemDto created = itemService.create(testItemDto);

        itemService.delete(created.getId());

        assertThrows(RuntimeException.class, () -> itemService.getById(created.getId()));
    }
}
