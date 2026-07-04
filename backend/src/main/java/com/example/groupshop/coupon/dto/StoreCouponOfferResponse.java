package com.example.groupshop.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Store homepage coupon offer as seen by a buyer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreCouponOfferResponse {

    private Long id;
    private String name;
    private String couponType;
    private String claimCondition;
    private Long amount;
    private Long thresholdAmount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalQuantity;
    private Integer claimedQuantity;
    private Integer perUserLimit;
    private String status;
    private Boolean claimable;
    private Boolean claimed;
    private Boolean viewerSubscribed;
    private String unavailableReason;
}
