package com.example.groupshop.cart.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for checkout preview from cart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartCheckoutPreviewRequest {

    @NotNull
    private Long addressId;

    @NotEmpty
    private List<Long> cartItemIds;

    /** Optional share token — required for hidden group buys when cart items don't have one */
    private String shareToken;
}
