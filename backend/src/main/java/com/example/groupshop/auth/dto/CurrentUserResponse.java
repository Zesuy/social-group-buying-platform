package com.example.groupshop.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body for GET /api/v1/me — current user context.
 *
 * <p>{@code leader} and {@code store} are null when the user has not created a store.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserResponse {

    private UserSummary user;
    private LeaderSummary leader;
    private StoreSummary store;

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderSummary {
        private Long id;
        private String displayName;
        private String avatarUrl;
        private String bio;
        private Integer memberCount;
        private Integer followerCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreSummary {
        private Long id;
        private String name;
        private String logoUrl;
        private String description;
        private String defaultDeliveryType;
    }
}
