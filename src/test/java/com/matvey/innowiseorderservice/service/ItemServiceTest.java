package com.matvey.innowiseorderservice.service;

import com.matvey.innowiseorderservice.dto.ItemDto;
import com.matvey.innowiseorderservice.entity.Item;
import com.matvey.innowiseorderservice.exception.ItemNotFoundException;
import com.matvey.innowiseorderservice.mapper.ItemMapper;
import com.matvey.innowiseorderservice.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemService itemService;

    private UUID testItemId;
    private Item testItem;
    private ItemDto testItemDto;

    @BeforeEach
    void setUp() {
        testItemId = UUID.randomUUID();

        testItem = new Item();
        testItem.setId(testItemId);
        testItem.setName("Test Item");
        testItem.setPrice(new BigDecimal("50.00"));
        testItem.setCreatedAt(LocalDateTime.now());
        testItem.setUpdatedAt(LocalDateTime.now());

        testItemDto = new ItemDto();
        testItemDto.setId(testItemId);
        testItemDto.setName("Test Item");
        testItemDto.setPrice(new BigDecimal("50.00"));
        testItemDto.setCreatedAt(LocalDateTime.now());
        testItemDto.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void create_Success() {
        when(itemMapper.toEntity(testItemDto)).thenReturn(testItem);
        when(itemRepository.save(testItem)).thenReturn(testItem);
        when(itemMapper.toDto(testItem)).thenReturn(testItemDto);

        ItemDto result = itemService.create(testItemDto);

        assertNotNull(result);
        assertEquals(testItemDto.getName(), result.getName());
        verify(itemRepository, times(1)).save(testItem);
    }

    @Test
    void getById_Success() {
        when(itemRepository.findById(testItemId)).thenReturn(Optional.of(testItem));
        when(itemMapper.toDto(testItem)).thenReturn(testItemDto);

        ItemDto result = itemService.getById(testItemId);

        assertNotNull(result);
        assertEquals(testItemId, result.getId());
        verify(itemRepository, times(1)).findById(testItemId);
    }

    @Test
    void getById_NotFound() {
        when(itemRepository.findById(testItemId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> itemService.getById(testItemId));
        verify(itemRepository, times(1)).findById(testItemId);
    }

    @Test
    void getAll_Success() {
        when(itemRepository.findAll()).thenReturn(List.of(testItem));
        when(itemMapper.toDto(testItem)).thenReturn(testItemDto);

        List<ItemDto> result = itemService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(itemRepository, times(1)).findAll();
    }

    @Test
    void update_Success() {
        ItemDto updatedDto = new ItemDto();
        updatedDto.setName("Updated Item");
        updatedDto.setPrice(new BigDecimal("75.00"));

        when(itemRepository.findById(testItemId)).thenReturn(Optional.of(testItem));
        when(itemRepository.save(testItem)).thenReturn(testItem);
        when(itemMapper.toDto(testItem)).thenReturn(updatedDto);

        ItemDto result = itemService.update(testItemId, updatedDto);

        assertNotNull(result);
        verify(itemRepository, times(1)).findById(testItemId);
        verify(itemRepository, times(1)).save(testItem);
        verify(itemMapper, times(1)).updateEntityFromDto(updatedDto, testItem);
    }

    @Test
    void update_NotFound() {
        ItemDto updatedDto = new ItemDto();

        when(itemRepository.findById(testItemId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> itemService.update(testItemId, updatedDto));
        verify(itemRepository, times(1)).findById(testItemId);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void delete_Success() {
        itemService.delete(testItemId);

        verify(itemRepository, times(1)).deleteById(testItemId);
    }
}
