package com.example.groupshop.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.store.dto.CreateStoreRequest;
import com.example.groupshop.store.dto.StoreResponse;
import com.example.groupshop.store.dto.UpdateStoreRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for store management.
 */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final LeaderMapper leaderMapper;
    private final StoreMapper storeMapper;

    /**
     * Create a store for the current user.
     *
     * <p>In a transaction: creates or reuses a {@link Leader} for the user,
     * then creates a {@link Store}. New leaders get their display fields
     * from the store request. If the user already has an "orphan" leader
     * (leader without a store), it is reused and its display fields are updated.
     *
     * @throws BusinessException with {@link ErrorCode#STORE_ALREADY_EXISTS} if the user already has a store
     */
    @Transactional
    public StoreResponse createStore(Long userId, CreateStoreRequest request) {
        // Check if user already has a leader (and possibly a store)
        Leader existingLeader = leaderMapper.selectOne(
                new LambdaQueryWrapper<Leader>()
                        .eq(Leader::getUserId, userId));

        if (existingLeader != null) {
            // Check if this leader already has a store
            Store existingStore = storeMapper.selectOne(
                    new LambdaQueryWrapper<Store>()
                            .eq(Store::getLeaderId, existingLeader.getId()));
            if (existingStore != null) {
                throw new BusinessException(ErrorCode.STORE_ALREADY_EXISTS);
            }
        }

        // Create or reuse leader, syncing display fields from the request
        Leader leader;
        if (existingLeader != null) {
            leader = existingLeader;
            leader.setDisplayName(request.getName());
            if (request.getLogoUrl() != null) {
                leader.setAvatarUrl(request.getLogoUrl());
            }
            if (request.getDescription() != null) {
                leader.setBio(request.getDescription());
            }
            leaderMapper.updateById(leader);
        } else {
            leader = new Leader();
            leader.setUserId(userId);
            leader.setDisplayName(request.getName());
            leader.setAvatarUrl(request.getLogoUrl());
            leader.setBio(request.getDescription());
            leader.setServiceStatus("normal");
            leader.setMemberCount(0);
            leader.setFollowerCount(0);
            leaderMapper.insert(leader);
        }

        // Validate lat/lng pair
        if (request.getLatitude() != null || request.getLongitude() != null) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "经纬度必须同时设置");
            }
        }

        // Create store
        Store store = new Store();
        store.setLeaderId(leader.getId());
        store.setName(request.getName());
        store.setLogoUrl(request.getLogoUrl());
        store.setDescription(request.getDescription());
        store.setDefaultDeliveryType(request.getDefaultDeliveryType().getValue());
        store.setDistributionEnabled(false);
        store.setStatus("active");
        store.setLatitude(request.getLatitude());
        store.setLongitude(request.getLongitude());
        storeMapper.insert(store);

        return buildStoreResponse(leader, store);
    }

    /**
     * Get the current user's store with leader info, or {@code null} if the user has no store.
     *
     * <p>Returns both leader and store summaries per the upstream API design.
     */
    public StoreResponse getMyStore(Long userId) {
        Leader leader = leaderMapper.selectOne(
                new LambdaQueryWrapper<Leader>()
                        .eq(Leader::getUserId, userId));
        if (leader == null) {
            return null;
        }
        Store store = storeMapper.selectOne(
                new LambdaQueryWrapper<Store>()
                        .eq(Store::getLeaderId, leader.getId()));
        if (store == null) {
            return null;
        }
        return buildStoreResponse(leader, store);
    }

    /**
     * Partial update the current user's store.
     *
     * <p>Only fields that are non-null in the request are updated.
     * Store display fields (name, logo, description) are synced to the leader.
     *
     * @throws BusinessException with {@link ErrorCode#LEADER_REQUIRED} if the user has no store
     */
    @Transactional
    public StoreResponse updateMyStore(Long userId, UpdateStoreRequest request) {
        Leader leader = leaderMapper.selectOne(
                new LambdaQueryWrapper<Leader>()
                        .eq(Leader::getUserId, userId));
        if (leader == null) {
            throw new BusinessException(ErrorCode.LEADER_REQUIRED);
        }

        Store store = storeMapper.selectOne(
                new LambdaQueryWrapper<Store>()
                        .eq(Store::getLeaderId, leader.getId()));
        if (store == null) {
            throw new BusinessException(ErrorCode.LEADER_REQUIRED);
        }

        // Partial update: only update fields that are provided (non-null)
        boolean storeChanged = false;
        boolean leaderChanged = false;

        if (request.getName() != null) {
            store.setName(request.getName());
            leader.setDisplayName(request.getName());
            storeChanged = true;
            leaderChanged = true;
        }
        if (request.getLogoUrl() != null) {
            store.setLogoUrl(request.getLogoUrl());
            leader.setAvatarUrl(request.getLogoUrl());
            storeChanged = true;
            leaderChanged = true;
        }
        if (request.getDescription() != null) {
            store.setDescription(request.getDescription());
            leader.setBio(request.getDescription());
            storeChanged = true;
            leaderChanged = true;
        }
        if (request.getDefaultDeliveryType() != null) {
            store.setDefaultDeliveryType(request.getDefaultDeliveryType().getValue());
            storeChanged = true;
        }

        if (request.getLatitude() != null || request.getLongitude() != null) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "经纬度必须同时设置");
            }
            store.setLatitude(request.getLatitude());
            store.setLongitude(request.getLongitude());
            storeChanged = true;
        }

        if (storeChanged) {
            storeMapper.updateById(store);
        }
        if (leaderChanged) {
            leaderMapper.updateById(leader);
        }

        return buildStoreResponse(leader, store);
    }

    private StoreResponse buildStoreResponse(Leader leader, Store store) {
        return StoreResponse.builder()
                .leader(StoreResponse.LeaderInfo.builder()
                        .id(leader.getId())
                        .displayName(leader.getDisplayName())
                        .avatarUrl(leader.getAvatarUrl())
                        .build())
                .store(buildStoreInfo(store))
                .build();
    }

    private StoreResponse.StoreInfo buildStoreInfo(Store store) {
        return StoreResponse.StoreInfo.builder()
                .id(store.getId())
                .leaderId(store.getLeaderId())
                .name(store.getName())
                .logoUrl(store.getLogoUrl())
                .description(store.getDescription())
                .defaultDeliveryType(store.getDefaultDeliveryType())
                .distributionEnabled(store.getDistributionEnabled())
                .status(store.getStatus())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .build();
    }
}
