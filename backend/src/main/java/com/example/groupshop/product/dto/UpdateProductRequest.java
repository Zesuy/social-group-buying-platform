package com.example.groupshop.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for partial-updating a product.
 *
 * <p>Only non-null fields are updated.
 */
@Data
public class UpdateProductRequest {

    @Size(min = 1, max = 128)
    private String name;

    private String description;

    private String coverImageUrl;

    /** 商品详情图片，传入非空数组时整体替换；传 [] 表示清空；不传表示不改 */
    private List<String> detailImageUrls;

    @Min(0)
    private Long basePriceAmount;

    @Min(0)
    private Integer stock;

    /** 商品分类ID，传入时校验必须为active分类 */
    private Long categoryId;
}
