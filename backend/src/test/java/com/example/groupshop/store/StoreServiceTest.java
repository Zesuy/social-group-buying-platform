package com.example.groupshop.store;

import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import com.example.groupshop.store.dto.CreateStoreRequest;
import com.example.groupshop.store.dto.StoreResponse;
import com.example.groupshop.store.dto.UpdateStoreRequest;
import com.example.groupshop.store.service.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link StoreService}.
 *
 * <p>Class-level {@link Transactional} ensures test data is rolled back after each test.
 */
@Transactional
class StoreServiceTest extends ServiceTestBase {

    @Autowired
    private StoreService storeService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaderMapper leaderMapper;

    @Autowired
    private StoreMapper storeMapper;

    private Long userId;

    @BeforeEach
    void setUp() {
        // Create a test user
        User user = new User();
        user.setNickname("测试用户");
        user.setPhone("13800009901");
        user.setStatus("normal");
        userMapper.insert(user);
        userId = user.getId();
    }

    // ── Create store ────────────────────────────────────────────────

    @Test
    void createStore_shouldCreateLeaderAndStore() {
        CreateStoreRequest request = new CreateStoreRequest();
        request.setName("测试店铺");
        request.setLogoUrl("https://example.com/logo.png");
        request.setDescription("店铺简介");
        request.setDefaultDeliveryType(com.example.groupshop.common.enums.DeliveryType.EXPRESS);

        StoreResponse response = storeService.createStore(userId, request);

        // Verify response
        assertThat(response).isNotNull();
        assertThat(response.getLeader()).isNotNull();
        assertThat(response.getLeader().getId()).isPositive();
        assertThat(response.getLeader().getDisplayName()).isEqualTo("测试店铺");
        assertThat(response.getLeader().getAvatarUrl()).isEqualTo("https://example.com/logo.png");
        assertThat(response.getStore()).isNotNull();
        assertThat(response.getStore().getId()).isPositive();
        assertThat(response.getStore().getLeaderId()).isEqualTo(response.getLeader().getId());
        assertThat(response.getStore().getName()).isEqualTo("测试店铺");
        assertThat(response.getStore().getLogoUrl()).isEqualTo("https://example.com/logo.png");
        assertThat(response.getStore().getDescription()).isEqualTo("店铺简介");
        assertThat(response.getStore().getDefaultDeliveryType()).isEqualTo("express");
        assertThat(response.getStore().getDistributionEnabled()).isFalse();
        assertThat(response.getStore().getStatus()).isEqualTo("active");

        // Verify database state
        Leader leader = leaderMapper.selectById(response.getLeader().getId());
        assertThat(leader).isNotNull();
        assertThat(leader.getUserId()).isEqualTo(userId);
        assertThat(leader.getDisplayName()).isEqualTo("测试店铺");
        assertThat(leader.getServiceStatus()).isEqualTo("normal");
        assertThat(leader.getMemberCount()).isZero();
        assertThat(leader.getFollowerCount()).isZero();

        Store store = storeMapper.selectById(response.getStore().getId());
        assertThat(store).isNotNull();
        assertThat(store.getLeaderId()).isEqualTo(leader.getId());
        assertThat(store.getDistributionEnabled()).isFalse();
        assertThat(store.getStatus()).isEqualTo("active");
    }

    @Test
    void createStore_shouldThrowWhenStoreAlreadyExists() {
        CreateStoreRequest request = new CreateStoreRequest();
        request.setName("测试店铺");
        request.setDefaultDeliveryType(com.example.groupshop.common.enums.DeliveryType.EXPRESS);

        // First create succeeds
        storeService.createStore(userId, request);

        // Second create throws
        assertThatThrownBy(() -> storeService.createStore(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.STORE_ALREADY_EXISTS.getDefaultMessage());

        // Verify only one leader and one store exist
        Leader leader = leaderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Leader>()
                        .eq(Leader::getUserId, userId));
        assertThat(leader).isNotNull();
        assertThat(storeMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Store>()
                        .eq(Store::getLeaderId, leader.getId()))).isEqualTo(1);
    }

    @Test
    void createStore_shouldReuseOrphanLeader() {
        // Create an orphan leader (no store)
        Leader orphanLeader = new Leader();
        orphanLeader.setUserId(userId);
        orphanLeader.setDisplayName("旧名称");
        orphanLeader.setServiceStatus("normal");
        orphanLeader.setMemberCount(0);
        orphanLeader.setFollowerCount(0);
        leaderMapper.insert(orphanLeader);
        Long orphanLeaderId = orphanLeader.getId();

        // Create store
        CreateStoreRequest request = new CreateStoreRequest();
        request.setName("新店铺名称");
        request.setDefaultDeliveryType(com.example.groupshop.common.enums.DeliveryType.PICKUP);

        StoreResponse response = storeService.createStore(userId, request);

        // Verify the same leader was reused
        assertThat(response.getLeader().getId()).isEqualTo(orphanLeaderId);
        assertThat(response.getLeader().getDisplayName()).isEqualTo("新店铺名称"); // updated

        // Verify only one leader exists
        assertThat(leaderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Leader>()
                        .eq(Leader::getUserId, userId))).isEqualTo(1);
    }

    // ── Get my store ────────────────────────────────────────────────

    @Test
    void getMyStore_shouldReturnNullWhenNoStore() {
        StoreResponse response = storeService.getMyStore(userId);
        assertThat(response).isNull();
    }

    @Test
    void getMyStore_shouldReturnStoreWhenExists() {
        // Create a store first
        CreateStoreRequest request = new CreateStoreRequest();
        request.setName("我的店铺");
        request.setDefaultDeliveryType(com.example.groupshop.common.enums.DeliveryType.EXPRESS);
        storeService.createStore(userId, request);

        // Get my store — returns both leader and store
        StoreResponse response = storeService.getMyStore(userId);
        assertThat(response).isNotNull();
        assertThat(response.getLeader()).isNotNull();
        assertThat(response.getLeader().getDisplayName()).isEqualTo("我的店铺");
        assertThat(response.getStore()).isNotNull();
        assertThat(response.getStore().getName()).isEqualTo("我的店铺");
        assertThat(response.getStore().getDefaultDeliveryType()).isEqualTo("express");
        assertThat(response.getStore().getStatus()).isEqualTo("active");
    }

    // ── Update store ────────────────────────────────────────────────

    @Test
    void updateMyStore_shouldPartialUpdate() {
        // Create a store first
        CreateStoreRequest createRequest = new CreateStoreRequest();
        createRequest.setName("原始名称");
        createRequest.setLogoUrl("https://example.com/old-logo.png");
        createRequest.setDescription("原始简介");
        createRequest.setDefaultDeliveryType(com.example.groupshop.common.enums.DeliveryType.EXPRESS);
        storeService.createStore(userId, createRequest);

        // Partial update: only name and description
        UpdateStoreRequest updateRequest = new UpdateStoreRequest();
        updateRequest.setName("新名称");
        updateRequest.setDescription("新简介");
        // logoUrl and defaultDeliveryType are null — should not change

        StoreResponse response = storeService.updateMyStore(userId, updateRequest);

        assertThat(response.getStore().getName()).isEqualTo("新名称");
        assertThat(response.getStore().getLogoUrl()).isEqualTo("https://example.com/old-logo.png");
        assertThat(response.getStore().getDescription()).isEqualTo("新简介");
        assertThat(response.getStore().getDefaultDeliveryType()).isEqualTo("express");
        assertThat(response.getStore().getDistributionEnabled()).isFalse();
        assertThat(response.getStore().getStatus()).isEqualTo("active");

        // Verify leader was synced
        assertThat(response.getLeader().getDisplayName()).isEqualTo("新名称");
        assertThat(response.getLeader().getAvatarUrl()).isEqualTo("https://example.com/old-logo.png");
    }

    @Test
    void updateMyStore_shouldThrowWhenNoStore() {
        UpdateStoreRequest request = new UpdateStoreRequest();
        request.setName("新名称");

        assertThatThrownBy(() -> storeService.updateMyStore(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.LEADER_REQUIRED.getDefaultMessage());
    }

    @Test
    void updateMyStore_shouldThrowWhenNoStoreButHasOrphanLeader() {
        // Create orphan leader without a store
        Leader orphanLeader = new Leader();
        orphanLeader.setUserId(userId);
        orphanLeader.setDisplayName("孤儿团长");
        orphanLeader.setServiceStatus("normal");
        orphanLeader.setMemberCount(0);
        orphanLeader.setFollowerCount(0);
        leaderMapper.insert(orphanLeader);

        UpdateStoreRequest request = new UpdateStoreRequest();
        request.setName("新名称");

        // Leader exists but has no store — should still throw LEADER_REQUIRED
        assertThatThrownBy(() -> storeService.updateMyStore(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.LEADER_REQUIRED.getDefaultMessage());
    }

    @Test
    void updateMyStore_shouldThrowWhenNoLeader() {
        // User has no leader at all
        UpdateStoreRequest request = new UpdateStoreRequest();
        request.setName("新名称");

        assertThatThrownBy(() -> storeService.updateMyStore(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.LEADER_REQUIRED.getDefaultMessage());
    }

    // ── Store location (latitude / longitude) ────────────────────────

    @Test
    void createStore_shouldSetLatitudeAndLongitude() {
        CreateStoreRequest request = new CreateStoreRequest();
        request.setName("坐标店铺");
        request.setDefaultDeliveryType(com.example.groupshop.common.enums.DeliveryType.EXPRESS);
        request.setLatitude(new java.math.BigDecimal("31.2304"));
        request.setLongitude(new java.math.BigDecimal("121.4737"));

        StoreResponse response = storeService.createStore(userId, request);

        assertThat(response.getStore().getLatitude()).isEqualByComparingTo("31.2304");
        assertThat(response.getStore().getLongitude()).isEqualByComparingTo("121.4737");
    }

    @Test
    void createStore_shouldThrowWhenOnlyLatitudeProvided() {
        CreateStoreRequest request = new CreateStoreRequest();
        request.setName("缺经度店铺");
        request.setDefaultDeliveryType(com.example.groupshop.common.enums.DeliveryType.EXPRESS);
        request.setLatitude(new java.math.BigDecimal("31.2304"));
        // longitude is null

        assertThatThrownBy(() -> storeService.createStore(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("经纬度必须同时设置");
    }

    @Test
    void createStore_shouldThrowWhenOnlyLongitudeProvided() {
        CreateStoreRequest request = new CreateStoreRequest();
        request.setName("缺纬度店铺");
        request.setDefaultDeliveryType(com.example.groupshop.common.enums.DeliveryType.EXPRESS);
        request.setLongitude(new java.math.BigDecimal("121.4737"));
        // latitude is null

        assertThatThrownBy(() -> storeService.createStore(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("经纬度必须同时设置");
    }

    @Test
    void updateMyStore_shouldSetLatitudeAndLongitude() {
        // Create store first
        CreateStoreRequest createRequest = new CreateStoreRequest();
        createRequest.setName("原始店铺");
        createRequest.setDefaultDeliveryType(com.example.groupshop.common.enums.DeliveryType.EXPRESS);
        storeService.createStore(userId, createRequest);

        // Update location
        UpdateStoreRequest updateRequest = new UpdateStoreRequest();
        updateRequest.setLatitude(new java.math.BigDecimal("30.2741"));
        updateRequest.setLongitude(new java.math.BigDecimal("120.1551"));

        StoreResponse response = storeService.updateMyStore(userId, updateRequest);

        assertThat(response.getStore().getLatitude()).isEqualByComparingTo("30.2741");
        assertThat(response.getStore().getLongitude()).isEqualByComparingTo("120.1551");
    }

    @Test
    void updateMyStore_shouldThrowWhenOnlyLatitudeProvided() {
        // Create store first
        CreateStoreRequest createRequest = new CreateStoreRequest();
        createRequest.setName("店铺");
        createRequest.setDefaultDeliveryType(com.example.groupshop.common.enums.DeliveryType.EXPRESS);
        storeService.createStore(userId, createRequest);

        // Update with only latitude — should fail
        UpdateStoreRequest updateRequest = new UpdateStoreRequest();
        updateRequest.setLatitude(new java.math.BigDecimal("31.2304"));
        // longitude is null

        assertThatThrownBy(() -> storeService.updateMyStore(userId, updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("经纬度必须同时设置");
    }

    @Test
    void updateMyStore_shouldThrowWhenOnlyLongitudeProvided() {
        // Create store first
        CreateStoreRequest createRequest = new CreateStoreRequest();
        createRequest.setName("店铺2");
        createRequest.setDefaultDeliveryType(com.example.groupshop.common.enums.DeliveryType.EXPRESS);
        storeService.createStore(userId, createRequest);

        UpdateStoreRequest updateRequest = new UpdateStoreRequest();
        updateRequest.setLongitude(new java.math.BigDecimal("121.4737"));
        // latitude is null

        assertThatThrownBy(() -> storeService.updateMyStore(userId, updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("经纬度必须同时设置");
    }
}
