package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单表 — orders
 *
 * <p>Full Order entity for order creation, listing, and management.
 */
@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;
    private Long userId;
    private Long leaderId;
    private Long storeId;
    private Long groupBuyId;
    private Long addressId;

    // ── Address snapshot ──────────────────────────────────────────────
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private String fullAddress;

    // ── Amounts (in fen) ─────────────────────────────────────────────
    private Long totalAmount;
    private Long discountAmount;
    private Long payAmount;

    // ── Status ───────────────────────────────────────────────────────
    /** unpaid / paid / refunded */
    private String payStatus;

    /** pending_pay / paid / shipped / completed / canceled / after_sale */
    private String orderStatus;

    private String remark;

    // ── Timestamps ───────────────────────────────────────────────────
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
