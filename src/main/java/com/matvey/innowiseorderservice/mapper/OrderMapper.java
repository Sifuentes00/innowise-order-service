package com.matvey.innowiseorderservice.mapper;

import com.matvey.innowiseorderservice.dto.OrderDto;
import com.matvey.innowiseorderservice.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

    OrderDto toDto(Order order);

    @Mapping(target = "orderItems", ignore = true)
    Order toEntity(OrderDto orderDto);

    @Mapping(target = "orderItems", ignore = true)
    void updateEntityFromDto(OrderDto orderDto, @MappingTarget Order order);

    default OrderDto toDtoWithItems(Order order, OrderItemMapper orderItemMapper) {
        OrderDto orderDto = toDto(order);
        if (order.getOrderItems() != null) {
            orderDto.setOrderItems(order.getOrderItems().stream()
                    .map(orderItemMapper::toDto)
                    .collect(Collectors.toList()));
        }
        return orderDto;
    }
}
