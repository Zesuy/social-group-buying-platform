package com.example.groupshop.cart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.groupshop.cart.dto.AddCartItemRequest;
import com.example.groupshop.cart.dto.CartCheckoutPreviewRequest;
import com.example.groupshop.cart.dto.CartItemResponse;
import com.example.groupshop.cart.dto.UpdateCartItemRequest;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.model.entity.Cart;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.entity.GroupBuyShareToken;
import com.example.groupshop.model.entity.Product;
import com.example.groupshop.model.mapper.CartMapper;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import com.example.groupshop.model.mapper.GroupBuyMapper;
import com.example.groupshop.model.mapper.GroupBuyShareTokenMapper;
import com.example.groupshop.model.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for cart management.
 *
 * <p>Handles adding, updating, deleting, listing cart items, and
 * checkout preview. Cart items are bound to a user and a group buy item.
 * For hidden group buys, a valid share token is required.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartMapper cartMapper;
    private final GroupBuyMapper groupBuyMapper;
    private final GroupBuyItemMapper groupBuyItemMapper;
    private final ProductMapper productMapper;
    private final GroupBuyShareTokenMapper groupBuyShareTokenMapper;

    // ── List Cart Items ─────────────────────────────────────────────────

    /**
     * List all cart items for the current user with full details.
     */
    public List<CartItemResponse> getCartItems(Long userId) {
        List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .orderByDesc(Cart::getCreatedAt));

        return carts.stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());
    }

    // ── Add Cart Item ───────────────────────────────────────────────────

    /**
     * Add an item to the cart. If the same {@code groupBuyItemId} already
     * exists for this user, quantities are merged and re-validated against stock.
     *
     * <p>For hidden group buys, a valid matching share token is required.
     */
    @Transactional
    public CartItemResponse addCartItem(Long userId, AddCartItemRequest request) {
        // Validate quantity is positive (defense-in-depth beyond @Min(1))
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "购买数量必须为正整数");
        }

        // Validate group buy item exists and has stock
        GroupBuyItem gbItem = groupBuyItemMapper.selectById(request.getGroupBuyItemId());
        if (gbItem == null) {
            throw new BusinessException(ErrorCode.ITEM_NOT_IN_GROUP_BUY, "团购商品不存在");
        }

        // Validate group buy is purchasable
        GroupBuy groupBuy = validateGroupBuyForCart(gbItem.getGroupBuyId(), request.getShareToken());

        // Validate stock
        if (gbItem.getGroupStock() < request.getQuantity()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "库存不足");
        }

        // Check if this item already exists in the user's cart
        Cart existing = cartMapper.selectOne(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .eq(Cart::getGroupBuyItemId, request.getGroupBuyItemId()));

        Long shareTokenId = resolveShareTokenId(groupBuy, request.getShareToken());

        if (existing != null) {
            // Merge quantities
            int mergedQuantity = existing.getQuantity() + request.getQuantity();
            if (gbItem.getGroupStock() < mergedQuantity) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "合并后库存不足");
            }
            existing.setQuantity(mergedQuantity);
            if (shareTokenId != null) {
                existing.setShareTokenId(shareTokenId);
            }
            cartMapper.updateById(existing);
            return toCartItemResponse(existing);
        }

        // Create new cart item
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setGroupBuyId(groupBuy.getId());
        cart.setGroupBuyItemId(request.getGroupBuyItemId());
        cart.setQuantity(request.getQuantity());
        cart.setShareTokenId(shareTokenId);
        cartMapper.insert(cart);

        return toCartItemResponse(cart);
    }

    // ── Update Cart Item ────────────────────────────────────────────────

    /**
     * Update the quantity (and optionally the share token) of a cart item.
     */
    @Transactional
    public CartItemResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = findCartForUser(cartItemId, userId);

        // Validate group buy item still exists and has stock
        GroupBuyItem gbItem = groupBuyItemMapper.selectById(cart.getGroupBuyItemId());
        if (gbItem == null) {
            throw new BusinessException(ErrorCode.ITEM_NOT_IN_GROUP_BUY, "团购商品不存在");
        }

        // Resolve share token — prefer request token, fall back to saved token
        GroupBuy groupBuy = groupBuyMapper.selectById(cart.getGroupBuyId());
        if (groupBuy == null) {
            throw new BusinessException(ErrorCode.GROUP_BUY_NOT_PURCHASABLE, "团购不存在");
        }

        String effectiveToken = request.getShareToken();
        if ((effectiveToken == null || effectiveToken.isBlank()) && cart.getShareTokenId() != null) {
            // Try to use the existing saved token
            GroupBuyShareToken savedToken = groupBuyShareTokenMapper.selectById(cart.getShareTokenId());
            if (savedToken != null && "active".equals(savedToken.getStatus())
                    && (savedToken.getExpiresAt() == null || savedToken.getExpiresAt().isAfter(LocalDateTime.now()))) {
                effectiveToken = savedToken.getToken();
            }
        }

        // Re-validate group buy
        validateGroupBuyForCart(groupBuy.getId(), effectiveToken);

        // Validate new quantity against stock
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "购买数量必须为正整数");
        }
        if (gbItem.getGroupStock() < request.getQuantity()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "库存不足");
        }

        cart.setQuantity(request.getQuantity());

        // Update share token if provided
        if (request.getShareToken() != null && !request.getShareToken().isBlank()) {
            Long tokenId = resolveShareTokenId(groupBuy, request.getShareToken());
            if (tokenId != null) {
                cart.setShareTokenId(tokenId);
            }
        }

        cartMapper.updateById(cart);
        return toCartItemResponse(cart);
    }

    // ── Delete Cart Item ────────────────────────────────────────────────

    /**
     * Delete a single cart item.
     */
    @Transactional
    public void deleteCartItem(Long userId, Long cartItemId) {
        Cart cart = findCartForUser(cartItemId, userId);
        cartMapper.deleteById(cart.getId());
    }

    // ── Clear Cart ──────────────────────────────────────────────────────

    /**
     * Clear all cart items for the current user.
     */
    @Transactional
    public void deleteAllCartItems(Long userId) {
        cartMapper.delete(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId));
    }

    // ── Checkout Preview ───────────────────────────────────────────────

    /**
     * Preview an order from selected cart items. Requires all items to
     * belong to the same group buy.
     *
     * <p>Does NOT clear the cart items. Returns order preview-like data.
     */
    public CartCheckoutPreviewResult checkoutPreview(Long userId, CartCheckoutPreviewRequest request) {
        // Load and validate cart items
        List<Cart> carts = cartMapper.selectBatchIds(request.getCartItemIds());
        if (carts.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "购物车商品不存在");
        }

        // Verify every requested ID was found (no silent ignoring of missing IDs)
        java.util.Set<Long> requested = new java.util.HashSet<>(request.getCartItemIds());
        java.util.Set<Long> found = carts.stream()
                .map(Cart::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (!requested.equals(found)) {
            throw new BusinessException(ErrorCode.CART_NOT_FOUND, "部分购物车商品不存在");
        }

        // Verify all belong to the current user
        for (Cart cart : carts) {
            if (!cart.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.CART_FORBIDDEN, "不能操作他人购物车");
            }
        }

        // Verify all belong to the same group buy
        Long firstGroupBuyId = carts.get(0).getGroupBuyId();
        for (Cart cart : carts) {
            if (!cart.getGroupBuyId().equals(firstGroupBuyId)) {
                throw new BusinessException(ErrorCode.CART_CROSS_GROUP_BUY, "购物车结算必须属于同一团购");
            }
        }

        // Validate group buy is still purchasable, reuse saved share tokens
        GroupBuy groupBuy = groupBuyMapper.selectById(firstGroupBuyId);
        if (groupBuy == null) {
            throw new BusinessException(ErrorCode.GROUP_BUY_NOT_PURCHASABLE, "团购不存在");
        }

        // Check hidden group buy access
        String effectiveShareToken = resolveShareTokenForGroupBuy(groupBuy, carts, request.getShareToken());

        return new CartCheckoutPreviewResult(firstGroupBuyId, carts, effectiveShareToken);
    }

    // ── Load cart items for order creation ──────────────────────────────

    /**
     * Load and validate cart items for order creation.
     * Returns the cart items, the derived groupBuyId, and resolved share token.
     */
    public CartCheckoutPreviewResult loadCartItemsForOrder(Long userId, List<Long> cartItemIds, String requestShareToken) {
        return checkoutPreview(userId, CartCheckoutPreviewRequest.builder()
                .cartItemIds(cartItemIds)
                .shareToken(requestShareToken)
                .build());
    }

    /**
     * Delete cart items by their IDs (used after successful order creation).
     */
    @Transactional
    public void deleteCartItemsByIds(List<Long> cartItemIds) {
        cartMapper.deleteBatchIds(cartItemIds);
    }

    // ── Internal helpers ───────────────────────────────────────────────

    /**
     * Validate that a group buy is active for cart operations.
     * For hidden group buys, a valid share token is required.
     */
    private GroupBuy validateGroupBuyForCart(Long groupBuyId, String shareToken) {
        GroupBuy groupBuy = groupBuyMapper.selectById(groupBuyId);
        if (groupBuy == null) {
            throw new BusinessException(ErrorCode.GROUP_BUY_NOT_PURCHASABLE, "团购不存在");
        }
        if (!"published".equals(groupBuy.getStatus())) {
            throw new BusinessException(ErrorCode.GROUP_BUY_NOT_PURCHASABLE, "团购不可购买");
        }

        // Handle hidden group buys
        if ("hidden".equals(groupBuy.getVisibility())) {
            if (shareToken == null || shareToken.isBlank()) {
                throw new BusinessException(ErrorCode.HIDDEN_GROUP_BUY_REQUIRES_TOKEN, "隐藏团购需要有效分享 token");
            }
            validateShareToken(shareToken, groupBuyId);
        }

        if (groupBuy.getEndTime() != null && groupBuy.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.GROUP_BUY_ENDED, "团购已结束");
        }
        if (groupBuy.getStartTime() != null && groupBuy.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.GROUP_BUY_NOT_PURCHASABLE, "团购尚未开始");
        }
        return groupBuy;
    }

    /**
     * Validate a share token matches a group buy and is active/not expired.
     */
    private GroupBuyShareToken validateShareToken(String tokenValue, Long groupBuyId) {
        GroupBuyShareToken token = groupBuyShareTokenMapper.selectOne(
                new LambdaQueryWrapper<GroupBuyShareToken>()
                        .eq(GroupBuyShareToken::getToken, tokenValue)
                        .eq(GroupBuyShareToken::getGroupBuyId, groupBuyId)
                        .eq(GroupBuyShareToken::getStatus, "active"));
        if (token == null) {
            throw new BusinessException(ErrorCode.SHARE_TOKEN_INVALID, "分享 token 无效或与团购不匹配");
        }
        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.SHARE_TOKEN_INVALID, "分享 token 已过期");
        }
        return token;
    }

    /**
     * Resolve the share token ID for a hidden group buy. Returns null for public group buys.
     */
    private Long resolveShareTokenId(GroupBuy groupBuy, String shareToken) {
        if (!"hidden".equals(groupBuy.getVisibility()) || shareToken == null || shareToken.isBlank()) {
            return null;
        }
        GroupBuyShareToken token = validateShareToken(shareToken, groupBuy.getId());
        return token.getId();
    }

    /**
     * Resolve an effective share token for a hidden group buy from multiple sources:
     * 1. Request share token (highest priority)
     * 2. Saved share token on any cart item
     */
    private String resolveShareTokenForGroupBuy(GroupBuy groupBuy, List<Cart> carts, String requestShareToken) {
        if (!"hidden".equals(groupBuy.getVisibility())) {
            return null;
        }
        if (requestShareToken != null && !requestShareToken.isBlank()) {
            validateShareToken(requestShareToken, groupBuy.getId());
            return requestShareToken;
        }
        // Fall back to any saved share token on cart items
        for (Cart cart : carts) {
            if (cart.getShareTokenId() != null) {
                GroupBuyShareToken saved = groupBuyShareTokenMapper.selectById(cart.getShareTokenId());
                if (saved != null && "active".equals(saved.getStatus())
                        && (saved.getExpiresAt() == null || saved.getExpiresAt().isAfter(LocalDateTime.now()))) {
                    return saved.getToken();
                }
            }
        }
        throw new BusinessException(ErrorCode.HIDDEN_GROUP_BUY_REQUIRES_TOKEN, "隐藏团购需要有效分享 token");
    }

    /**
     * Find a cart item and verify it belongs to the user.
     */
    private Cart findCartForUser(Long cartItemId, Long userId) {
        Cart cart = cartMapper.selectById(cartItemId);
        if (cart == null) {
            throw new BusinessException(ErrorCode.CART_NOT_FOUND);
        }
        if (!cart.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CART_FORBIDDEN);
        }
        return cart;
    }

    /**
     * Convert a Cart entity to a CartItemResponse with product/group buy details.
     */
    private CartItemResponse toCartItemResponse(Cart cart) {
        GroupBuyItem gbItem = groupBuyItemMapper.selectById(cart.getGroupBuyItemId());
        if (gbItem == null) {
            return CartItemResponse.builder()
                    .cartItemId(cart.getId())
                    .groupBuyId(cart.getGroupBuyId())
                    .groupBuyItemId(cart.getGroupBuyItemId())
                    .quantity(cart.getQuantity())
                    .build();
        }

        Product product = productMapper.selectById(gbItem.getProductId());
        GroupBuy groupBuy = groupBuyMapper.selectById(cart.getGroupBuyId());

        return CartItemResponse.builder()
                .cartItemId(cart.getId())
                .groupBuyId(cart.getGroupBuyId())
                .groupBuyItemId(cart.getGroupBuyItemId())
                .productId(gbItem.getProductId())
                .title(gbItem.getDisplayName())
                .coverImageUrl(product != null ? product.getCoverImageUrl() : null)
                .groupPriceAmount(gbItem.getGroupPriceAmount())
                .quantity(cart.getQuantity())
                .availableStock(gbItem.getGroupStock())
                .visibility(groupBuy != null ? groupBuy.getVisibility() : null)
                .status(groupBuy != null ? groupBuy.getStatus() : null)
                .startTime(groupBuy != null && groupBuy.getStartTime() != null
                        ? groupBuy.getStartTime().toString() : null)
                .endTime(groupBuy != null && groupBuy.getEndTime() != null
                        ? groupBuy.getEndTime().toString() : null)
                .build();
    }
}
