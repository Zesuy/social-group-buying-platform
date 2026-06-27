package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Minimal OrderItem entity for group buy item protection checks.
 *
 * <p>Used to check whether a {@link GroupBuyItem} has been ordered,
 * which triggers price and deletion protection in GroupBuyService.
 */
@Data
@TableName("order_items")
public class OrderItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    /** Non-null in the database — set a dummy value for protection checks. */
    private Long productId;

    private Long groupBuyItemId;
    private Integer quantity;
}
