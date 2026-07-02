package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品分类表 — product_categories
 *
 * <p>Platform fixed categories. Only level-1 categories are initialized
 * in this batch (parentId=null, level=1).
 */
@Data
@TableName("product_categories")
public class ProductCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String name;

    /** 分类编码 */
    private String code;

    /** 父分类ID，本批 parentId=null */
    private Long parentId;

    /** 层级：1=一级 */
    private Integer level;

    /** 排序 */
    private Integer sortOrder;

    /** active / inactive */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
