package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品表 — products
 *
 * <p>Store-owned product catalog. Used as the source for group buy items.
 * Base price is for reference; the actual sale price is set per group buy item.
 * Soft-deletes use {@code status = "deleted"}.
 */
@Data
@TableName("products")
public class Product {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long storeId;
    private String name;
    private String description;
    private String coverImageUrl;

    /** 基础价格，单位分，用于参考 */
    private Long basePriceAmount;

    /** 参考库存，不参与支付扣减 */
    private Integer stock;

    /** P1 类目，关联 product_categories.id */
    private Long categoryId;

    /** 商品详情图片，JSON 字符串数组；实体层用 String 存 JSON */
    private String detailImageUrls;

    /** active / inactive / deleted */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
