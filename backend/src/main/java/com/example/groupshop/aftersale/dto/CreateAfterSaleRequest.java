package com.example.groupshop.aftersale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request DTO for creating an after-sale (refund) request.
 */
@Data
public class CreateAfterSaleRequest {

    /** Currently only "refund" is supported */
    @NotBlank(message = "售后类型不能为空")
    @Pattern(regexp = "refund", message = "当前仅支持 refund 类型")
    private String type;

    @NotBlank(message = "售后原因不能为空")
    private String reason;
}
