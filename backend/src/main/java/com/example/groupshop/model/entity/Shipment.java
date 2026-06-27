package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发货记录表 — shipments
 *
 * <p>Each shipment is created when a leader ships an order.
 * An order can have multiple shipments (partial shipments).
 */
@Data
@TableName("shipments")
public class Shipment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    /** express / pickup / local_delivery */
    private String deliveryType;

    private String logisticsCompany;
    private String trackingNo;

    /** 操作人，通常为团长用户 ID */
    private Long shippedBy;

    private LocalDateTime shippedAt;
    private LocalDateTime createdAt;
}
