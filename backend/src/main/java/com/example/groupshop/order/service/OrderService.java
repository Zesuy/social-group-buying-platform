package com.example.groupshop.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.groupshop.address.dto.AddressResponse;
import com.example.groupshop.address.service.AddressService;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.model.entity.Address;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.entity.GroupBuyShareToken;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.model.entity.OrderItem;
import com.example.groupshop.model.entity.Product;
import com.example.groupshop.model.mapper.AddressMapper;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import com.example.groupshop.model.mapper.GroupBuyMapper;
import com.example.groupshop.model.mapper.GroupBuyShareTokenMapper;
import com.example.groupshop.model.mapper.MemberRelationMapper;
import com.example.groupshop.model.mapper.OrderItemMapper;
import com.example.groupshop.model.mapper.OrderMapper;
import com.example.groupshop.model.mapper.ProductMapper;
import com.example.groupshop.order.dto.CreateOrderRequest;
import com.example.groupshop.order.dto.OrderItemEntry;
import com.example.groupshop.order.dto.OrderPreviewRequest;
import com.example.groupshop.order.dto.OrderPreviewResponse;
import com.example.groupshop.order.dto.OrderPreviewResponse.AddressPreview;
import com.example.groupshop.order.dto.OrderPreviewResponse.ItemPreview;
import com.example.groupshop.order.dto.OrderResponse;
import com.example.groupshop.order.dto.OrderResponse.OrderItemData;
import com.example.groupshop.common.util.CurrentStoreHelper;
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
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GroupBuyMapper groupBuyMapper;
    private final GroupBuyItemMapper groupBuyItemMapper;
    private final AddressMapper addressMapper;
    private final ProductMapper productMapper;
    private final MemberRelationMapper memberRelationMapper;
    private final AddressService addressService;
    private final GroupBuyShareTokenMapper groupBuyShareTokenMapper;

    private static final String DB_STATUS_PENDING_PAY = "pending_pay";
    private static final String API_STATUS_PENDING_PAY = "pendingPay";

    // ── Order Preview ──────────────────────────────────────────────────

    /**
     * Preview order: validate group buy, items, address, stock.
     * Does NOT create an order or reserve stock.
     */
    public OrderPreviewResponse previewOrder(Long userId, OrderPreviewRequest request) {
        // Validate group buy with optional share token
        GroupBuy groupBuy = validateGroupBuyForPurchase(request.getGroupBuyId(), request.getShareToken());

        // Validate items belong to group buy and have sufficient stock
        List<GroupBuyItem> gbItems = validateAndLoadItems(request.getGroupBuyId(), request.getItems());

        // Validate address
        Address address = addressService.findAddressForUser(request.getAddressId(), userId);

        // Calculate amounts
        long totalAmount = 0;
        List<ItemPreview> itemPreviews = new ArrayList<>();
        Map<Long, GroupBuyItem> itemMap = gbItems.stream()
                .collect(Collectors.toMap(GroupBuyItem::getId, i -> i));

        for (OrderPreviewRequest.OrderItemEntry entry : request.getItems()) {
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

        return OrderPreviewResponse.builder()
                .groupBuyId(request.getGroupBuyId())
                .address(toAddressPreview(address))
                .items(itemPreviews)
                .totalAmount(totalAmount)
                .discountAmount(0L)
                .payAmount(totalAmount)
                .build();
    }

    // ── Create Order ───────────────────────────────────────────────────

    /**
     * Create an order. Re-validates everything from preview, saves address
     * and item snapshots. Does NOT deduct stock.
     */
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        // Validate group buy with optional share token
        GroupBuy groupBuy = validateGroupBuyForPurchase(request.getGroupBuyId(), request.getShareToken());

        // Validate items belong to group buy and have sufficient stock
        List<GroupBuyItem> gbItems = validateAndLoadItems(request.getGroupBuyId(), request.getItems());

        // Validate address and get snapshot
        Address address = addressService.findAddressForUser(request.getAddressId(), userId);

        // Build order number: date prefix + IdWorker value
        String orderNo = generateOrderNo();

        // Calculate amounts
        long totalAmount = 0;
        List<OrderItemData> itemDataList = new ArrayList<>();
        Map<Long, GroupBuyItem> itemMap = gbItems.stream()
                .collect(Collectors.toMap(GroupBuyItem::getId, i -> i));

        for (CreateOrderRequest.OrderItemEntry entry : request.getItems()) {
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

        // Create order
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setLeaderId(groupBuy.getLeaderId());
        order.setStoreId(groupBuy.getStoreId());
        order.setGroupBuyId(request.getGroupBuyId());
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
        order.setDiscountAmount(0L);
        order.setPayAmount(totalAmount);

        // Status — DB uses snake_case
        order.setPayStatus("unpaid");
        order.setOrderStatus(DB_STATUS_PENDING_PAY);
        order.setRemark(request.getRemark());

        orderMapper.insert(order);

        // Create order items with snapshots
        for (int i = 0; i < request.getItems().size(); i++) {
            CreateOrderRequest.OrderItemEntry entry = request.getItems().get(i);
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

            itemDataList.get(i).setId(orderItem.getId());
        }

        return toOrderResponse(order, itemDataList);
    }

    // ── My Orders ──────────────────────────────────────────────────────

    /**
     * List current user's orders with optional status filter.
     * API status (camelCase) is converted to DB status (snake_case) for the query.
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

    // ── Cancel Order ───────────────────────────────────────────────────

    /**
     * Cancel an order. Only allowed when orderStatus=pendingPay and payStatus=unpaid.
     */
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = findOrderForUser(orderId, userId);

        if (!DB_STATUS_PENDING_PAY.equals(order.getOrderStatus()) || !"unpaid".equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELABLE, "当前订单状态不可取消");
        }

        order.setOrderStatus("canceled");
        orderMapper.updateById(order);

        return toOrderResponseWithItems(order);
    }

    // ── Simulate Pay (Batch 07) ───────────────────────────────────────

    /**
     * Simulate payment for an order. Only the order owner can pay.
     *
     * <p>In a single transaction:
     * <ol>
     *   <li>Validates order belongs to user and is in pendingPay+unpaid state</li>
     *   <li>Updates order to paid status, sets paidAt</li>
     *   <li>Atomically deducts group_stock and increments sold_count for each item</li>
     *   <li>Creates or updates MemberRelation for the buyer</li>
     * </ol>
     *
     * @throws BusinessException with ORDER_ALREADY_PAID if already paid
     * @throws BusinessException with ORDER_NOT_PAYABLE if not in payable state
     * @throws BusinessException with INSUFFICIENT_STOCK if stock insufficient
     */
    @Transactional
    public OrderResponse simulatePay(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // Check payment state
        if ("paid".equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID, "订单已支付");
        }
        if (!DB_STATUS_PENDING_PAY.equals(order.getOrderStatus()) || !"unpaid".equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PAYABLE, "订单当前状态不可支付");
        }

        // Load items for stock deduction
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId()));

        // Validate and deduct stock atomically (optimistic via MyBatis update with condition)
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
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<GroupBuyItem>()
                            .eq(GroupBuyItem::getId, gbItem.getId())
                            .eq(GroupBuyItem::getGroupStock, gbItem.getGroupStock())
                            .setSql("group_stock = group_stock - " + orderItem.getQuantity()
                                    + ", sold_count = sold_count + " + orderItem.getQuantity()));
            if (updated == 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "库存不足，并发扣减失败");
            }
        }

        // Update order to paid — atomic condition update prevents concurrent duplicate payment
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
            // Already paid or status changed — re-read to determine the right error
            Order current = orderMapper.selectById(order.getId());
            if ("paid".equals(current.getPayStatus())) {
                throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID, "订单已支付");
            }
            throw new BusinessException(ErrorCode.ORDER_NOT_PAYABLE, "订单当前状态不可支付");
        }
        // Sync back the in-memory order so upsertMemberRelation sees current state
        order.setPayStatus("paid");
        order.setOrderStatus("paid");
        order.setPaidAt(now);

        // Create or update member relation
        upsertMemberRelation(order, now);

        return toOrderResponseWithItems(order);
    }

    /**
     * Atomically create or update member relation via DB UPSERT.
     *
     * <p>Uses {@code INSERT ... ON DUPLICATE KEY UPDATE} to eliminate the
     * read-then-write gap that would let concurrent first payments from the
     * same user to the same store both attempt to insert and collide on the
     * unique constraint {@code uk_member_relations_user_store}.
     */
    private void upsertMemberRelation(Order order, LocalDateTime now) {
        memberRelationMapper.upsert(
                order.getUserId(),
                order.getLeaderId(),
                order.getStoreId(),
                order.getPayAmount(),
                now);
    }

    // ── Complete Order (Batch 10) ────────────────────────────────────

    /**
     * Complete (confirm receipt for) an order. Only the order owner can complete.
     *
     * <p>Uses an atomic conditional update to transition {@code shipped → completed},
     * preventing concurrent requests from double-completing.
     *
     * @throws BusinessException with ORDER_ALREADY_COMPLETED if already completed
     * @throws BusinessException with ORDER_NOT_COMPLETABLE if not in a completable state
     */
    @Transactional
    public OrderResponse completeOrder(Long userId, Long orderId) {
        Order order = findOrderForUser(orderId, userId);

        // Atomic condition update: only shipped orders can transition to completed
        LocalDateTime now = LocalDateTime.now();
        int updated = orderMapper.update(null,
                new LambdaUpdateWrapper<Order>()
                        .eq(Order::getId, order.getId())
                        .eq(Order::getOrderStatus, "shipped")
                        .set(Order::getOrderStatus, "completed")
                        .set(Order::getCompletedAt, now));
        if (updated == 0) {
            // Re-read to determine the actual state
            Order current = orderMapper.selectById(order.getId());
            if (current == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            if ("completed".equals(current.getOrderStatus())) {
                throw new BusinessException(ErrorCode.ORDER_ALREADY_COMPLETED, "订单已完成");
            }
            throw new BusinessException(ErrorCode.ORDER_NOT_COMPLETABLE, "当前订单状态不可确认收货");
        }
        // Sync in-memory state
        order.setOrderStatus("completed");
        order.setCompletedAt(now);

        return toOrderResponseWithItems(order);
    }

    // ── Internal helpers ──────────────────────────────────────────────

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

        // Handle visibility
        if ("hidden".equals(groupBuy.getVisibility())) {
            // Hidden group buy requires a valid matching share token
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
     * Also checks for duplicate items, empty items, and positive quantities.
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
        // Use date prefix + IdWorker value for uniqueness within VARCHAR(64)
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr();
    }

    /**
     * Convert API camelCase order status to DB snake_case.
     */
    private String toDbOrderStatus(String apiStatus) {
        if (API_STATUS_PENDING_PAY.equals(apiStatus)) {
            return DB_STATUS_PENDING_PAY;
        }
        // For direct matches (paid, shipped, completed, canceled), return as-is
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

    /**
     * Convert DB snake_case order status to API camelCase.
     */
    private String toApiOrderStatus(String dbStatus) {
        if (DB_STATUS_PENDING_PAY.equals(dbStatus)) {
            return API_STATUS_PENDING_PAY;
        }
        return dbStatus;
    }
}
