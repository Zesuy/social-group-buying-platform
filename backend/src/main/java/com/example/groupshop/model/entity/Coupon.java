package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优惠券表 — coupons
 *
 * <p>Store-level coupons. coupon_type: amount / red_packet.
 * Both are treated as fixed-amount discounts in this batch.
 */
@Data
@TableName("coupons")
public class Coupon {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long storeId;
    private String name;
    private String couponType;
    private String claimCondition;
    private Long amount;
    private Long thresholdAmount;
    private Integer totalQuantity;
    private Integer claimedQuantity;
    private Integer perUserLimit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
