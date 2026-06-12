package com.matvey.innowiseorderservice.mapper;

import com.matvey.innowiseorderservice.client.UserClient;
import com.matvey.innowiseorderservice.dto.OrderDto;
import com.matvey.innowiseorderservice.dto.OrderItemDto;
import com.matvey.innowiseorderservice.dto.OrderWithUserDto;
import com.matvey.innowiseorderservice.dto.UserDto;
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

    default OrderWithUserDto toOrderWithUserDto(Order order, OrderItemMapper orderItemMapper, UserClient userClient) {
        OrderWithUserDto orderWithUserDto = new OrderWithUserDto();
        orderWithUserDto.setId(order.getId());
        orderWithUserDto.setUserId(order.getUserId());
        orderWithUserDto.setEmail(order.getEmail());
        orderWithUserDto.setStatus(order.getStatus());
        orderWithUserDto.setTotalPrice(order.getTotalPrice());
        orderWithUserDto.setDeleted(order.getDeleted());
        orderWithUserDto.setCreatedAt(order.getCreatedAt());
        orderWithUserDto.setUpdatedAt(order.getUpdatedAt());

        if (order.getOrderItems() != null) {
            orderWithUserDto.setOrderItems(order.getOrderItems().stream()
                    .map(orderItemMapper::toDto)
                    .collect(Collectors.toList()));
        }

        UserDto userDto = userClient.getUserByEmail(order.getEmail());
        orderWithUserDto.setUser(userDto);

        return orderWithUserDto;
    }
}
