package com.example.groupshop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for POST /api/v1/auth/mock-login.
 */
@Data
public class MockLoginRequest {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String nickname;
    private String avatarUrl;
}
