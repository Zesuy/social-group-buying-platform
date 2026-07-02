package com.example.groupshop.favorite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a single favorite item with group buy summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponse {

    private Long id;
    private Long groupBuyId;
    private String title;
    private String coverImageUrl;
    private Long minPriceAmount;
    private Integer soldCount;
    private String endTime;
    private String favoritedAt;
}
