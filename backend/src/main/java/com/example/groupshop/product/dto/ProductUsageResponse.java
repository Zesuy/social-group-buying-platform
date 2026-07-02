package com.example.groupshop.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a single product usage record in a group buy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUsageResponse {

    private Long groupBuyId;
    private String title;
    private String status;
    private Long itemId;
    private String displayName;
    private Long groupPriceAmount;
    private Integer groupStock;
    private Integer soldCount;
    private String startAt;
    private String endAt;
    private String createdAt;
}
