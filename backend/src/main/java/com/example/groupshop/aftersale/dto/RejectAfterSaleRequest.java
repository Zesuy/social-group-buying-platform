package com.example.groupshop.aftersale.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for rejecting an after-sale request.
 */
@Data
public class RejectAfterSaleRequest {

    @NotBlank(message = "拒绝原因不能为空")
    private String rejectReason;
}
