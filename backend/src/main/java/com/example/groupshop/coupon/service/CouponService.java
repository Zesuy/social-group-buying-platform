package com.example.groupshop.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.util.CurrentStoreHelper;
import com.example.groupshop.coupon.dto.AvailableCouponResponse;
import com.example.groupshop.coupon.dto.CouponResponse;
import com.example.groupshop.coupon.dto.CreateCouponRequest;
import com.example.groupshop.coupon.dto.UpdateCouponRequest;
import com.example.groupshop.coupon.dto.UserCouponResponse;
import com.example.groupshop.model.entity.Coupon;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.UserCoupon;
import com.example.groupshop.model.mapper.CouponMapper;
import com.example.groupshop.model.mapper.GroupBuyMapper;
import com.example.groupshop.model.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for coupon management.
 *
 * <p>Handles store coupon CRUD, user claiming, locking/releasing for orders,
 * and payment-time write-off.
 */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final GroupBuyMapper groupBuyMapper;
    private final CurrentStoreHelper currentStoreHelper;

    // ── Store Coupon Management ─────────────────────────────────────────

    /**
     * List coupons for the current user's store.
     */
    public List<CouponResponse> getStoreCoupons(Long userId) {
        Long storeId = currentStoreHelper.getLeaderAndStore(userId).getStore().getId();

        List<Coupon> coupons = couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getStoreId, storeId)
                        .orderByDesc(Coupon::getCreatedAt));

        return coupons.stream()
                .map(this::toCouponResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a coupon for the current user's store.
     */
    @Transactional
    public CouponResponse createCoupon(Long userId, CreateCouponRequest request) {
        Long storeId = currentStoreHelper.getLeaderAndStore(userId).getStore().getId();

        validateCouponType(request.getCouponType());

        Coupon coupon = new Coupon();
        coupon.setStoreId(storeId);
        coupon.setName(request.getName());
        coupon.setCouponType(request.getCouponType());
        coupon.setAmount(request.getAmount());
        coupon.setThresholdAmount(request.getThresholdAmount() != null ? request.getThresholdAmount() : 0L);
        coupon.setTotalQuantity(request.getTotalQuantity());
        coupon.setClaimedQuantity(0);
        coupon.setPerUserLimit(request.getPerUserLimit() != null ? request.getPerUserLimit() : 1);
        coupon.setStartTime(request.getStartTime());
        coupon.setEndTime(request.getEndTime());
        coupon.setStatus("active");

        couponMapper.insert(coupon);
        return toCouponResponse(coupon);
    }

    /**
     * Partial-update a coupon.
     */
    @Transactional
    public CouponResponse updateCoupon(Long userId, Long couponId, UpdateCouponRequest request) {
        Long storeId = currentStoreHelper.getLeaderAndStore(userId).getStore().getId();

        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!coupon.getStoreId().equals(storeId)) {
            throw new BusinessException(ErrorCode.STORE_FORBIDDEN);
        }

        if (request.getName() != null) coupon.setName(request.getName());
        if (request.getCouponType() != null) {
            validateCouponType(request.getCouponType());
            coupon.setCouponType(request.getCouponType());
        }
        if (request.getAmount() != null) coupon.setAmount(request.getAmount());
        if (request.getThresholdAmount() != null) coupon.setThresholdAmount(request.getThresholdAmount());
        if (request.getTotalQuantity() != null) coupon.setTotalQuantity(request.getTotalQuantity());
        if (request.getPerUserLimit() != null) coupon.setPerUserLimit(request.getPerUserLimit());
        if (request.getStartTime() != null) coupon.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) coupon.setEndTime(request.getEndTime());

        couponMapper.updateById(coupon);
        return toCouponResponse(coupon);
    }

    /**
     * Disable a coupon (no new claims, existing locked coupons still usable).
     */
    @Transactional
    public void disableCoupon(Long userId, Long couponId) {
        Long storeId = currentStoreHelper.getLeaderAndStore(userId).getStore().getId();

        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!coupon.getStoreId().equals(storeId)) {
            throw new BusinessException(ErrorCode.STORE_FORBIDDEN);
        }

        coupon.setStatus("disabled");
        couponMapper.updateById(coupon);
    }

    // ── Public Coupon Listing ──────────────────────────────────────────

    /**
     * List available coupons for a group buy (same store, active, in stock, not expired).
     * Returns all coupons with availability reasons.
     */
    public List<AvailableCouponResponse> getAvailableCouponsForGroupBuy(Long groupBuyId) {
        GroupBuy groupBuy = groupBuyMapper.selectById(groupBuyId);
        if (groupBuy == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "团购不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Coupon> coupons = couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getStoreId, groupBuy.getStoreId())
                        .eq(Coupon::getStatus, "active")
                        .le(Coupon::getStartTime, now)
                        .ge(Coupon::getEndTime, now)
                        // Only show coupons that still have stock
                        .apply("claimed_quantity < total_quantity"));

        return coupons.stream()
                .map(c -> toAvailableCouponResponse(c, null))
                .collect(Collectors.toList());
    }

    /**
     * Get available and unavailable coupons for a user during order preview.
     * Returns all store coupons with availability reasons based on user context.
     */
    public CouponCheckResult getCouponsForOrderPreview(Long userId, Long groupBuyId, Long totalAmount) {
        GroupBuy groupBuy = groupBuyMapper.selectById(groupBuyId);
        if (groupBuy == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "团购不存在");
        }

        // Lazy expire user coupons before checking
        lazyExpireUserCoupons(userId);

        List<AvailableCouponResponse> available = new ArrayList<>();
        List<AvailableCouponResponse> unavailable = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        // Get all active store coupons for the group buy's store
        List<Coupon> storeCoupons = couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getStoreId, groupBuy.getStoreId())
                        .eq(Coupon::getStatus, "active"));

        // Get user's unused coupons for the same store
        if (!storeCoupons.isEmpty()) {
            List<UserCoupon> userCoupons = userCouponMapper.selectList(
                    new LambdaQueryWrapper<UserCoupon>()
                            .eq(UserCoupon::getUserId, userId)
                            .eq(UserCoupon::getStatus, "unused")
                            .in(UserCoupon::getCouponId,
                                    storeCoupons.stream().map(Coupon::getId).collect(Collectors.toList())));

            if (!userCoupons.isEmpty()) {
                // Map coupon template by ID
                java.util.Map<Long, Coupon> couponMap = storeCoupons.stream()
                        .collect(Collectors.toMap(Coupon::getId, c -> c));

                for (UserCoupon uc : userCoupons) {
                    Coupon template = couponMap.get(uc.getCouponId());
                    if (template == null) {
                        continue;
                    }

                    AvailableCouponResponse resp = toAvailableCouponResponse(template, uc);

                    // Check coupon validity
                    String reason = null;
                    if (now.isBefore(template.getStartTime())) {
                        reason = "优惠券尚未到可用时间";
                    } else if (now.isAfter(template.getEndTime())) {
                        reason = "优惠券已过期";
                    } else if (template.getThresholdAmount() > 0 && totalAmount < template.getThresholdAmount()) {
                        reason = "未达到使用门槛";
                    }

                    if (reason != null) {
                        resp.setUnavailableReason(reason);
                        unavailable.add(resp);
                    } else {
                        available.add(resp);
                    }
                }
            }
        }

        return new CouponCheckResult(available, unavailable);
    }

    /**
     * Validate a specific user coupon for an order.
     * Returns the validated UserCoupon if valid.
     */
    public UserCoupon validateUserCouponForOrder(Long userId, Long userCouponId, Long groupBuyId, Long totalAmount) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "优惠券不存在");
        }

        if (!"unused".equals(userCoupon.getStatus())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券状态不可用");
        }

        // Validate coupon template still valid
        Coupon template = couponMapper.selectById(userCoupon.getCouponId());
        if (template == null || !"active".equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券已停用");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(template.getStartTime()) || now.isAfter(template.getEndTime())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券不在有效期内");
        }

        // Validate coupon belongs to same store as group buy
        GroupBuy groupBuy = groupBuyMapper.selectById(groupBuyId);
        if (groupBuy == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "团购不存在");
        }
        if (!template.getStoreId().equals(groupBuy.getStoreId())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券不适用于该团购");
        }

        // Validate threshold
        if (template.getThresholdAmount() > 0 && totalAmount < template.getThresholdAmount()) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "未达到使用门槛");
        }

        return userCoupon;
    }

    // ── User Claiming ───────────────────────────────────────────────────

    /**
     * Claim a coupon for the current user.
     *
     * <p>Uses pessimistic locking (FOR UPDATE) on the coupon row to serialize
     * concurrent claims, preventing TOCTOU races on both the per-user limit
     * and the global stock check.
     */
    @Transactional
    public UserCouponResponse claimCoupon(Long userId, Long couponId) {
        LocalDateTime now = LocalDateTime.now();

        // Lock the coupon row to serialize concurrent claims
        Coupon coupon = couponMapper.selectForUpdate(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "优惠券不存在");
        }
        if (!"active".equals(coupon.getStatus())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券已停用");
        }

        // Validate in time window
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券不在领取时间范围内");
        }

        // Check global stock (under lock — guaranteed fresh)
        if (coupon.getClaimedQuantity() >= coupon.getTotalQuantity()) {
            throw new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK, "优惠券已被领完");
        }

        // Check per-user limit (under lock — race-free)
        long userClaimedCount = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId));
        if (userClaimedCount >= coupon.getPerUserLimit()) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_CLAIMED, "已达到每人限领数量");
        }

        // Atomic stock deduction (still needed since FOR UPDATE doesn't auto-increment)
        int updated = couponMapper.incrementClaimedQuantity(couponId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK, "优惠券已被领完");
        }

        // Create user coupon
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setCouponName(coupon.getName());
        userCoupon.setCouponType(coupon.getCouponType());
        userCoupon.setAmount(coupon.getAmount());
        userCoupon.setThresholdAmount(coupon.getThresholdAmount());
        userCoupon.setStatus("unused");
        userCoupon.setExpiredAt(coupon.getEndTime());
        userCouponMapper.insert(userCoupon);

        return toUserCouponResponse(userCoupon);
    }

    // ── My Coupons ──────────────────────────────────────────────────────

    /**
     * List current user's coupons with optional status filter.
     * Lazy-expires overdue coupons before querying.
     */
    public List<UserCouponResponse> getMyCoupons(Long userId, String status) {
        // Lazy expire overdue coupons
        lazyExpireUserCoupons(userId);

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .orderByDesc(UserCoupon::getClaimedAt);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(UserCoupon::getStatus, status);
        }

        List<UserCoupon> list = userCouponMapper.selectList(wrapper);
        return list.stream()
                .map(this::toUserCouponResponse)
                .collect(Collectors.toList());
    }

    // ── Order Coupon Locking / Releasing / Write-off ──────────────────

    /**
     * Lock a user coupon for an order.
     * Called within the create-order transaction.
     */
    @Transactional
    public void lockUserCoupon(Long userCouponId, Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        int updated = userCouponMapper.lockForOrder(userCouponId, orderId, now);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券锁定失败，可能已被使用");
        }
    }

    /**
     * Release a locked user coupon back to unused.
     * Called when an order is canceled.
     */
    @Transactional
    public void releaseUserCoupon(Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        userCouponMapper.releaseLock(orderId, now);
    }

    /**
     * Mark a locked user coupon as used after payment.
     * Idempotent: only updates if status = 'locked'.
     */
    @Transactional
    public void useUserCoupon(Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        userCouponMapper.markUsed(orderId, now);
    }

    /**
     * Check if an order has a locked coupon.
     */
    public boolean hasLockedCoupon(Long orderId) {
        return userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getLockedOrderId, orderId)
                        .eq(UserCoupon::getStatus, "locked")) > 0;
    }

    // ── Internal ────────────────────────────────────────────────────────

    private void validateCouponType(String couponType) {
        if (!"amount".equals(couponType) && !"red_packet".equals(couponType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "优惠券类型仅支持 amount 和 red_packet");
        }
    }

    /**
     * Lazily expire user coupons that have passed their expiry date.
     */
    private void lazyExpireUserCoupons(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        userCouponMapper.batchExpire(now);
    }

    private AvailableCouponResponse toAvailableCouponResponse(Coupon coupon, UserCoupon userCoupon) {
        return AvailableCouponResponse.builder()
                .id(coupon.getId())
                .name(coupon.getName())
                .couponType(coupon.getCouponType())
                .amount(coupon.getAmount())
                .thresholdAmount(coupon.getThresholdAmount())
                .startTime(coupon.getStartTime())
                .endTime(coupon.getEndTime())
                .totalQuantity(coupon.getTotalQuantity())
                .claimedQuantity(coupon.getClaimedQuantity())
                .perUserLimit(coupon.getPerUserLimit())
                .unavailableReason(null)
                .build();
    }

    private CouponResponse toCouponResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .storeId(coupon.getStoreId())
                .name(coupon.getName())
                .couponType(coupon.getCouponType())
                .amount(coupon.getAmount())
                .thresholdAmount(coupon.getThresholdAmount())
                .totalQuantity(coupon.getTotalQuantity())
                .claimedQuantity(coupon.getClaimedQuantity())
                .perUserLimit(coupon.getPerUserLimit())
                .startTime(coupon.getStartTime())
                .endTime(coupon.getEndTime())
                .status(coupon.getStatus())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }

    private UserCouponResponse toUserCouponResponse(UserCoupon userCoupon) {
        return UserCouponResponse.builder()
                .id(userCoupon.getId())
                .couponId(userCoupon.getCouponId())
                .couponName(userCoupon.getCouponName())
                .couponType(userCoupon.getCouponType())
                .amount(userCoupon.getAmount())
                .thresholdAmount(userCoupon.getThresholdAmount())
                .status(userCoupon.getStatus())
                .lockedOrderId(userCoupon.getLockedOrderId())
                .claimedAt(userCoupon.getClaimedAt())
                .lockedAt(userCoupon.getLockedAt())
                .usedAt(userCoupon.getUsedAt())
                .expiredAt(userCoupon.getExpiredAt())
                .build();
    }

    // ── Inner class for coupon check result ──────────────────────────────

    /**
     * Result of checking available/unavailable coupons for order preview.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CouponCheckResult {
        private List<AvailableCouponResponse> availableCoupons;
        private List<AvailableCouponResponse> unavailableCoupons;
    }
}
