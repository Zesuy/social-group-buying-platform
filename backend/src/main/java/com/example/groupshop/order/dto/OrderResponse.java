package com.example.groupshop.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNo;
    private Long groupBuyId;
    private Long storeId;
    private Long leaderId;
    private Long totalAmount;
    private Long discountAmount;
    private Long payAmount;
    private String payStatus;
    private String orderStatus;
    private String paidAt;
    private String shippedAt;
    private String remark;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private String fullAddress;
    private List<OrderItemData> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemData {
        private Long id;
        private Long groupBuyItemId;
        private Long productId;
        private String productName;
        private String skuName;
        private Long unitPriceAmount;
        private Integer quantity;
        private Long totalAmount;
    }
}
