package com.example.groupshop.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for creating an order.
 *
 * <p>Supports two modes:
 * <ul>
 *   <li><b>Direct mode:</b> {@code groupBuyId + items} — create order from selected items directly.</li>
 *   <li><b>Cart mode:</b> {@code cartItemIds} — create order from selected cart items.
 *       All cart items must belong to the same group buy.</li>
 * </ul>
 * The two modes are mutually exclusive.
 */
@Data
public class CreateOrderRequest {

    /** Optional: groupBuyId for direct purchase mode (required when not using cartItemIds) */
    private Long groupBuyId;

    /** Address ID — required for both modes */
    @NotNull
    private Long addressId;

    @Size(max = 256)
    private String remark;

    /** Optional share token — required for hidden group buys */
    private String shareToken;

    /** Direct purchase items (mutually exclusive with cartItemIds) */
    @Valid
    private List<OrderItemEntry> items;

    /** Cart item IDs for cart checkout (mutually exclusive with groupBuyId + items) */
    private List<Long> cartItemIds;

    /** Optional user coupon ID to use for this order */
    private Long userCouponId;

    @Data
    public static class OrderItemEntry implements com.example.groupshop.order.dto.OrderItemEntry {
        @NotNull
        private Long groupBuyItemId;

        @NotNull
        @Min(1)
        private Integer quantity;
    }
}
