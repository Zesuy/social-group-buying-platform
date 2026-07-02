package com.example.groupshop.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding an item to the cart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCartItemRequest {

    @NotNull
    private Long groupBuyItemId;

    @NotNull
    @Min(1)
    private Integer quantity;

    /** Optional share token — required for hidden group buys */
    private String shareToken;
}
