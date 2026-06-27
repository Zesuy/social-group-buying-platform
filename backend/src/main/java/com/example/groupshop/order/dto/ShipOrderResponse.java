package com.example.groupshop.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for shipping an order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipOrderResponse {

    private OrderResponse order;
    private ShipmentData shipment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipmentData {
        private Long id;
        private Long orderId;
        private String deliveryType;
        private String logisticsCompany;
        private String trackingNo;
        private String shippedAt;
    }
}
