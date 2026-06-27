package com.example.groupshop.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for order preview.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPreviewResponse {

    private Long groupBuyId;
    private AddressPreview address;
    private List<ItemPreview> items;
    private Long totalAmount;
    private Long discountAmount;
    private Long payAmount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressPreview {
        private Long id;
        private String receiverName;
        private String receiverPhone;
        private String province;
        private String city;
        private String district;
        private String detail;
        private String fullAddress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemPreview {
        private Long groupBuyItemId;
        private Long productId;
        private String productName;
        private String skuName;
        private Long unitPriceAmount;
        private Integer quantity;
        private Long totalAmount;
        private Integer availableStock;
        private Integer soldCount;
    }
}
