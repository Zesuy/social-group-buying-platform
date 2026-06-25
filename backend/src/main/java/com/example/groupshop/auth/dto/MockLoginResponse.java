package com.example.groupshop.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body for POST /api/v1/auth/mock-login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockLoginResponse {

    private String accessToken;
    private UserSummary user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String nickname;
        private String avatarUrl;
        private String phone;
        private Boolean hasLeader;
        private Long leaderId;
        private Long storeId;
    }
}
