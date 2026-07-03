package com.example.groupshop.subscription.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.subscription.dto.LeaderSubscriberListResponse;
import com.example.groupshop.subscription.dto.SubscriptionListResponse;
import com.example.groupshop.subscription.dto.SubscriptionRequest;
import com.example.groupshop.subscription.dto.SubscriptionResponse;
import com.example.groupshop.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Subscription controller — subscribe to a leader, cancel, and list.
 *
 * <p>Batch 09.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Subscribe to a leader.
     */
    @PostMapping("/leaders/{leaderId}/subscription")
    public ApiResponse<SubscriptionResponse> subscribe(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long leaderId,
            @Valid @RequestBody SubscriptionRequest request) {
        return ApiResponse.success(subscriptionService.subscribe(userId, leaderId, request));
    }

    /**
     * Cancel subscription to a leader.
     */
    @DeleteMapping("/leaders/{leaderId}/subscription")
    public ApiResponse<Void> cancelSubscription(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long leaderId) {
        subscriptionService.cancelSubscription(userId, leaderId);
        return ApiResponse.success();
    }

    /**
     * List current user's active subscriptions.
     */
    @GetMapping("/my/subscriptions")
    public ApiResponse<SubscriptionListResponse> listMySubscriptions(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.success(subscriptionService.listMySubscriptions(userId));
    }

    /**
     * List current leader's active subscribers.
     */
    @GetMapping("/my/store/subscribers")
    public ApiResponse<LeaderSubscriberListResponse> listMySubscribers(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.success(subscriptionService.listMySubscribers(userId));
    }
}
