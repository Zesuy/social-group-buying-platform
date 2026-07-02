package com.example.groupshop.groupbuy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Share card response — structured data for the frontend to render a share poster.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareCardResponse {

    /** The share token value */
    private String shareToken;

    /** Landing path (relative URL path, e.g. /share/group-buys/{token}) */
    private String landingPath;

    // ── Group buy info ──
    private Long groupBuyId;
    private String title;
    private String coverImageUrl;
    private Long minPriceAmount;
    private Long maxPriceAmount;
    private String endTime;
    private String groupType;

    // ── Store / leader info ──
    private Long storeId;
    private String storeName;
    private String storeLogoUrl;
    private Long leaderId;
    private String leaderName;
    private String leaderAvatarUrl;

    // ── Delivery promise ──
    private String deliveryType;
    private String shippingTime;
}
