package com.example.groupshop.order.dto;

import com.example.groupshop.common.enums.DeliveryType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for shipping an order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipOrderRequest {

    /** express / pickup / local_delivery — validated against DeliveryType enum */
    @NotNull(message = "配送方式不能为空")
    private DeliveryType deliveryType;

    /** 物流公司，配送方式为 express 时建议填写 */
    private String logisticsCompany;

    /** 运单号，配送方式为 express 时建议填写 */
    private String trackingNo;
}
