package com.example.groupshop.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for a coupon as seen by a buyer when browsing
 * available coupons for a group buy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableCouponResponse {

    private Long id;
    private String name;
    private String couponType;
    private Long amount;
    private Long thresholdAmount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalQuantity;
    private Integer claimedQuantity;
    private Integer perUserLimit;
    /** Reason if not available; null if available */
    private String unavailableReason;
}
