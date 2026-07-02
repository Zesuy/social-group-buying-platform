package com.example.groupshop.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request DTO for creating a coupon.
 */
@Data
public class CreateCouponRequest {

    @NotBlank
    private String name;

    /** amount / red_packet */
    @NotBlank
    private String couponType;

    /** Discount amount in fen */
    @NotNull
    @Positive
    private Long amount;

    /** Threshold in fen (0 = no threshold) */
    private Long thresholdAmount = 0L;

    @NotNull
    @Positive
    private Integer totalQuantity;

    private Integer perUserLimit = 1;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;
}
