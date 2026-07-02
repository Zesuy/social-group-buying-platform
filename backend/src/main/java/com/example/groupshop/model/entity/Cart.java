package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物车表 — carts
 *
 * <p>Stores cart items for buyers. Each cart item links a user to a
 * group buy item with a quantity. For hidden group buys, the
 * {@code shareTokenId} is saved at add time so subsequent cart operations
 * (preview, checkout) can reuse the same token without requiring it again.
 *
 * <p>Unique constraint on {@code (userId, groupBuyItemId)} ensures
 * repeated add-to-cart for the same item merges quantities.
 */
@Data
@TableName("carts")
public class Cart {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Long groupBuyId;
    private Long groupBuyItemId;
    private Integer quantity;

    /** Saved share token ID for hidden group buy access in subsequent operations */
    private Long shareTokenId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
