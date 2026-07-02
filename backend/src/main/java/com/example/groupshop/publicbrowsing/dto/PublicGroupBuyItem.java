package com.example.groupshop.publicbrowsing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single item in the public group buy list response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicGroupBuyItem {
    private Long id;
    private String title;
    private String coverImageUrl;
    private String status;
    private String groupType;
    private String startTime;
    private String endTime;
    private String shippingTime;
    private Long minPriceAmount;
    private Integer soldCount;
    private LeaderLite leader;
    private StoreLite store;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderLite {
        private Long id;
        private String displayName;
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreLite {
        private Long id;
        private String name;
    }
}
