package com.example.groupshop.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a single cart item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long cartItemId;
    private Long groupBuyId;
    private Long groupBuyItemId;
    private Long productId;
    private String title;
    private String coverImageUrl;
    private Long groupPriceAmount;
    private Integer quantity;
    private Integer availableStock;
    private String visibility;
    private String status;
    private String startTime;
    private String endTime;
}
