package com.matvey.innowiseorderservice.dto;

import com.matvey.innowiseorderservice.enums.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderRequest implements Serializable {

    private OrderStatus status;

    @Min(value = 0, message = "Total price cannot be negative")
    private BigDecimal totalPrice;

    @Valid
    private List<UpdateOrderItemRequest> items;
}
