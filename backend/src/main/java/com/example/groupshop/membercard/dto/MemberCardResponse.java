package com.example.groupshop.membercard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a member card.
 *
 * <p>Shape matches {@code docs/API设计.md} — nested leader/store objects.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCardResponse {

    private Long id;
    private String levelName;
    private Integer growthValue;
    private Long totalOrderAmount;
    private Integer totalOrders;
    private String lastOrderAt;

    /** Enriched leader info */
    private LeaderInfo leader;

    /** Enriched store info */
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
        private String name;
        private String logoUrl;
    }
}
