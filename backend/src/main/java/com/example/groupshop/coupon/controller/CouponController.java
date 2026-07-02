package com.example.groupshop.coupon.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.coupon.dto.AvailableCouponResponse;
import com.example.groupshop.coupon.dto.CouponResponse;
import com.example.groupshop.coupon.dto.CreateCouponRequest;
import com.example.groupshop.coupon.dto.UpdateCouponRequest;
import com.example.groupshop.coupon.dto.UserCouponResponse;
import com.example.groupshop.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for coupon management and claiming.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ── Store Coupon Management ──────────────────────────────────────────

    /**
     * List coupons for the current user's store.
     */
    @GetMapping("/my/store/coupons")
    public ApiResponse<List<CouponResponse>> getStoreCoupons(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.success(couponService.getStoreCoupons(userId));
    }

    /**
     * Create a coupon for the current user's store.
     */
    @PostMapping("/my/store/coupons")
    public ApiResponse<CouponResponse> createCoupon(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody CreateCouponRequest request) {
        return ApiResponse.success(couponService.createCoupon(userId, request));
    }

    /**
     * Partial-update a store coupon.
     */
    @PatchMapping("/my/store/coupons/{couponId}")
    public ApiResponse<CouponResponse> updateCoupon(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long couponId,
            @Valid @RequestBody UpdateCouponRequest request) {
        return ApiResponse.success(couponService.updateCoupon(userId, couponId, request));
    }

    /**
     * Disable a store coupon (no new claims).
     */
    @PostMapping("/my/store/coupons/{couponId}/disable")
    public ApiResponse<Void> disableCoupon(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long couponId) {
        couponService.disableCoupon(userId, couponId);
        return ApiResponse.success();
    }

    // ── Public Coupon Listing ────────────────────────────────────────────

    /**
     * List available coupons for a group buy (public, no login required).
     */
    @GetMapping("/group-buys/{groupBuyId}/coupons")
    public ApiResponse<List<AvailableCouponResponse>> getGroupBuyCoupons(
            @PathVariable Long groupBuyId) {
        return ApiResponse.success(couponService.getAvailableCouponsForGroupBuy(groupBuyId));
    }

    // ── User Claiming ────────────────────────────────────────────────────

    /**
     * Claim a coupon for the current user.
     */
    @PostMapping("/coupons/{couponId}/claim")
    public ApiResponse<UserCouponResponse> claimCoupon(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long couponId) {
        return ApiResponse.success(couponService.claimCoupon(userId, couponId));
    }

    // ── My Coupons ───────────────────────────────────────────────────────

    /**
     * List current user's coupons with optional status filter.
     */
    @GetMapping("/my/coupons")
    public ApiResponse<List<UserCouponResponse>> getMyCoupons(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(couponService.getMyCoupons(userId, status));
    }
}
