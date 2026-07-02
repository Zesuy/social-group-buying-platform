package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 售后表 — after_sales
 *
 * <p>Represents a buyer's after-sale (refund) request.
 * Status flow: pending → approved → completed (refund done)
 *             pending → rejected
 */
@Data
@TableName("after_sales")
public class AfterSale {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;
    private Long userId;
    private Long leaderId;
    private Long storeId;

    /** refund / return_refund (currently only refund) */
    private String type;

    private String reason;

    /** pending / approved / rejected / completed */
    private String status;

    /** Refund amount in fen */
    private Long amount;

    /** Snapshot of the order's DB orderStatus at application time */
    private String originalOrderStatus;

    private String rejectReason;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
