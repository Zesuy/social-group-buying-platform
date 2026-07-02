package com.example.groupshop.store.dto;

import com.example.groupshop.common.enums.DeliveryType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for POST /api/v1/stores — create a store.
 */
@Data
public class CreateStoreRequest {

    @NotBlank(message = "店铺名称不能为空")
    private String name;

    private String logoUrl;

    private String description;

    @NotNull(message = "默认配送方式不能为空")
    private DeliveryType defaultDeliveryType;

    /** GPS latitude (WGS84), must be paired with longitude if provided */
    @DecimalMin(value = "-90", message = "纬度不能小于 -90")
    @DecimalMax(value = "90", message = "纬度不能大于 90")
    private BigDecimal latitude;

    /** GPS longitude (WGS84), must be paired with latitude if provided */
    @DecimalMin(value = "-180", message = "经度不能小于 -180")
    @DecimalMax(value = "180", message = "经度不能大于 180")
    private BigDecimal longitude;
}
