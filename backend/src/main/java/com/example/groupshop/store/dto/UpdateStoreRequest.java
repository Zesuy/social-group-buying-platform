package com.example.groupshop.store.dto;

import com.example.groupshop.common.enums.DeliveryType;
import jakarta.validation.constraints.Size;
import lombok.Data;

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
}
