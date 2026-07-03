package com.example.groupshop.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Summary of a Product for nested display inside a group buy detail item.
 *
 * <p>Contains the product's own description and detail images, separate from
 * the group buy activity content.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryData {

    private Long id;
    private String name;
    private String description;
    private String coverImageUrl;
    private List<String> detailImageUrls;
    private Long basePriceAmount;
    private String status;
}
