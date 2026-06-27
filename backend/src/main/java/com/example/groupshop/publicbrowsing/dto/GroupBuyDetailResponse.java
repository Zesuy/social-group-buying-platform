package com.example.groupshop.publicbrowsing.dto;

import com.example.groupshop.groupbuy.dto.GroupBuyResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Public group buy detail response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupBuyDetailResponse {
    private GroupBuyResponse.GroupBuyData groupBuy;
    private LeaderDetail leader;
    private StoreDetail store;
    private List<GroupBuyDetailItemData> items;
    private ViewerInfo viewer;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderDetail {
        private Long id;
        private String displayName;
        private String avatarUrl;
        private Integer followerCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreDetail {
        private Long id;
        private String name;
        private String logoUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupBuyDetailItemData {
        private Long id;
        private Long productId;
        private String displayName;
        private Long groupPriceAmount;
        private Integer groupStock;
        private Integer soldCount;
        private Integer sortOrder;
        /** 商品封面，取自 products.coverImageUrl，符合 API 设计文档口径 */
        private String coverImageUrl;
    }
}
