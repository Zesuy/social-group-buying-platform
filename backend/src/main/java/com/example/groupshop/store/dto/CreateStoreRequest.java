package com.example.groupshop.store.dto;

import com.example.groupshop.common.enums.DeliveryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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
}
