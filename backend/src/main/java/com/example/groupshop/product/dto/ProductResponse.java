package com.example.groupshop.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a single product.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private Long storeId;
    private String name;
    private String description;
    private String coverImageUrl;
    private Long basePriceAmount;
    private Integer stock;
    private String status;
}
