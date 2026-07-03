package com.example.groupshop.subscription.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.Subscription;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.SubscriptionMapper;
import com.example.groupshop.notification.service.NotificationService;
import com.example.groupshop.subscription.dto.SubscriptionListResponse;
import com.example.groupshop.subscription.dto.SubscriptionRequest;
import com.example.groupshop.subscription.dto.SubscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for subscription management.
 *
 * <p>Batch 09: subscribe, cancel, and list subscriptions.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionMapper subscriptionMapper;
    private final LeaderMapper leaderMapper;
    private final StoreMapper storeMapper;
    private final NotificationService notificationService;

    /**
     * Subscribe to a leader.
     *
     * <p>If there's already an active subscription, returns it idempotently.
     * If there's a canceled subscription, re-activates it.
     */
    @Transactional
    public SubscriptionResponse subscribe(Long userId, Long leaderId, SubscriptionRequest request) {
        // Verify leader exists
        Leader leader = leaderMapper.selectById(leaderId);
        if (leader == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "团长不存在");
        }

        // Get store for the leader
        Store store = storeMapper.selectOne(
                new LambdaQueryWrapper<Store>()
                        .eq(Store::getLeaderId, leader.getId()));
        if (store == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "团长店铺不存在");
        }

        // Check existing subscription
        Subscription existing = subscriptionMapper.selectOne(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getUserId, userId)
                        .eq(Subscription::getLeaderId, leaderId));

        LocalDateTime now = LocalDateTime.now();

        if (existing != null) {
            if ("active".equals(existing.getStatus())) {
                // Idempotent: return existing
                return toResponse(existing);
            }
            // Re-activate canceled subscription
            existing.setStatus("active");
            existing.setSubscribedAt(now);
            existing.setCanceledAt(null);
            if (request.getSource() != null) {
                existing.setSource(request.getSource());
            }
            subscriptionMapper.updateById(existing);
            notificationService.notifySubscriptionCreated(existing, userId);
            return toResponse(existing);
        }

        // Create new subscription
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setLeaderId(leaderId);
        subscription.setStoreId(store.getId());
        subscription.setStatus("active");
        subscription.setSource(request != null ? request.getSource() : null);
        subscription.setSubscribedAt(now);
        subscription.setCanceledAt(null);
        subscriptionMapper.insert(subscription);
        notificationService.notifySubscriptionCreated(subscription, userId);

        return toResponse(subscription);
    }

    /**
     * Cancel subscription to a leader. Idempotent: if no active subscription
     * exists, returns success with null data.
     */
    @Transactional
    public void cancelSubscription(Long userId, Long leaderId) {
        Subscription existing = subscriptionMapper.selectOne(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getUserId, userId)
                        .eq(Subscription::getLeaderId, leaderId));

        if (existing == null || "canceled".equals(existing.getStatus())) {
            // Idempotent: no error
            return;
        }

        existing.setStatus("canceled");
        existing.setCanceledAt(LocalDateTime.now());
        subscriptionMapper.updateById(existing);
    }

    /**
     * List the current user's active subscriptions.
     */
    public SubscriptionListResponse listMySubscriptions(Long userId) {
        List<Subscription> subscriptions = subscriptionMapper.selectList(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getUserId, userId)
                        .eq(Subscription::getStatus, "active")
                        .orderByDesc(Subscription::getCreatedAt));

        List<SubscriptionResponse> items = subscriptions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return SubscriptionListResponse.builder()
                .items(items)
                .build();
    }

    /**
     * Check if a user has an active subscription to a leader.
     */
    public boolean isSubscribed(Long userId, Long leaderId) {
        if (userId == null) {
            return false;
        }
        Long count = subscriptionMapper.selectCount(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getUserId, userId)
                        .eq(Subscription::getLeaderId, leaderId)
                        .eq(Subscription::getStatus, "active"));
        return count != null && count > 0;
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUserId())
                .leaderId(subscription.getLeaderId())
                .storeId(subscription.getStoreId())
                .status(subscription.getStatus())
                .source(subscription.getSource())
                .subscribedAt(subscription.getSubscribedAt() != null
                        ? subscription.getSubscribedAt().toString() : null)
                .canceledAt(subscription.getCanceledAt() != null
                        ? subscription.getCanceledAt().toString() : null)
                .build();
    }
}
