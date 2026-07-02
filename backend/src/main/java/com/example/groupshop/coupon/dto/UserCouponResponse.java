package com.example.groupshop.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for a user's coupon.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponResponse {

    private Long id;
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
}
