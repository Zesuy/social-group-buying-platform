package com.example.groupshop.cart.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.cart.dto.AddCartItemRequest;
import com.example.groupshop.cart.dto.CartCheckoutPreviewRequest;
import com.example.groupshop.cart.dto.CartItemResponse;
import com.example.groupshop.cart.dto.UpdateCartItemRequest;
import com.example.groupshop.cart.service.CartCheckoutPreviewResult;
import com.example.groupshop.cart.service.CartService;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.order.dto.OrderPreviewResponse;
import com.example.groupshop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cart controller — manage cart items and checkout preview.
 *
 * <p>All endpoints require login.
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;

    /**
     * List all cart items for the current user.
     */
    @GetMapping("/items")
    public ApiResponse<List<CartItemResponse>> listCartItems(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.success(cartService.getCartItems(userId));
    }

    /**
     * Add an item to the cart. If the item already exists, quantities are merged.
     */
    @PostMapping("/items")
    public ApiResponse<CartItemResponse> addCartItem(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success(cartService.addCartItem(userId, request));
    }

    /**
     * Update the quantity (and optionally share token) of a cart item.
     */
    @PatchMapping("/items/{cartItemId}")
    public ApiResponse<CartItemResponse> updateCartItem(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success(cartService.updateCartItem(userId, cartItemId, request));
    }

    /**
     * Delete a single cart item.
     */
    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<Void> deleteCartItem(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long cartItemId) {
        cartService.deleteCartItem(userId, cartItemId);
        return ApiResponse.success();
    }

    /**
     * Clear all cart items for the current user.
     */
    @DeleteMapping("/items")
    public ApiResponse<Void> clearCart(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        cartService.deleteAllCartItems(userId);
        return ApiResponse.success();
    }

    /**
     * Preview an order from selected cart items.
     * All items must belong to the same group buy.
     * Does NOT clear the cart.
     */
    @PostMapping("/checkout-preview")
    public ApiResponse<OrderPreviewResponse> checkoutPreview(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody CartCheckoutPreviewRequest request) {
        // Resolve cart items and derive order params
        CartCheckoutPreviewResult preview = cartService.checkoutPreview(userId, request);

        // Build order preview request from cart data and delegate to OrderService
        return ApiResponse.success(orderService.previewOrderFromCart(
                userId,
                preview.getGroupBuyId(),
                request.getAddressId(),
                preview.getCartItems(),
                preview.getShareToken()));
    }
}
