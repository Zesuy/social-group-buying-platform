package com.example.groupshop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body for POST /api/v1/auth/codes.
 */
@Data
public class SendAuthCodeRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码场景不能为空")
    @Pattern(regexp = "login|register", message = "验证码场景仅支持 login 或 register")
    private String scene;
}
