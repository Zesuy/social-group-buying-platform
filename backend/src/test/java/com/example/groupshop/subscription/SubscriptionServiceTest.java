package com.example.groupshop.subscription;

import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.Subscription;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.SubscriptionMapper;
import com.example.groupshop.model.mapper.UserMapper;
import com.example.groupshop.subscription.dto.SubscriptionListResponse;
import com.example.groupshop.subscription.dto.SubscriptionRequest;
import com.example.groupshop.subscription.dto.SubscriptionResponse;
import com.example.groupshop.subscription.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link SubscriptionService}.
 */
@Transactional
class SubscriptionServiceTest extends ServiceTestBase {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionMapper subscriptionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaderMapper leaderMapper;

    @Autowired
    private StoreMapper storeMapper;

    private Long leaderId;
    private Long userId;
    private Long storeId;

    @BeforeEach
    void setUp() {
        // Set up leader
        User leaderUser = new User();
        leaderUser.setNickname("团长");
        leaderUser.setPhone("13800009906");
        leaderUser.setStatus("normal");
        userMapper.insert(leaderUser);

        Leader leader = new Leader();
        leader.setUserId(leaderUser.getId());
        leader.setDisplayName("团长");
        leader.setAvatarUrl("https://static.example.com/leader.png");
        leader.setServiceStatus("normal");
        leader.setMemberCount(0);
        leader.setFollowerCount(0);
        leaderMapper.insert(leader);
        leaderId = leader.getId();

        Store store = new Store();
        store.setLeaderId(leader.getId());
        store.setName("店铺");
        store.setLogoUrl("https://static.example.com/store.png");
        store.setDefaultDeliveryType("express");
        store.setDistributionEnabled(false);
        store.setStatus("active");
        storeMapper.insert(store);
        storeId = store.getId();

        // Set up user
        User user = new User();
        user.setNickname("用户");
        user.setPhone("13800009907");
        user.setStatus("normal");
        userMapper.insert(user);
        userId = user.getId();
    }

    @Test
    void subscribe_shouldSucceed() {
        SubscriptionRequest request = SubscriptionRequest.builder()
                .source("homepage")
                .build();

        SubscriptionResponse response = subscriptionService.subscribe(userId, leaderId, request);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getLeaderId()).isEqualTo(leaderId);
        assertThat(response.getStoreId()).isEqualTo(storeId);
        assertThat(response.getStatus()).isEqualTo("active");
        assertThat(response.getSource()).isEqualTo("homepage");
        assertThat(response.getSubscribedAt()).isNotNull();
        assertThat(response.getLeader()).isNotNull();
        assertThat(response.getLeader().getDisplayName()).isEqualTo("团长");
        assertThat(response.getLeader().getAvatarUrl()).isEqualTo("https://static.example.com/leader.png");
        assertThat(response.getStore()).isNotNull();
        assertThat(response.getStore().getName()).isEqualTo("店铺");
        assertThat(response.getStore().getLogoUrl()).isEqualTo("https://static.example.com/store.png");
    }

    @Test
    void subscribe_shouldBeIdempotent() {
        SubscriptionResponse first = subscriptionService.subscribe(userId, leaderId,
                SubscriptionRequest.builder().source("homepage").build());

        SubscriptionResponse second = subscriptionService.subscribe(userId, leaderId,
                SubscriptionRequest.builder().source("product_detail").build());

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getStatus()).isEqualTo("active");
        // Source should remain from the first subscription (not overwritten)
    }

    @Test
    void subscribe_shouldReactivateAfterCancel() {
        subscriptionService.subscribe(userId, leaderId,
                SubscriptionRequest.builder().source("homepage").build());
        subscriptionService.cancelSubscription(userId, leaderId);

        SubscriptionResponse reactivated = subscriptionService.subscribe(userId, leaderId,
                SubscriptionRequest.builder().source("invitation").build());

        assertThat(reactivated.getStatus()).isEqualTo("active");
        assertThat(reactivated.getSource()).isEqualTo("invitation");
    }

    @Test
    void subscribe_shouldFailWhenLeaderNotFound() {
        assertThatThrownBy(() -> subscriptionService.subscribe(userId, 99999L,
                SubscriptionRequest.builder().build()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void cancelSubscription_shouldSucceed() {
        subscriptionService.subscribe(userId, leaderId,
                SubscriptionRequest.builder().source("homepage").build());

        subscriptionService.cancelSubscription(userId, leaderId);

        Subscription sub = subscriptionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getUserId, userId)
                        .eq(Subscription::getLeaderId, leaderId));
        assertThat(sub.getStatus()).isEqualTo("canceled");
        assertThat(sub.getCanceledAt()).isNotNull();
    }

    @Test
    void cancelSubscription_shouldBeIdempotent() {
        // Cancel when no subscription exists — should not throw
        subscriptionService.cancelSubscription(userId, leaderId);

        // Cancel when already canceled — should not throw
        subscriptionService.cancelSubscription(userId, leaderId);
    }

    @Test
    void listMySubscriptions_shouldReturnActiveOnly() {
        // Create a second leader
        User leaderUser2 = new User();
        leaderUser2.setNickname("团长2");
        leaderUser2.setPhone("13800009908");
        leaderUser2.setStatus("normal");
        userMapper.insert(leaderUser2);

        Leader leader2 = new Leader();
        leader2.setUserId(leaderUser2.getId());
        leader2.setDisplayName("团长2");
        leader2.setServiceStatus("normal");
        leader2.setMemberCount(0);
        leader2.setFollowerCount(0);
        leaderMapper.insert(leader2);

        Store store2 = new Store();
        store2.setLeaderId(leader2.getId());
        store2.setName("店铺2");
        store2.setDefaultDeliveryType("express");
        store2.setDistributionEnabled(false);
        store2.setStatus("active");
        storeMapper.insert(store2);

        // Subscribe to first and cancel
        subscriptionService.subscribe(userId, leaderId,
                SubscriptionRequest.builder().build());
        subscriptionService.cancelSubscription(userId, leaderId);

        // Subscribe to second (active)
        subscriptionService.subscribe(userId, leader2.getId(),
                SubscriptionRequest.builder().build());

        SubscriptionListResponse result = subscriptionService.listMySubscriptions(userId);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getLeaderId()).isEqualTo(leader2.getId());
        assertThat(result.getItems().get(0).getLeader()).isNotNull();
        assertThat(result.getItems().get(0).getLeader().getDisplayName()).isEqualTo("团长2");
        assertThat(result.getItems().get(0).getStore()).isNotNull();
        assertThat(result.getItems().get(0).getStore().getName()).isEqualTo("店铺2");
    }

    @Test
    void isSubscribed_shouldReturnTrueWhenActive() {
        subscriptionService.subscribe(userId, leaderId,
                SubscriptionRequest.builder().build());

        assertThat(subscriptionService.isSubscribed(userId, leaderId)).isTrue();
    }

    @Test
    void isSubscribed_shouldReturnFalseWhenNot() {
        assertThat(subscriptionService.isSubscribed(userId, leaderId)).isFalse();
    }

    @Test
    void isSubscribed_shouldReturnFalseWhenCanceled() {
        subscriptionService.subscribe(userId, leaderId,
                SubscriptionRequest.builder().build());
        subscriptionService.cancelSubscription(userId, leaderId);

        assertThat(subscriptionService.isSubscribed(userId, leaderId)).isFalse();
    }

    @Test
    void isSubscribed_shouldReturnFalseForNullUserId() {
        assertThat(subscriptionService.isSubscribed(null, leaderId)).isFalse();
    }
}
