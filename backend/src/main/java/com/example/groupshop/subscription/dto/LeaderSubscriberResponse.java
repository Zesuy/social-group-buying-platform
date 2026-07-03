package com.example.groupshop.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subscriber visible to a leader.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderSubscriberResponse {

    private Long subscriptionId;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String source;
    private String subscribedAt;
}
