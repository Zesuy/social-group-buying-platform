package com.example.groupshop.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.groupshop.address.service.AddressService;
import com.example.groupshop.cart.service.CartCheckoutPreviewResult;
import com.example.groupshop.cart.service.CartService;
import com.example.groupshop.chat.service.ChatService;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.coupon.dto.AvailableCouponResponse;
import com.example.groupshop.coupon.service.CouponService;
import com.example.groupshop.memberlevel.service.MemberLevelRuleService;
import com.example.groupshop.model.entity.Address;
import com.example.groupshop.model.entity.AfterSale;
import com.example.groupshop.model.entity.Cart;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.entity.GroupBuyShareToken;
import com.example.groupshop.model.entity.MemberRelation;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.model.entity.OrderItem;
import com.example.groupshop.model.entity.Product;
import com.example.groupshop.model.entity.UserCoupon;
import com.example.groupshop.model.mapper.AfterSaleMapper;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import com.example.groupshop.model.mapper.GroupBuyMapper;
import com.example.groupshop.model.mapper.GroupBuyShareTokenMapper;
import com.example.groupshop.model.mapper.MemberRelationMapper;
import com.example.groupshop.model.mapper.OrderItemMapper;
import com.example.groupshop.model.mapper.OrderMapper;
import com.example.groupshop.model.mapper.ProductMapper;
import com.example.groupshop.notification.service.NotificationService;
import com.example.groupshop.order.dto.CreateOrderRequest;
import com.example.groupshop.order.dto.OrderItemEntry;
import com.example.groupshop.order.dto.OrderPreviewRequest;
import com.example.groupshop.order.dto.OrderPreviewResponse;
import com.example.groupshop.order.dto.OrderPreviewResponse.AddressPreview;
import com.example.groupshop.order.dto.OrderPreviewResponse.ItemPreview;
import com.example.groupshop.order.dto.OrderResponse;
import com.example.groupshop.order.dto.OrderResponse.OrderItemData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for order preview, creation, and querying.
 *
 * <p>Supports two purchase modes:
 * <ul>
 *   <li><b>Direct mode:</b> buyer picks items and quantities directly.</li>
 *   <li><b>Cart mode:</b> buyer selects from pre-added cart items.
 *       All cart items must belong to the same group buy.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GroupBuyMapper groupBuyMapper;
    private final GroupBuyItemMapper groupBuyItemMapper;
    private final ProductMapper productMapper;
    private final MemberRelationMapper memberRelationMapper;
    private final AddressService addressService;
    private final GroupBuyShareTokenMapper groupBuyShareTokenMapper;
    private final CartService cartService;
    private final CouponService couponService;
    private final MemberLevelRuleService memberLevelRuleService;
    private final AfterSaleMapper afterSaleMapper;
    private final NotificationService notificationService;
    private final ChatService chatService;

    private static final String DB_STATUS_PENDING_PAY = "pending_pay";
    private static final String API_STATUS_PENDING_PAY = "pendingPay";

    // ── Order Preview (public) ──────────────────────────────────────────

    /**
     * Preview an order, supporting both direct and cart modes.
     *
     * <p>In cart mode ({@code cartItemIds} set), loads the cart items,
     * validates they belong to the same group buy, and derives the
     * groupBuyId and item entries from the cart.
     *
     * <p>In direct mode ({@code groupBuyId + items}), validates the
     * group buy and items directly.
     *
     * <p>The two modes are mutually exclusive — passing both or neither
     * results in {@code VALIDATION_ERROR}.
     */
    public OrderPreviewResponse previewOrder(Long userId, OrderPreviewRequest request) {
        // Validate that either cart mode or direct mode is used
        validateOrderMode(request.getCartItemIds(), request.getGroupBuyId(), request.getItems());

        if (request.getCartItemIds() != null && !request.getCartItemIds().isEmpty()) {
            // ── Cart mode ──────────────────────────────────────────────────
            CartCheckoutPreviewResult preview = cartService.loadCartItemsForOrder(
                    userId, request.getCartItemIds(), request.getShareToken());
            return previewOrderFromCart(userId, preview.getGroupBuyId(),
                    request.getAddressId(), preview.getCartItems(), preview.getShareToken(),
                    request.getUserCouponId());
        }

        // ── Direct mode ──────────────────────────────────────────────────
        return doPreviewOrder(userId, request.getGroupBuyId(), request.getAddressId(),
                request.getShareToken(), request.getItems(), request.getUserCouponId());
    }

    /**
     * Preview an order from cart items (used by CartController.checkoutPreview).
     * All cart items must belong to the same group buy.
     */
    public OrderPreviewResponse previewOrderFromCart(Long userId, Long groupBuyId,
                                                       Long addressId,
                                                       List<Cart> cartItems,
                                                       String shareToken) {
        return previewOrderFromCart(userId, groupBuyId, addressId, cartItems, shareToken, null);
    }

    /**
     * Preview an order from cart items with optional coupon.
     */
    public OrderPreviewResponse previewOrderFromCart(Long userId, Long groupBuyId,
                                                       Long addressId,
                                                       List<Cart> cartItems,
                                                       String shareToken,
                                                       Long userCouponId) {
        // Create transient OrderItemEntry list from cart items
        List<OrderItemEntry> entries = cartItems.stream()
                .map(cart -> new CartDerivedOrderItemEntry(cart.getGroupBuyItemId(), cart.getQuantity()))
                .collect(Collectors.toList());

        return doPreviewOrder(userId, groupBuyId, addressId, shareToken, entries, userCouponId);
    }

    // ── Create Order ───────────────────────────────────────────────────

    /**
     * Create an order, supporting both direct and cart modes.
     *
     * <p>In cart mode, after successful creation, the processed cart items
     * are deleted from the cart.
     */
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        // Validate that either cart mode or direct mode is used
        validateOrderMode(request.getCartItemIds(), request.getGroupBuyId(), request.getItems());

        List<Long> cartItemIdsForCleanup = null;

        if (request.getCartItemIds() != null && !request.getCartItemIds().isEmpty()) {
            // ── Cart mode: load from cart, derive groupBuyId/items ────────
            CartCheckoutPreviewResult result = cartService.loadCartItemsForOrder(
                    userId, request.getCartItemIds(), request.getShareToken());

            // Create transient item entries from cart
            List<OrderItemEntry> entries = result.getCartItems().stream()
                    .map(cart -> new CartDerivedOrderItemEntry(
                            cart.getGroupBuyItemId(), cart.getQuantity()))
                    .collect(Collectors.toList());

            cartItemIdsForCleanup = request.getCartItemIds();

            return doCreateOrder(userId, result.getGroupBuyId(), request.getAddressId(),
                    result.getShareToken(), request.getRemark(), entries, cartItemIdsForCleanup,
                    request.getUserCouponId());
        }

        // ── Direct mode ──────────────────────────────────────────────────
        return doCreateOrder(userId, request.getGroupBuyId(), request.getAddressId(),
                request.getShareToken(), request.getRemark(), request.getItems(), null,
                request.getUserCouponId());
    }

    // ── Internal order preview (shared by both modes) ───────────────────

    /**
     * Core preview logic shared by direct and cart modes.
     */
    private OrderPreviewResponse doPreviewOrder(Long userId, Long groupBuyId,
                                                  Long addressId,
                                                  String shareToken,
                                                  List<? extends OrderItemEntry> entries,
                                                  Long userCouponId) {
        // Validate group buy
        GroupBuy groupBuy = validateGroupBuyForPurchase(groupBuyId, shareToken);

        // Validate items
        List<GroupBuyItem> gbItems = validateAndLoadItems(groupBuyId, entries);

        // Validate address
        Address address = addressService.findAddressForUser(addressId, userId);

        // Calculate amounts
        long totalAmount = 0;
        List<ItemPreview> itemPreviews = new ArrayList<>();
        Map<Long, GroupBuyItem> itemMap = gbItems.stream()
                .collect(Collectors.toMap(GroupBuyItem::getId, i -> i));

        for (OrderItemEntry entry : entries) {
            GroupBuyItem gbItem = itemMap.get(entry.getGroupBuyItemId());
            long unitPrice = gbItem.getGroupPriceAmount();
            int quantity = entry.getQuantity();
            long itemTotal = unitPrice * quantity;
            totalAmount += itemTotal;

            Product product = productMapper.selectById(gbItem.getProductId());

            itemPreviews.add(ItemPreview.builder()
                    .groupBuyItemId(gbItem.getId())
                    .productId(gbItem.getProductId())
                    .productName(gbItem.getDisplayName())
                    .skuName("")
                    .unitPriceAmount(unitPrice)
                    .quantity(quantity)
                    .totalAmount(itemTotal)
                    .availableStock(gbItem.getGroupStock())
                    .soldCount(gbItem.getSoldCount())
                    .build());
        }

        OrderPreviewResponse.OrderPreviewResponseBuilder builder = OrderPreviewResponse.builder()
                .groupBuyId(groupBuyId)
                .address(toAddressPreview(address))
                .items(itemPreviews)
                .totalAmount(totalAmount);

        // ── Coupon handling ──────────────────────────────────────────────
        if (userCouponId != null) {
            // Validate and use the specified coupon
            UserCoupon userCoupon = couponService.validateUserCouponForOrder(userId, userCouponId, groupBuyId, totalAmount);
            long discountAmount = calculateDiscount(userCoupon, totalAmount);
            long payAmount = totalAmount - discountAmount;

            builder.discountAmount(discountAmount)
                    .payAmount(payAmount)
                    .selectedCoupon(toAvailableCouponResponseFromUserCoupon(userCoupon));
        } else {
            // No coupon specified — show available/unavailable list
            builder.discountAmount(0L)
                    .payAmount(totalAmount);

            // Get coupon availability info
            CouponService.CouponCheckResult checkResult = couponService.getCouponsForOrderPreview(userId, groupBuyId, totalAmount);
            if (!checkResult.getAvailableCoupons().isEmpty()) {
                builder.availableCoupons(checkResult.getAvailableCoupons());
            }
            if (!checkResult.getUnavailableCoupons().isEmpty()) {
                builder.unavailableCoupons(checkResult.getUnavailableCoupons());
            }
        }

        return builder.build();
    }

    // ── Internal order creation (shared by both modes) ─────────────────

    /**
     * Core create-order logic shared by direct and cart modes.
     * Optionally cleans up cart items after successful creation.
     */
    private OrderResponse doCreateOrder(Long userId, Long groupBuyId,
                                         Long addressId, String shareToken,
                                         String remark,
                                         List<? extends OrderItemEntry> entries,
                                         List<Long> cartItemIdsToDelete,
                                         Long userCouponId) {
        // Validate group buy
        GroupBuy groupBuy = validateGroupBuyForPurchase(groupBuyId, shareToken);

        // Validate items
        List<GroupBuyItem> gbItems = validateAndLoadItems(groupBuyId, entries);

        // Validate address and get snapshot
        Address address = addressService.findAddressForUser(addressId, userId);

        // Build order number
        String orderNo = generateOrderNo();

        // Calculate amounts
        long totalAmount = 0;
        List<OrderItemData> itemDataList = new ArrayList<>();
        Map<Long, GroupBuyItem> itemMap = gbItems.stream()
                .collect(Collectors.toMap(GroupBuyItem::getId, i -> i));

        for (OrderItemEntry entry : entries) {
            GroupBuyItem gbItem = itemMap.get(entry.getGroupBuyItemId());
            long unitPrice = gbItem.getGroupPriceAmount();
            int quantity = entry.getQuantity();
            long itemTotal = unitPrice * quantity;
            totalAmount += itemTotal;

            itemDataList.add(OrderItemData.builder()
                    .groupBuyItemId(gbItem.getId())
                    .productId(gbItem.getProductId())
                    .productName(gbItem.getDisplayName())
                    .skuName("")
                    .unitPriceAmount(unitPrice)
                    .quantity(quantity)
                    .totalAmount(itemTotal)
                    .build());
        }

        // Handle coupon: validate and lock
        long discountAmount = 0;
        Long resolvedUserCouponId = null;
        Long resolvedCouponId = null;
        String resolvedCouponName = null;
        String resolvedCouponType = null;

        if (userCouponId != null) {
            UserCoupon userCoupon = couponService.validateUserCouponForOrder(userId, userCouponId, groupBuyId, totalAmount);
            discountAmount = calculateDiscount(userCoupon, totalAmount);
            resolvedUserCouponId = userCoupon.getId();
            resolvedCouponId = userCoupon.getCouponId();
            resolvedCouponName = userCoupon.getCouponName();
            resolvedCouponType = userCoupon.getCouponType();
        }

        long payAmount = totalAmount - discountAmount;
        if (payAmount < 0) {
            payAmount = 0L;
        }

        // Create order
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setLeaderId(groupBuy.getLeaderId());
        order.setStoreId(groupBuy.getStoreId());
        order.setGroupBuyId(groupBuyId);
        order.setAddressId(address.getId());

        // Address snapshot
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setProvince(address.getProvince());
        order.setCity(address.getCity());
        order.setDistrict(address.getDistrict());
        order.setDetail(address.getDetail());
        order.setFullAddress(address.getProvince() + address.getCity()
                + address.getDistrict() + address.getDetail());

        // Amounts
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setPayAmount(payAmount);

        // Coupon snapshot
        order.setUserCouponId(resolvedUserCouponId);
        order.setCouponId(resolvedCouponId);
        order.setCouponName(resolvedCouponName);
        order.setCouponType(resolvedCouponType);

        // Status — DB uses snake_case
        order.setPayStatus("unpaid");
        order.setOrderStatus(DB_STATUS_PENDING_PAY);
        order.setRemark(remark);

        orderMapper.insert(order);

        // Lock coupon if used
        if (resolvedUserCouponId != null) {
            couponService.lockUserCoupon(resolvedUserCouponId, order.getId());
        }

        // Create order items with snapshots
        int idx = 0;
        for (OrderItemEntry entry : entries) {
            GroupBuyItem gbItem = itemMap.get(entry.getGroupBuyItemId());
            Product product = productMapper.selectById(gbItem.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(gbItem.getProductId());
            orderItem.setGroupBuyItemId(gbItem.getId());
            orderItem.setProductName(gbItem.getDisplayName());
            orderItem.setSkuName("");
            orderItem.setUnitPriceAmount(gbItem.getGroupPriceAmount());
            orderItem.setQuantity(entry.getQuantity());
            orderItem.setTotalAmount(gbItem.getGroupPriceAmount() * entry.getQuantity());
            orderItemMapper.insert(orderItem);

            itemDataList.get(idx).setId(orderItem.getId());
            idx++;
        }

        // Clean up cart items if this was a cart checkout
        if (cartItemIdsToDelete != null && !cartItemIdsToDelete.isEmpty()) {
            cartService.deleteCartItemsByIds(cartItemIdsToDelete);
        }

        chatService.recordOrderCreated(order);
        return toOrderResponse(order, itemDataList);
    }

    // ── My Orders ──────────────────────────────────────────────────────

    /**
     * List current user's orders with optional status filter.
     */
    public PageResponse<OrderResponse> getMyOrders(Long userId, String status, int page, int pageSize) {
        Page<Order> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt);

        if (status != null && !status.isEmpty()) {
            String dbStatus = toDbOrderStatus(status);
            wrapper.eq(Order::getOrderStatus, dbStatus);
        }

        Page<Order> result = orderMapper.selectPage(pageObj, wrapper);

        List<OrderResponse> items = result.getRecords().stream()
                .map(this::toOrderResponseWithItems)
                .collect(Collectors.toList());

        return PageResponse.of(items, page, pageSize, result.getTotal());
    }

    /**
     * Get a single order detail. Only the order owner can view it.
     */
    public OrderResponse getMyOrder(Long userId, Long orderId) {
        Order order = findOrderForUser(orderId, userId);
        return toOrderResponseWithItems(order);
    }

    public Order requirePayableOrderForUser(Long userId, Long orderId, boolean validateStock) {
        Order order = findOrderForUser(orderId, userId);
        ensurePayable(order);
        if (validateStock) {
            validateStockAvailable(order);
        }
        return order;
    }

    // ── Cancel Order ───────────────────────────────────────────────────

    /**
     * Cancel an order. Only allowed when orderStatus=pendingPay and payStatus=unpaid.
     * Releases locked user coupon if one was used.
     */
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = findOrderForUser(orderId, userId);

        if (!DB_STATUS_PENDING_PAY.equals(order.getOrderStatus()) || !"unpaid".equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELABLE, "当前订单状态不可取消");
        }

        order.setOrderStatus("canceled");
        orderMapper.updateById(order);

        // Release locked coupon if any
        if (order.getUserCouponId() != null) {
            couponService.releaseUserCoupon(order.getId());
        }

        return toOrderResponseWithItems(order);
    }

    // ── Simulate Pay (Batch 07) ───────────────────────────────────────

    /**
     * Simulate payment for an order.
     * Handles coupon write-off and member growth accumulation.
     */
    @Transactional
    public OrderResponse simulatePay(Long userId, Long orderId) {
        Order order = findOrderForUser(orderId, userId);
        return completePaidOrder(order, false);
    }

    @Transactional
    public OrderResponse completeZeroAmountPayment(Long userId, Long orderId) {
        Order order = findOrderForUser(orderId, userId);
        if (order.getPayAmount() > 0) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PAYABLE, "订单应付金额不为 0");
        }
        return completePaidOrder(order, false);
    }

    @Transactional
    public OrderResponse completePaidOrderFromCallback(String orderNo) {
        Order order = findOrderByOrderNo(orderNo);
        return completePaidOrder(order, true);
    }

    public Order findOrderByOrderNo(String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return order;
    }

    private OrderResponse completePaidOrder(Order order, boolean allowAlreadyPaid) {
        if ("paid".equals(order.getPayStatus())) {
            if (allowAlreadyPaid) {
                return toOrderResponseWithItems(order);
            }
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID, "订单已支付");
        }
        ensurePayable(order);

        deductStock(order);

        // Update order to paid — atomic condition update
        LocalDateTime now = LocalDateTime.now();
        int updated = orderMapper.update(null,
                new LambdaUpdateWrapper<Order>()
                        .eq(Order::getId, order.getId())
                        .eq(Order::getPayStatus, "unpaid")
                        .eq(Order::getOrderStatus, DB_STATUS_PENDING_PAY)
                        .set(Order::getPayStatus, "paid")
                        .set(Order::getOrderStatus, "paid")
                        .set(Order::getPaidAt, now));
        if (updated == 0) {
            Order current = orderMapper.selectById(order.getId());
            if (current != null && "paid".equals(current.getPayStatus()) && allowAlreadyPaid) {
                return toOrderResponseWithItems(current);
            }
            if (current != null && "paid".equals(current.getPayStatus())) {
                throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID, "订单已支付");
            }
            throw new BusinessException(ErrorCode.ORDER_NOT_PAYABLE, "订单当前状态不可支付");
        }
        order.setPayStatus("paid");
        order.setOrderStatus("paid");
        order.setPaidAt(now);

        // Mark coupon as used if any
        if (order.getUserCouponId() != null) {
            couponService.useUserCoupon(order.getId());
        }

        // Create or update member relation with payAmount
        upsertMemberRelation(order, now);
        notificationService.notifyOrderPaid(order, order.getUserId());
        chatService.recordOrderPaid(order);

        return toOrderResponseWithItems(order);
    }

    private void ensurePayable(Order order) {
        if ("paid".equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID, "订单已支付");
        }
        if (!DB_STATUS_PENDING_PAY.equals(order.getOrderStatus()) || !"unpaid".equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PAYABLE, "订单当前状态不可支付");
        }
    }

    private void validateStockAvailable(Order order) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId()));

        for (OrderItem orderItem : orderItems) {
            GroupBuyItem gbItem = groupBuyItemMapper.selectById(orderItem.getGroupBuyItemId());
            if (gbItem == null) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "团购商品不存在");
            }
            if (gbItem.getGroupStock() < orderItem.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "库存不足");
            }
        }
    }

    private void deductStock(Order order) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId()));

        // Validate and deduct stock atomically
        for (OrderItem orderItem : orderItems) {
            GroupBuyItem gbItem = groupBuyItemMapper.selectById(orderItem.getGroupBuyItemId());
            if (gbItem == null) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "团购商品不存在");
            }
            if (gbItem.getGroupStock() < orderItem.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "库存不足");
            }
            // Atomic deduction: groupStock -= quantity, soldCount += quantity
            int updated = groupBuyItemMapper.update(null,
                    new LambdaUpdateWrapper<GroupBuyItem>()
                            .eq(GroupBuyItem::getId, gbItem.getId())
                            .eq(GroupBuyItem::getGroupStock, gbItem.getGroupStock())
                            .setSql("group_stock = group_stock - " + orderItem.getQuantity()
                                    + ", sold_count = sold_count + " + orderItem.getQuantity()));
            if (updated == 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "库存不足，并发扣减失败");
            }
        }
    }

    /**
     * Atomically create or update member relation via DB UPSERT,
     * then recalculate level name based on current rules.
     */
    private void upsertMemberRelation(Order order, LocalDateTime now) {
        memberRelationMapper.upsert(
                order.getUserId(),
                order.getLeaderId(),
                order.getStoreId(),
                order.getPayAmount(),
                now);

        // Read back the member relation to get current growth value
        MemberRelation relation = memberRelationMapper.selectOne(
                new LambdaQueryWrapper<MemberRelation>()
                        .eq(MemberRelation::getUserId, order.getUserId())
                        .eq(MemberRelation::getStoreId, order.getStoreId()));

        if (relation != null) {
            // Recalculate level name based on current rules
            memberLevelRuleService.recalculateMemberLevel(
                    order.getStoreId(), relation.getId(), relation.getGrowthValue());
        }
    }

    // ── Complete Order (Batch 10) ────────────────────────────────────

    /**
     * Complete (confirm receipt for) an order.
     */
    @Transactional
    public OrderResponse completeOrder(Long userId, Long orderId) {
        Order order = findOrderForUser(orderId, userId);

        LocalDateTime now = LocalDateTime.now();
        int updated = orderMapper.update(null,
                new LambdaUpdateWrapper<Order>()
                        .eq(Order::getId, order.getId())
                        .eq(Order::getOrderStatus, "shipped")
                        .set(Order::getOrderStatus, "completed")
                        .set(Order::getCompletedAt, now));
        if (updated == 0) {
            Order current = orderMapper.selectById(order.getId());
            if (current == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            if ("completed".equals(current.getOrderStatus())) {
                throw new BusinessException(ErrorCode.ORDER_ALREADY_COMPLETED, "订单已完成");
            }
            throw new BusinessException(ErrorCode.ORDER_NOT_COMPLETABLE, "当前订单状态不可确认收货");
        }
        order.setOrderStatus("completed");
        order.setCompletedAt(now);
        notificationService.notifyOrderCompleted(order, userId);
        chatService.recordOrderCompleted(order);

        return toOrderResponseWithItems(order);
    }

    // ── Validation ───────────────────────────────────────────────────

    /**
     * Validate that the order request uses exactly one mode:
     * either {@code cartItemIds} (cart mode) or {@code groupBuyId + items} (direct mode).
     */
    private void validateOrderMode(List<Long> cartItemIds, Long groupBuyId, List<?> items) {
        boolean hasCartMode = cartItemIds != null && !cartItemIds.isEmpty();
        boolean hasDirectMode = groupBuyId != null && items != null && !items.isEmpty();

        if (hasCartMode && hasDirectMode) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "items 和 cartItemIds 不能同时使用");
        }
        if (!hasCartMode && !hasDirectMode) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "必须选择一种下单方式：items（直接购买）或 cartItemIds（购物车结算）");
        }
    }

    /**
     * Validate that a group buy is purchasable: published, started, not ended.
     * For hidden group buys, a valid matching share token is required.
     */
    private GroupBuy validateGroupBuyForPurchase(Long groupBuyId, String shareToken) {
        GroupBuy groupBuy = groupBuyMapper.selectById(groupBuyId);
        if (groupBuy == null) {
            throw new BusinessException(ErrorCode.GROUP_BUY_NOT_PURCHASABLE, "团购不存在");
        }
        if (!"published".equals(groupBuy.getStatus())) {
            throw new BusinessException(ErrorCode.GROUP_BUY_NOT_PURCHASABLE, "团购不可购买");
        }

        if ("hidden".equals(groupBuy.getVisibility())) {
            if (shareToken == null || shareToken.isBlank()) {
                throw new BusinessException(ErrorCode.HIDDEN_GROUP_BUY_REQUIRES_TOKEN, "隐藏团购需要有效分享 token");
            }
            GroupBuyShareToken token = groupBuyShareTokenMapper.selectOne(
                    new LambdaQueryWrapper<GroupBuyShareToken>()
                            .eq(GroupBuyShareToken::getToken, shareToken)
                            .eq(GroupBuyShareToken::getGroupBuyId, groupBuyId)
                            .eq(GroupBuyShareToken::getStatus, "active"));
            if (token == null) {
                throw new BusinessException(ErrorCode.SHARE_TOKEN_INVALID, "分享 token 无效或与团购不匹配");
            }
            if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.SHARE_TOKEN_INVALID, "分享 token 已过期");
            }
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
     * Validate that items belong to the group buy and have sufficient stock.
     */
    private List<GroupBuyItem> validateAndLoadItems(Long groupBuyId, List<? extends OrderItemEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "下单商品不能为空");
        }

        List<GroupBuyItem> gbItems = groupBuyItemMapper.selectList(
                new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, groupBuyId));

        if (gbItems.isEmpty()) {
            throw new BusinessException(ErrorCode.ITEM_NOT_IN_GROUP_BUY, "团购中没有可购买的商品");
        }

        Map<Long, GroupBuyItem> itemMap = gbItems.stream()
                .collect(Collectors.toMap(GroupBuyItem::getId, i -> i));

        Map<Long, Boolean> seen = new HashMap<>();
        for (OrderItemEntry entry : entries) {
            if (entry.getQuantity() == null || entry.getQuantity() <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "购买数量必须为正整数");
            }
            if (seen.containsKey(entry.getGroupBuyItemId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "存在重复的团购商品");
            }
            seen.put(entry.getGroupBuyItemId(), true);

            GroupBuyItem gbItem = itemMap.get(entry.getGroupBuyItemId());
            if (gbItem == null) {
                throw new BusinessException(ErrorCode.ITEM_NOT_IN_GROUP_BUY, "商品不属于该团购");
            }
            if (gbItem.getGroupStock() < entry.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "库存不足");
            }
        }

        return gbItems;
    }

    // ── Coupon helpers ────────────────────────────────────────────────

    /**
     * Calculate discount amount from a user coupon.
     * Discount cannot exceed total amount.
     */
    private long calculateDiscount(UserCoupon userCoupon, long totalAmount) {
        long discount = userCoupon.getAmount();
        return Math.min(discount, totalAmount);
    }

    /**
     * Convert a UserCoupon to an AvailableCouponResponse for preview display.
     */
    private AvailableCouponResponse toAvailableCouponResponseFromUserCoupon(UserCoupon userCoupon) {
        return AvailableCouponResponse.builder()
                .id(userCoupon.getCouponId())
                .userCouponId(userCoupon.getId())
                .name(userCoupon.getCouponName())
                .couponType(userCoupon.getCouponType())
                .amount(userCoupon.getAmount())
                .thresholdAmount(userCoupon.getThresholdAmount())
                .unavailableReason(null)
                .build();
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private Order findOrderForUser(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return order;
    }

    private String generateOrderNo() {
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr();
    }

    private String toDbOrderStatus(String apiStatus) {
        if (API_STATUS_PENDING_PAY.equals(apiStatus)) {
            return DB_STATUS_PENDING_PAY;
        }
        return apiStatus;
    }

    private AddressPreview toAddressPreview(Address address) {
        return AddressPreview.builder()
                .id(address.getId())
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .province(address.getProvince())
                .city(address.getCity())
                .district(address.getDistrict())
                .detail(address.getDetail())
                .fullAddress(address.getProvince() + address.getCity()
                        + address.getDistrict() + address.getDetail())
                .build();
    }

    private OrderResponse toOrderResponse(Order order, List<OrderItemData> items) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .groupBuyId(order.getGroupBuyId())
                .storeId(order.getStoreId())
                .leaderId(order.getLeaderId())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .payAmount(order.getPayAmount())
                .payStatus(order.getPayStatus())
                .orderStatus(toApiOrderStatus(order.getOrderStatus()))
                .paidAt(order.getPaidAt() != null ? order.getPaidAt().toString() : null)
                .shippedAt(order.getShippedAt() != null ? order.getShippedAt().toString() : null)
                .completedAt(order.getCompletedAt() != null ? order.getCompletedAt().toString() : null)
                .remark(order.getRemark())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .province(order.getProvince())
                .city(order.getCity())
                .district(order.getDistrict())
                .detail(order.getDetail())
                .fullAddress(order.getFullAddress())
                .items(items)
                .userCouponId(order.getUserCouponId())
                .couponId(order.getCouponId())
                .couponName(order.getCouponName())
                .couponType(order.getCouponType())
                .afterSale(loadAfterSaleSummary(order.getId()))
                .build();
    }

    /**
     * Load the after-sale summary for an order, if one exists.
     * Returns the most recent pending/approved/completed after-sale, or null.
     */
    private OrderResponse.AfterSaleSummary loadAfterSaleSummary(Long orderId) {
        AfterSale afterSale = afterSaleMapper.selectOne(
                new LambdaQueryWrapper<AfterSale>()
                        .eq(AfterSale::getOrderId, orderId)
                        .orderByDesc(AfterSale::getCreatedAt)
                        .last("LIMIT 1"));
        if (afterSale == null) {
            return null;
        }
        return OrderResponse.AfterSaleSummary.builder()
                .id(afterSale.getId())
                .type(afterSale.getType())
                .status(afterSale.getStatus())
                .amount(afterSale.getAmount())
                .reason(afterSale.getReason())
                .rejectReason(afterSale.getRejectReason())
                .createdAt(afterSale.getCreatedAt() != null ? afterSale.getCreatedAt().toString() : null)
                .approvedAt(afterSale.getApprovedAt() != null ? afterSale.getApprovedAt().toString() : null)
                .rejectedAt(afterSale.getRejectedAt() != null ? afterSale.getRejectedAt().toString() : null)
                .completedAt(afterSale.getCompletedAt() != null ? afterSale.getCompletedAt().toString() : null)
                .build();
    }

    private OrderResponse toOrderResponseWithItems(Order order) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId()));

        List<OrderItemData> itemDataList = orderItems.stream()
                .map(oi -> OrderItemData.builder()
                        .id(oi.getId())
                        .groupBuyItemId(oi.getGroupBuyItemId())
                        .productId(oi.getProductId())
                        .productName(oi.getProductName())
                        .skuName(oi.getSkuName())
                        .unitPriceAmount(oi.getUnitPriceAmount())
                        .quantity(oi.getQuantity())
                        .totalAmount(oi.getTotalAmount())
                        .build())
                .collect(Collectors.toList());

        return toOrderResponse(order, itemDataList);
    }

    private String toApiOrderStatus(String dbStatus) {
        if (DB_STATUS_PENDING_PAY.equals(dbStatus)) {
            return API_STATUS_PENDING_PAY;
        }
        return dbStatus;
    }

    /**
     * Transient OrderItemEntry implementation derived from cart items.
     */
    private static class CartDerivedOrderItemEntry implements OrderItemEntry {
        private final Long groupBuyItemId;
        private final Integer quantity;

        CartDerivedOrderItemEntry(Long groupBuyItemId, Integer quantity) {
            this.groupBuyItemId = groupBuyItemId;
            this.quantity = quantity;
        }

        @Override
        public Long getGroupBuyItemId() {
            return groupBuyItemId;
        }

        @Override
        public Integer getQuantity() {
            return quantity;
        }
    }
}
