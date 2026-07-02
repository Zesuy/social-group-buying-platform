package com.example.groupshop.store.dto;

import com.example.groupshop.common.enums.DeliveryType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for PATCH /api/v1/my/store — partial update a store.
 *
 * <p>All fields are nullable/optional. When a field is provided, it must pass
 * validation (e.g. non-empty string).
 */
@Data
public class UpdateStoreRequest {

    @Size(min = 1, message = "店铺名称不能为空")
    private String name;

    @Size(min = 1, message = "店铺Logo不能为空")
    private String logoUrl;

    @Size(min = 1, message = "店铺简介不能为空")
    private String description;

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
