package com.example.groupshop.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for order preview.
 */
@Data
public class OrderPreviewRequest {

    @NotNull
    private Long groupBuyId;

    @NotNull
    private Long addressId;

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
