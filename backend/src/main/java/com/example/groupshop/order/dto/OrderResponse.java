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
    private String completedAt;
    private String remark;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private String fullAddress;
    private List<OrderItemData> items;

    /** Coupon snapshot */
    private Long userCouponId;
    private Long couponId;
    private String couponName;
    private String couponType;

    /** After-sale summary (nullable — only present when an after-sale exists) */
    private AfterSaleSummary afterSale;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AfterSaleSummary {
        private Long id;
        private String type;
        private String status;
        private Long amount;
        private String reason;
        private String rejectReason;
        private String createdAt;
        private String approvedAt;
        private String rejectedAt;
        private String completedAt;
    }

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
