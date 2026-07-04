package com.example.groupshop.coupon.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request DTO for partial-updating a coupon.
 */
@Data
public class UpdateCouponRequest {

    private String name;

    /** amount / red_packet */
    private String couponType;

    /** general / new_subscriber */
    private String claimCondition;

    /** Discount amount in fen */
    @Positive
    private Long amount;

    /** Threshold in fen (0 = no threshold) */
    private Long thresholdAmount;

    @Positive
    private Integer totalQuantity;

    private Integer perUserLimit;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
