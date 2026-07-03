package com.example.groupshop.publicbrowsing.dto;

import com.example.groupshop.common.dto.ProductSummaryData;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    /** 热销商品，按 soldCount 降序、sortOrder 升序、id 升序选出 */
    private GroupBuyDetailItemData featuredItem;
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
        /** GPS latitude (WGS84), nullable */
        private BigDecimal latitude;
        /** GPS longitude (WGS84), nullable */
        private BigDecimal longitude;
        /** Distance in meters from the user's location, nullable */
        private Long distanceMeters;
        /** Human-readable distance text (e.g. "800m", "1.2km"), nullable */
        private String distanceText;
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
        /** 嵌套商品信息，包含商品自己的描述和详情图 */
        private ProductSummaryData product;
    }
}
