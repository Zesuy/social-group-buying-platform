package com.example.groupshop.groupbuy.dto;

import com.example.groupshop.common.dto.ContentBlockData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for a group buy with its items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupBuyResponse {

    private GroupBuyData groupBuy;
    private List<GroupBuyItemData> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupBuyData {
        private Long id;
        private Long storeId;
        private Long leaderId;
        private String title;
        private String introduction;
        private String coverImageUrl;
        private String groupType;
        private String deliveryType;
        private String shippingTime;
        private String startTime;
        private String endTime;
        private String visibility;
        private String status;
        /** 团购活动多图 */
        private List<String> galleryImageUrls;
        /** 团购活动结构化正文块 */
        private List<ContentBlockData> contentBlocks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupBuyItemData {
        private Long id;
        private Long groupBuyId;
        private Long productId;
        private String displayName;
        private Long groupPriceAmount;
        private Integer groupStock;
        private Integer soldCount;
        private Integer sortOrder;
        /** 关联商品名称，取自 products.name */
        private String productName;
        /** 关联商品封面，取自 products.coverImageUrl */
        private String productCoverImageUrl;
        /** 关联商品基础价格，取自 products.basePriceAmount */
        private Long productBasePriceAmount;
        /** 关联商品状态，取自 products.status */
        private String productStatus;
    }
}
