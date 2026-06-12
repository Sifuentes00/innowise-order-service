package com.matvey.innowiseorderservice.mapper;

import com.matvey.innowiseorderservice.dto.OrderItemDto;
import com.matvey.innowiseorderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = ItemMapper.class)
public interface OrderItemMapper {

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "item.id", target = "itemId")
    OrderItemDto toDto(OrderItem orderItem);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "item", ignore = true)
    OrderItem toEntity(OrderItemDto orderItemDto);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "item", ignore = true)
    void updateEntityFromDto(OrderItemDto orderItemDto, @MappingTarget OrderItem orderItem);
}
