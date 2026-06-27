package com.example.groupshop.leader.dto;

import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.publicbrowsing.dto.PublicGroupBuyItem;
import com.example.groupshop.publicbrowsing.dto.ViewerInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Leader homepage response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderHomepageResponse {
    private LeaderInfo leader;
    private StoreInfo store;
    private ViewerInfo viewer;
    private PageResponse<PublicGroupBuyItem> groupBuys;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderInfo {
        private Long id;
        private String displayName;
        private String avatarUrl;
        private String bio;
        private Integer memberCount;
        private Integer followerCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreInfo {
        private Long id;
        private String name;
        private String logoUrl;
        private String description;
        private String defaultDeliveryType;
    }
}
