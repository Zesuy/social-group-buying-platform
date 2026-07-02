package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户优惠券表 — user_coupons
 *
 * <p>User-owned coupons claimed from a store coupon.
 * Status: unused / locked / used / expired.
 */
@Data
@TableName("user_coupons")
public class UserCoupon {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Long couponId;
    private String couponName;
    private String couponType;
    private Long amount;
    private Long thresholdAmount;
    private String status;
    private Long lockedOrderId;
    private LocalDateTime claimedAt;
    private LocalDateTime lockedAt;
    private LocalDateTime usedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
