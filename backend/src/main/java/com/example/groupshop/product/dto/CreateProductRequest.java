package com.example.groupshop.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for creating a product.
 */
@Data
public class CreateProductRequest {

    @NotBlank
    @Size(min = 1, max = 128)
    private String name;

    private String description;

    private String coverImageUrl;

    /** 商品详情图片，最多 9 张；实体层校验后转为 JSON */
    private List<String> detailImageUrls;

    @NotNull
    @Min(0)
    private Long basePriceAmount;

    @NotNull
    @Min(0)
    private Integer stock;

    /** 商品分类ID，独立创建商品时必须指定 */
    @NotNull
    private Long categoryId;
}
