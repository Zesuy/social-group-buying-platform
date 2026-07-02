package com.example.groupshop.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response body for POST /api/v1/stores and PATCH /api/v1/my/store.
 * Contains both leader and store summaries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {

    private LeaderInfo leader;
    private StoreInfo store;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderInfo {
        private Long id;
        private String displayName;
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreInfo {
        private Long id;
        private Long leaderId;
        private String name;
        private String logoUrl;
        private String description;
        private String defaultDeliveryType;
        private Boolean distributionEnabled;
        private String status;
        /** GPS latitude (WGS84), nullable */
        private BigDecimal latitude;
        /** GPS longitude (WGS84), nullable */
        private BigDecimal longitude;
    }
}
