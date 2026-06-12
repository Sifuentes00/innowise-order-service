package com.matvey.innowiseorderservice.service;

import com.matvey.innowiseorderservice.dto.ItemDto;
import com.matvey.innowiseorderservice.entity.Item;
import com.matvey.innowiseorderservice.exception.ItemNotFoundException;
import com.matvey.innowiseorderservice.mapper.ItemMapper;
import com.matvey.innowiseorderservice.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public ItemDto create(ItemDto itemDto) {
        Item item = itemMapper.toEntity(itemDto);
        Item savedItem = itemRepository.save(item);
        return itemMapper.toDto(savedItem);
    }

    public ItemDto getById(UUID id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + id));
        return itemMapper.toDto(item);
    }

    public List<ItemDto> getAll() {
        List<Item> items = itemRepository.findAll();
        return items.stream()
                .map(itemMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ItemDto update(UUID id, ItemDto itemDto) {
        Item existingItem = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + id));
        itemMapper.updateEntityFromDto(itemDto, existingItem);
        Item updatedItem = itemRepository.save(existingItem);
        return itemMapper.toDto(updatedItem);
    }

    @Transactional
    public void delete(UUID id) {
        itemRepository.deleteById(id);
    }
}
