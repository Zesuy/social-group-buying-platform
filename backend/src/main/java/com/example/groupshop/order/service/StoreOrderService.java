package com.example.groupshop.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.common.util.CurrentStoreHelper;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.model.entity.Shipment;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.mapper.OrderItemMapper;
import com.example.groupshop.model.mapper.OrderMapper;
import com.example.groupshop.model.mapper.ShipmentMapper;
import com.example.groupshop.order.dto.OrderResponse;
import com.example.groupshop.order.dto.OrderResponse.OrderItemData;
import com.example.groupshop.order.dto.ShipOrderRequest;
import com.example.groupshop.order.dto.ShipOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for store-level order management (leader's store orders).
 *
 * <p>Batch 08: list, detail, and ship orders belonging to the current user's store.
 */
@Service
@RequiredArgsConstructor
public class StoreOrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ShipmentMapper shipmentMapper;
    private final CurrentStoreHelper currentStoreHelper;

    private static final String API_STATUS_PENDING_PAY = "pendingPay";

    // ── List Store Orders ───────────────────────────────────────────────

    /**
     * List orders for the current user's store with optional status filter.
     */
    public PageResponse<OrderResponse> getStoreOrders(Long userId, String status, int page, int pageSize) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Store store = ls.getStore();

        Page<Order> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getStoreId, store.getId())
                .orderByDesc(Order::getCreatedAt);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getOrderStatus, toDbOrderStatus(status));
        }

        Page<Order> result = orderMapper.selectPage(pageObj, wrapper);

        List<OrderResponse> items = result.getRecords().stream()
                .map(this::toOrderResponseWithItems)
                .collect(Collectors.toList());

        return PageResponse.of(items, page, pageSize, result.getTotal());
    }

    // ── Get Store Order Detail ──────────────────────────────────────────

    /**
     * Get an order detail, verifying it belongs to the current user's store.
     */
    public OrderResponse getStoreOrder(Long userId, Long orderId) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Store store = ls.getStore();

        Order order = findOrderForStore(orderId, store.getId());
        return toOrderResponseWithItems(order);
    }

    // ── Ship Order ──────────────────────────────────────────────────────

    /**
     * Ship an order for the current user's store.
     *
     * <p>Uses an atomic conditional update to transition {@code paid → shipped},
     * preventing concurrent requests from creating duplicate shipments.
     *
     * @throws BusinessException with ORDER_ALREADY_SHIPPED if already shipped
     * @throws BusinessException with ORDER_NOT_SHIPPABLE if not in a shippable state
     */
    @Transactional
    public ShipOrderResponse shipOrder(Long userId, Long orderId, ShipOrderRequest request) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Store store = ls.getStore();

        // Verify order exists and belongs to this store
        Order order = findOrderForStore(orderId, store.getId());

        // Atomic condition update: only paid orders can transition to shipped.
        // This prevents concurrent requests from both creating shipments.
        LocalDateTime now = LocalDateTime.now();
        int updated = orderMapper.update(null,
                new LambdaUpdateWrapper<Order>()
                        .eq(Order::getId, order.getId())
                        .eq(Order::getOrderStatus, "paid")
                        .set(Order::getOrderStatus, "shipped")
                        .set(Order::getShippedAt, now));
        if (updated == 0) {
            // Re-read to determine the actual state
            Order current = orderMapper.selectById(order.getId());
            if (current == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            if ("shipped".equals(current.getOrderStatus())) {
                throw new BusinessException(ErrorCode.ORDER_ALREADY_SHIPPED, "订单已发货");
            }
            throw new BusinessException(ErrorCode.ORDER_NOT_SHIPPABLE, "订单不可发货");
        }
        // Sync in-memory state
        order.setOrderStatus("shipped");
        order.setShippedAt(now);

        // Create shipment record (only after successful status transition)
        Shipment shipment = new Shipment();
        shipment.setOrderId(order.getId());
        shipment.setDeliveryType(request.getDeliveryType().getValue());
        shipment.setLogisticsCompany(request.getLogisticsCompany());
        shipment.setTrackingNo(request.getTrackingNo());
        shipment.setShippedBy(userId);
        shipment.setShippedAt(now);
        shipmentMapper.insert(shipment);

        OrderResponse orderResponse = toOrderResponseWithItems(order);
        ShipOrderResponse.ShipmentData shipmentData = toShipmentData(shipment);

        return ShipOrderResponse.builder()
                .order(orderResponse)
                .shipment(shipmentData)
                .build();
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private Order findOrderForStore(Long orderId, Long storeId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!order.getStoreId().equals(storeId)) {
            throw new BusinessException(ErrorCode.STORE_FORBIDDEN);
        }
        return order;
    }

    private String toDbOrderStatus(String apiStatus) {
        if (API_STATUS_PENDING_PAY.equals(apiStatus)) {
            return "pending_pay";
        }
        return apiStatus;
    }

    private OrderResponse toOrderResponseWithItems(Order order) {
        List<OrderItemData> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<com.example.groupshop.model.entity.OrderItem>()
                        .eq(com.example.groupshop.model.entity.OrderItem::getOrderId, order.getId()))
                .stream()
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

    private String toApiOrderStatus(String dbStatus) {
        if ("pending_pay".equals(dbStatus)) {
            return API_STATUS_PENDING_PAY;
        }
        return dbStatus;
    }

    private ShipOrderResponse.ShipmentData toShipmentData(Shipment shipment) {
        return ShipOrderResponse.ShipmentData.builder()
                .id(shipment.getId())
                .orderId(shipment.getOrderId())
                .deliveryType(shipment.getDeliveryType())
                .logisticsCompany(shipment.getLogisticsCompany())
                .trackingNo(shipment.getTrackingNo())
                .shippedAt(shipment.getShippedAt() != null ? shipment.getShippedAt().toString() : null)
                .build();
    }
}
