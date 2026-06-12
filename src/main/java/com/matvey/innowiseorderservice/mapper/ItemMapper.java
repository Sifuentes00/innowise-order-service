package com.matvey.innowiseorderservice.mapper;

import com.matvey.innowiseorderservice.dto.ItemDto;
import com.matvey.innowiseorderservice.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    ItemDto toDto(Item item);

    Item toEntity(ItemDto itemDto);

    void updateEntityFromDto(ItemDto itemDto, @MappingTarget Item item);
}
