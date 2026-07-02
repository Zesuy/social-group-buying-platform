package com.example.groupshop.browsing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a single browsing history entry with group buy summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowsingHistoryResponse {

    private Long id;
    private Long groupBuyId;
    private String title;
    private String coverImageUrl;
    private String viewedAt;
}
