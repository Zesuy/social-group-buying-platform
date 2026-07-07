package com.example.groupshop.aftersale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.groupshop.aftersale.dto.AfterSaleResponse;
import com.example.groupshop.aftersale.dto.CreateAfterSaleRequest;
import com.example.groupshop.aftersale.dto.RejectAfterSaleRequest;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.common.util.CurrentStoreHelper;
import com.example.groupshop.memberlevel.service.MemberLevelRuleService;
import com.example.groupshop.model.entity.AfterSale;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.entity.MemberRelation;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.model.entity.OrderItem;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.AfterSaleMapper;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import com.example.groupshop.model.mapper.MemberRelationMapper;
import com.example.groupshop.model.mapper.OrderItemMapper;
import com.example.groupshop.model.mapper.OrderMapper;
import com.example.groupshop.model.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for after-sale (refund) operations.
 *
 * <p>Handles buyer application, store approval/rejection, and simulated
 * refund completion with stock restoration and member stats reversal.
 *
 * <p>All state transitions use conditional (optimistic) updates to prevent
 * concurrent double-execution of side effects.
 */
@Service
@RequiredArgsConstructor
public class AfterSaleService {

    private final AfterSaleMapper afterSaleMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GroupBuyItemMapper groupBuyItemMapper;
    private final MemberRelationMapper memberRelationMapper;
    private final UserMapper userMapper;
    private final MemberLevelRuleService memberLevelRuleService;
    private final CurrentStoreHelper currentStoreHelper;

    // ── DB status constants ───────────────────────────────────────────────

    private static final String DB_ORDER_STATUS_PAID = "paid";
    private static final String DB_ORDER_STATUS_SHIPPED = "shipped";
    private static final String DB_ORDER_STATUS_COMPLETED = "completed";

    /** DB order statuses that allow after-sale application */
    private static final java.util.Set<String> APPLICABLE_ORDER_STATUSES =
            java.util.Set.of(DB_ORDER_STATUS_PAID, DB_ORDER_STATUS_SHIPPED, DB_ORDER_STATUS_COMPLETED);

    // ── Buyer: Create After-Sale ─────────────────────────────────────────

    /**
     * Create an after-sale (refund) request for an order.
     *
     * <p>Uses a conditional update on the order to atomically reserve it
     * for after-sale processing, preventing concurrent duplicate creations.
     */
    @Transactional
    public AfterSaleResponse createAfterSale(Long userId, Long orderId, CreateAfterSaleRequest request) {
        // Verify order ownership
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // Must be paid
        if (!"paid".equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.AFTER_SALE_NOT_APPLICABLE, "订单未支付，不可申请售后");
        }

        // Check no existing pending/approved after-sale (best-effort pre-check;
        // the real atomic guard is the conditional order-status update below)
        long existingCount = afterSaleMapper.selectCount(
                new LambdaQueryWrapper<AfterSale>()
                        .eq(AfterSale::getOrderId, orderId)
                        .in(AfterSale::getStatus, "pending", "approved"));
        if (existingCount > 0) {
            throw new BusinessException(ErrorCode.AFTER_SALE_IN_PROGRESS, "当前订单已有进行中的售后单");
        }

        // Order must be in an applicable state
        String dbOrderStatus = order.getOrderStatus();
        if (!APPLICABLE_ORDER_STATUSES.contains(dbOrderStatus)) {
            throw new BusinessException(ErrorCode.AFTER_SALE_NOT_APPLICABLE, "当前订单状态不可申请售后");
        }

        // ── Atomic: transition order from applicable state → after_sale ─────
        // Only one transaction will succeed, preventing concurrent after-sale creation.
        int updated = orderMapper.update(null,
                new LambdaUpdateWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getPayStatus, "paid")
                        .in(Order::getOrderStatus, APPLICABLE_ORDER_STATUSES.toArray())
                        .set(Order::getOrderStatus, "after_sale"));
        if (updated == 0) {
            // Either someone else already transitioned it, or the state changed.
            // Re-read to give the correct error.
            Order current = orderMapper.selectById(orderId);
            if (current != null && "after_sale".equals(current.getOrderStatus())) {
                // The count pre-check passed but we lost the race — another
                // transaction already created an after-sale.
                throw new BusinessException(ErrorCode.AFTER_SALE_IN_PROGRESS, "当前订单已有进行中的售后单");
            }
            throw new BusinessException(ErrorCode.AFTER_SALE_NOT_APPLICABLE, "当前订单状态不可申请售后");
        }

        // We own the order now — create the after-sale record
        AfterSale afterSale = new AfterSale();
        afterSale.setOrderId(orderId);
        afterSale.setUserId(userId);
        afterSale.setLeaderId(order.getLeaderId());
        afterSale.setStoreId(order.getStoreId());
        afterSale.setType(request.getType());
        afterSale.setReason(request.getReason());
        afterSale.setStatus("pending");
        afterSale.setAmount(order.getPayAmount());
        afterSale.setOriginalOrderStatus(dbOrderStatus);
        afterSaleMapper.insert(afterSale);

        // Re-read to get DB-populated fields (createdAt)
        afterSale = afterSaleMapper.selectById(afterSale.getId());

        return toAfterSaleResponse(afterSale, order);
    }

    // ── Buyer: List My After-Sales ───────────────────────────────────────

    /**
     * List current user's after-sale requests, most recent first.
     */
    public PageResponse<AfterSaleResponse> getMyAfterSales(Long userId, int page, int pageSize) {
        Page<AfterSale> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getUserId, userId)
                .orderByDesc(AfterSale::getCreatedAt);

        Page<AfterSale> result = afterSaleMapper.selectPage(pageObj, wrapper);

        List<AfterSaleResponse> items = result.getRecords().stream()
                .map(this::toAfterSaleResponseWithOrder)
                .collect(Collectors.toList());

        return PageResponse.of(items, page, pageSize, result.getTotal());
    }

    // ── Buyer: Get My After-Sale Detail ──────────────────────────────────

    /**
     * Get a single after-sale detail. Only the requesting user can view.
     */
    public AfterSaleResponse getMyAfterSale(Long userId, Long afterSaleId) {
        AfterSale afterSale = findAfterSaleForUser(afterSaleId, userId);
        return toAfterSaleResponseWithOrder(afterSale);
    }

    // ── Store: List Store After-Sales ────────────────────────────────────

    /**
     * List after-sales for the current user's store, most recent first.
     */
    public PageResponse<AfterSaleResponse> getStoreAfterSales(Long userId, int page, int pageSize) {
        return getStoreAfterSales(userId, null, page, pageSize);
    }

    /**
     * List after-sales for the current user's store, most recent first.
     */
    public PageResponse<AfterSaleResponse> getStoreAfterSales(Long userId, String status, int page, int pageSize) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Long storeId = ls.getStore().getId();

        Page<AfterSale> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getStoreId, storeId)
                .orderByDesc(AfterSale::getCreatedAt);
        if (status != null && !status.isBlank()) {
            wrapper.eq(AfterSale::getStatus, status.trim());
        }

        Page<AfterSale> result = afterSaleMapper.selectPage(pageObj, wrapper);

        List<AfterSaleResponse> items = result.getRecords().stream()
                .map(this::toAfterSaleResponseWithOrder)
                .collect(Collectors.toList());

        return PageResponse.of(items, page, pageSize, result.getTotal());
    }

    // ── Store: Get Store After-Sale Detail ───────────────────────────────

    /**
     * Get a single after-sale detail for the current user's store.
     */
    public AfterSaleResponse getStoreAfterSale(Long userId, Long afterSaleId) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Long storeId = ls.getStore().getId();

        AfterSale afterSale = findAfterSaleForStore(afterSaleId, storeId);
        return toAfterSaleResponseWithOrder(afterSale);
    }

    // ── Store: Approve After-Sale ────────────────────────────────────────

    /**
     * Approve an after-sale request. Uses a conditional update
     * ({@code WHERE id = ? AND status = 'pending'}) to prevent concurrent
     * approve/reject races.
     */
    @Transactional
    public AfterSaleResponse approveAfterSale(Long userId, Long afterSaleId) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Long storeId = ls.getStore().getId();

        // Verify the after-sale belongs to this store
        findAfterSaleForStore(afterSaleId, storeId);

        // Atomic: transition only if still pending
        LocalDateTime now = LocalDateTime.now();
        int updated = afterSaleMapper.update(null,
                new LambdaUpdateWrapper<AfterSale>()
                        .eq(AfterSale::getId, afterSaleId)
                        .eq(AfterSale::getStatus, "pending")
                        .set(AfterSale::getStatus, "approved")
                        .set(AfterSale::getApprovedAt, now));
        if (updated == 0) {
            AfterSale current = afterSaleMapper.selectById(afterSaleId);
            if (current == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.AFTER_SALE_NOT_APPROVABLE, "仅待审核的售后单可审核通过");
        }

        // Re-read and build response
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        Order order = orderMapper.selectById(afterSale.getOrderId());
        return toAfterSaleResponse(afterSale, order);
    }

    // ── Store: Reject After-Sale ─────────────────────────────────────────

    /**
     * Reject an after-sale request. Uses a conditional update
     * ({@code WHERE id = ? AND status = 'pending'}) to prevent concurrent
     * approve/reject races. On success, restores the order to its original status.
     */
    @Transactional
    public AfterSaleResponse rejectAfterSale(Long userId, Long afterSaleId, RejectAfterSaleRequest request) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Long storeId = ls.getStore().getId();

        // Verify the after-sale belongs to this store
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!afterSale.getStoreId().equals(storeId)) {
            throw new BusinessException(ErrorCode.STORE_FORBIDDEN, "该售后单不属于当前店铺");
        }

        // Atomic: transition only if still pending
        LocalDateTime now = LocalDateTime.now();
        int updated = afterSaleMapper.update(null,
                new LambdaUpdateWrapper<AfterSale>()
                        .eq(AfterSale::getId, afterSaleId)
                        .eq(AfterSale::getStatus, "pending")
                        .set(AfterSale::getStatus, "rejected")
                        .set(AfterSale::getRejectReason, request.getRejectReason())
                        .set(AfterSale::getRejectedAt, now));
        if (updated == 0) {
            AfterSale current = afterSaleMapper.selectById(afterSaleId);
            if (current == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.AFTER_SALE_NOT_REJECTABLE, "仅待审核的售后单可拒绝");
        }

        // Re-read to get fresh state
        afterSale = afterSaleMapper.selectById(afterSaleId);

        // Restore order to original status (outside the conditional guard since
        // the after-sale is now owned by this transaction)
        Order order = orderMapper.selectById(afterSale.getOrderId());
        if (order != null) {
            order.setOrderStatus(afterSale.getOriginalOrderStatus());
            orderMapper.updateById(order);
        }

        return toAfterSaleResponse(afterSale, order);
    }

    // ── Store: Complete Refund ───────────────────────────────────────────

    /**
     * Complete the refund for an approved after-sale.
     *
     * <p>Uses a conditional update on the after-sale
     * ({@code WHERE id = ? AND status = 'approved'}) as the atomic gate.
     * Only the first transaction to update 1 row proceeds with the side
     * effects (stock restore, member reversal). Subsequent transactions see
     * {@code completed} status and return idempotently without re-execution.
     *
     * <p>Within the winning transaction:
     * <ul>
     *   <li>Updates after-sale to completed</li>
     *   <li>Updates order to refunded/refunded</li>
     *   <li>Restores group stock (groupStock += quantity, soldCount -= quantity, min 0)</li>
     *   <li>Reverses member stats and recalculates level</li>
     * </ul>
     */
    @Transactional
    public AfterSaleResponse completeRefund(Long userId, Long afterSaleId) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Long storeId = ls.getStore().getId();

        // Verify the after-sale belongs to this store
        findAfterSaleForStore(afterSaleId, storeId);

        // Check idempotent case: already completed (fast path, no write needed)
        AfterSale current = afterSaleMapper.selectById(afterSaleId);
        if ("completed".equals(current.getStatus())) {
            Order order = orderMapper.selectById(current.getOrderId());
            if (order != null && "refunded".equals(order.getOrderStatus())
                    && "refunded".equals(order.getPayStatus())) {
                return toAfterSaleResponse(current, order);
            }
        }

        // ── Atomic: transition only if still approved, gate for all side effects ──
        LocalDateTime now = LocalDateTime.now();
        int updated = afterSaleMapper.update(null,
                new LambdaUpdateWrapper<AfterSale>()
                        .eq(AfterSale::getId, afterSaleId)
                        .eq(AfterSale::getStatus, "approved")
                        .set(AfterSale::getStatus, "completed")
                        .set(AfterSale::getCompletedAt, now));
        if (updated == 0) {
            AfterSale retry = afterSaleMapper.selectById(afterSaleId);
            if (retry == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            if ("completed".equals(retry.getStatus())) {
                // Another transaction completed the refund — return its result idempotently
                Order order = orderMapper.selectById(retry.getOrderId());
                return toAfterSaleResponse(retry, order);
            }
            throw new BusinessException(ErrorCode.AFTER_SALE_NOT_REFUNDABLE, "仅审核通过的售后单可退款");
        }

        // ── We won the race — execute side effects ──────────────────────────

        // Re-read for fresh after-sale state
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);

        // 1. Update order to refunded
        Order order = orderMapper.selectById(afterSale.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在");
        }
        order.setOrderStatus("refunded");
        order.setPayStatus("refunded");
        orderMapper.updateById(order);

        // 2. Restore group stock for each order item
        restoreGroupStock(order.getId());

        // 3. Reverse member stats
        reverseMemberStats(order);

        return toAfterSaleResponse(afterSale, order);
    }

    // ── Internal: Stock Restoration ───────────────────────────────────────

    /**
     * Restore group stock: groupStock += quantity, soldCount -= quantity (min 0).
     */
    private void restoreGroupStock(Long orderId) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId));

        for (OrderItem item : orderItems) {
            if (item.getGroupBuyItemId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            int quantity = item.getQuantity();

            // Atomic restore: groupStock += quantity, soldCount = GREATEST(0, soldCount - quantity)
            groupBuyItemMapper.update(null,
                    new LambdaUpdateWrapper<GroupBuyItem>()
                            .eq(GroupBuyItem::getId, item.getGroupBuyItemId())
                            .setSql("group_stock = group_stock + " + quantity
                                    + ", sold_count = GREATEST(0, sold_count - " + quantity + ")"));
        }
    }

    // ── Internal: Member Stats Reversal ───────────────────────────────────

    /**
     * Reverse member stats on refund and recalculate level.
     *
     * <p>Always decrements total_orders and recalculates level, even when
     * payAmount is 0 (fully discounted orders). Only the amount/growth
     * subtraction is skipped when payAmount <= 0.
     */
    private void reverseMemberStats(Order order) {
        if (order.getPayAmount() != null && order.getPayAmount() > 0) {
            memberRelationMapper.reverseOnRefund(
                    order.getUserId(),
                    order.getStoreId(),
                    order.getPayAmount());
        } else {
            // Pay amount is 0 or null — only decrement total_orders without
            // affecting amount/growth values.
            memberRelationMapper.update(null,
                    new LambdaUpdateWrapper<MemberRelation>()
                            .eq(MemberRelation::getUserId, order.getUserId())
                            .eq(MemberRelation::getStoreId, order.getStoreId())
                            .setSql("total_orders = GREATEST(0, total_orders - 1)"));
        }

        // Recalculate level name if member relation exists
        MemberRelation relation = memberRelationMapper.selectOne(
                new LambdaQueryWrapper<MemberRelation>()
                        .eq(MemberRelation::getUserId, order.getUserId())
                        .eq(MemberRelation::getStoreId, order.getStoreId()));

        if (relation != null) {
            memberLevelRuleService.recalculateMemberLevel(
                    order.getStoreId(), relation.getId(), relation.getGrowthValue());
        }
    }

    // ── Internal: Find Helpers ────────────────────────────────────────────

    private AfterSale findAfterSaleForUser(Long afterSaleId, Long userId) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!afterSale.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return afterSale;
    }

    private AfterSale findAfterSaleForStore(Long afterSaleId, Long storeId) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!afterSale.getStoreId().equals(storeId)) {
            throw new BusinessException(ErrorCode.STORE_FORBIDDEN, "该售后单不属于当前店铺");
        }
        return afterSale;
    }

    // ── Response Mapping ─────────────────────────────────────────────────

    private AfterSaleResponse toAfterSaleResponse(AfterSale afterSale, Order order) {
        AfterSaleResponse.AfterSaleResponseBuilder builder = AfterSaleResponse.builder()
                .id(afterSale.getId())
                .orderId(afterSale.getOrderId())
                .userId(afterSale.getUserId())
                .leaderId(afterSale.getLeaderId())
                .storeId(afterSale.getStoreId())
                .type(afterSale.getType())
                .reason(afterSale.getReason())
                .status(afterSale.getStatus())
                .amount(afterSale.getAmount())
                .rejectReason(afterSale.getRejectReason())
                .approvedAt(afterSale.getApprovedAt() != null ? afterSale.getApprovedAt().toString() : null)
                .rejectedAt(afterSale.getRejectedAt() != null ? afterSale.getRejectedAt().toString() : null)
                .completedAt(afterSale.getCompletedAt() != null ? afterSale.getCompletedAt().toString() : null)
                .createdAt(afterSale.getCreatedAt() != null ? afterSale.getCreatedAt().toString() : null)
                .updatedAt(afterSale.getUpdatedAt() != null ? afterSale.getUpdatedAt().toString() : null)
                .originalOrderStatus(toApiOrderStatus(afterSale.getOriginalOrderStatus()));

        if (order != null) {
            builder.orderNo(order.getOrderNo())
                    .orderStatus(toApiOrderStatus(order.getOrderStatus()))
                    .payStatus(order.getPayStatus())
                    .receiverName(order.getReceiverName())
                    .receiverPhone(order.getReceiverPhone())
                    .fullAddress(order.getFullAddress());
        }

        User buyer = userMapper.selectById(afterSale.getUserId());
        if (buyer != null) {
            builder.buyerNickname(buyer.getNickname())
                    .buyerAvatarUrl(buyer.getAvatarUrl());
        }

        return builder.build();
    }

    private AfterSaleResponse toAfterSaleResponseWithOrder(AfterSale afterSale) {
        Order order = orderMapper.selectById(afterSale.getOrderId());
        return toAfterSaleResponse(afterSale, order);
    }

    private String toApiOrderStatus(String dbStatus) {
        if ("pending_pay".equals(dbStatus)) {
            return "pendingPay";
        }
        return dbStatus;
    }
}
