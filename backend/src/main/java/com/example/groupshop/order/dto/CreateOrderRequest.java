package com.example.groupshop.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for creating an order.
 */
@Data
public class CreateOrderRequest {

    @NotNull
    private Long groupBuyId;

    @NotNull
    private Long addressId;

    @Size(max = 256)
    private String remark;

    /** Optional share token — required for hidden group buys */
    private String shareToken;

    @NotEmpty
    @Valid
    private List<OrderItemEntry> items;

    @Data
    public static class OrderItemEntry implements com.example.groupshop.order.dto.OrderItemEntry {
        @NotNull
        private Long groupBuyItemId;

        @NotNull
        private Integer quantity;
    }
}
