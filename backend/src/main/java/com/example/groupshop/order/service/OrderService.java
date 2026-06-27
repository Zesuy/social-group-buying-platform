package com.example.groupshop.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.groupshop.address.dto.AddressResponse;
import com.example.groupshop.address.service.AddressService;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.model.entity.Address;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.model.entity.OrderItem;
import com.example.groupshop.model.entity.Product;
import com.example.groupshop.model.mapper.AddressMapper;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import com.example.groupshop.model.mapper.GroupBuyMapper;
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
    private final AddressService addressService;

    private static final String DB_STATUS_PENDING_PAY = "pending_pay";
    private static final String API_STATUS_PENDING_PAY = "pendingPay";

    // ── Order Preview ──────────────────────────────────────────────────

    /**
     * Preview order: validate group buy, items, address, stock.
     * Does NOT create an order or reserve stock.
     */
    public OrderPreviewResponse previewOrder(Long userId, OrderPreviewRequest request) {
        // Validate group buy
        GroupBuy groupBuy = validateGroupBuyForPurchase(request.getGroupBuyId());

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
        // Validate group buy
        GroupBuy groupBuy = validateGroupBuyForPurchase(request.getGroupBuyId());

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

    // ── Internal helpers ──────────────────────────────────────────────

    /**
     * Validate that a group buy is purchasable: published, public, started, not ended.
     */
    private GroupBuy validateGroupBuyForPurchase(Long groupBuyId) {
        GroupBuy groupBuy = groupBuyMapper.selectById(groupBuyId);
        if (groupBuy == null) {
            throw new BusinessException(ErrorCode.GROUP_BUY_NOT_PURCHASABLE, "团购不存在");
        }
        if (!"published".equals(groupBuy.getStatus())) {
            throw new BusinessException(ErrorCode.GROUP_BUY_NOT_PURCHASABLE, "团购不可购买");
        }
        if (!"public".equals(groupBuy.getVisibility())) {
            throw new BusinessException(ErrorCode.GROUP_BUY_NOT_PURCHASABLE, "团购不可购买");
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
