package com.example.groupshop.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for listing subscriptions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionListResponse {

    private List<SubscriptionResponse> items;
}
