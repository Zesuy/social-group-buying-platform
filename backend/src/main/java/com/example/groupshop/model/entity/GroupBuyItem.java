package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 团购商品关系表 — group_buy_items
 *
 * <p>Each item links a {@link Product} to a {@link GroupBuy} with a
 * specific group price and stock. {@code groupStock} is the remaining
 * sellable stock and must never be negative. {@code soldCount} tracks
 * paid sales.
 */
@Data
@TableName("group_buy_items")
public class GroupBuyItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long groupBuyId;
    private Long productId;

    /** MVP 不使用 */
    private Long skuId;

    /** 团购中展示的商品名 */
    private String displayName;

    /** 团购价，单位分，下单价格以此为准 */
    private Long groupPriceAmount;

    /** 当前剩余可售库存，必须 >= 0 */
    private Integer groupStock;

    /** 已支付售出数量 */
    private Integer soldCount;

    private Integer sortOrder;

    /** P1 使用 */
    private Boolean showStock;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
