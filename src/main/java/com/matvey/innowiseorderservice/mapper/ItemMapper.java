package com.matvey.innowiseorderservice.mapper;

import com.matvey.innowiseorderservice.dto.ItemDto;
import com.matvey.innowiseorderservice.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    ItemDto toDto(Item item);

    Item toEntity(ItemDto itemDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(ItemDto itemDto, @MappingTarget Item item);
}
