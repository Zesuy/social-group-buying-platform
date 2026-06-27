package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Minimal Order entity for group buy item protection checks.
 *
 * <p>Full Order API is implemented in a later batch.
 * This minimal entity is used only to detect whether a {@link GroupBuyItem}
 * has been referenced by a paid order, so that price/deletion protection
 * can be enforced in GroupBuyService.
 */
@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** Non-null in the database — set a dummy value for protection checks. */
    private String orderNo;

    private Long groupBuyId;
    private String payStatus;
    private String orderStatus;
}
