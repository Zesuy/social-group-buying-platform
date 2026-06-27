package com.example.groupshop.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a single subscription.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private Long id;
    private Long userId;
    private Long leaderId;
    private Long storeId;
    private String status;
    private String source;
    private String subscribedAt;
    private String canceledAt;
}
