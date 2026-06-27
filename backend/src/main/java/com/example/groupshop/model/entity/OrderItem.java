package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单明细表 — order_items
 *
 * <p>Full OrderItem entity with product snapshots.
 */
@Data
@TableName("order_items")
public class OrderItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;
    private Long productId;
    private Long skuId;
    private Long groupBuyItemId;
    private String productName;
    private String skuName;
    private Long unitPriceAmount;
    private Integer quantity;
    private Long totalAmount;
    private LocalDateTime createdAt;
}
