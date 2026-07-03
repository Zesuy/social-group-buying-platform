package com.example.groupshop.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for leader subscriber list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderSubscriberListResponse {

    private List<LeaderSubscriberResponse> items;
    private long total;
}
