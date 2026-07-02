package com.example.groupshop.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for order preview.
 *
 * <p>Supports two modes:
 * <ul>
 *   <li><b>Direct mode:</b> {@code groupBuyId + items} — preview from selected items directly.</li>
 *   <li><b>Cart mode:</b> {@code cartItemIds} — preview from selected cart items.
 *       All cart items must belong to the same group buy.</li>
 * </ul>
 * The two modes are mutually exclusive.
 */
@Data
public class OrderPreviewRequest {

    /** Optional: groupBuyId for direct purchase mode (required when not using cartItemIds) */
    private Long groupBuyId;

    /** Address ID — required for both modes */
    @NotNull
    private Long addressId;

    /** Optional share token — required for hidden group buys */
    private String shareToken;

    /** Direct purchase items (mutually exclusive with cartItemIds) */
    @Valid
    private List<OrderItemEntry> items;

    /** Cart item IDs for cart checkout (mutually exclusive with groupBuyId + items) */
    private List<Long> cartItemIds;

    @Data
    public static class OrderItemEntry implements com.example.groupshop.order.dto.OrderItemEntry {
        @NotNull
        private Long groupBuyItemId;

        @NotNull
        @Min(1)
        private Integer quantity;
    }
}
