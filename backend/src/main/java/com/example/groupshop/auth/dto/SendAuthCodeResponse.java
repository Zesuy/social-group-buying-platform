package com.example.groupshop.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body for POST /api/v1/auth/codes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendAuthCodeResponse {

    private Integer expiresInSeconds;
    private String devCode;
}
