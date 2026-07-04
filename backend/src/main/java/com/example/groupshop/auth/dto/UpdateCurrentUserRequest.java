package com.example.groupshop.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for PATCH /api/v1/me.
 */
@Data
public class UpdateCurrentUserRequest {

    @Size(max = 50, message = "昵称不能超过 50 个字符")
    private String nickname;

    @Size(max = 500, message = "头像 URL 不能超过 500 个字符")
    private String avatarUrl;
}
