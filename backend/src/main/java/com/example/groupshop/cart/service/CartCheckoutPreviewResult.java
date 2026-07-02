package com.example.groupshop.cart.service;

import com.example.groupshop.model.entity.Cart;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Internal result of cart checkout preview — carries the derived
 * groupBuyId, the resolved cart items, and the effective share token
 * (for hidden group buys).
 */
@Data
@AllArgsConstructor
public class CartCheckoutPreviewResult {
    private Long groupBuyId;
    private List<Cart> cartItems;
    private String shareToken;
}
