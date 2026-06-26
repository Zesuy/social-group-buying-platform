package com.example.groupshop.auth.service;

import com.example.groupshop.auth.TokenStore;
import com.example.groupshop.auth.dto.CurrentUserResponse;
import com.example.groupshop.auth.dto.MockLoginRequest;
import com.example.groupshop.auth.dto.MockLoginResponse;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for MVP authentication: mock login and current user resolution.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TokenStore tokenStore;
    private final UserMapper userMapper;
    private final LeaderMapper leaderMapper;
    private final StoreMapper storeMapper;

    /**
     * Mock login: find or create a user by phone, generate a token.
     */
    @Transactional
    public MockLoginResponse mockLogin(MockLoginRequest request) {
        // Find existing user by phone
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getPhone, request.getPhone()));

        if (user == null) {
            // Create a new user
            user = new User();
            user.setPhone(request.getPhone());
            user.setNickname(request.getNickname() != null ? request.getNickname() : "用户" + request.getPhone().substring(Math.max(0, request.getPhone().length() - 4)));
            user.setAvatarUrl(request.getAvatarUrl());
            user.setStatus("normal");
            userMapper.insert(user);
        }

        // Generate token
        String token = tokenStore.createToken(user.getId());

        // Check leader/store status
        boolean hasLeader = false;
        Long leaderId = null;
        Long storeId = null;
        Leader leader = leaderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Leader>()
                        .eq(Leader::getUserId, user.getId()));
        if (leader != null) {
            hasLeader = true;
            leaderId = leader.getId();
            Store store = storeMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Store>()
                            .eq(Store::getLeaderId, leader.getId()));
            if (store != null) {
                storeId = store.getId();
            }
        }

        return MockLoginResponse.builder()
                .accessToken(token)
                .user(MockLoginResponse.UserSummary.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .avatarUrl(user.getAvatarUrl())
                        .phone(user.getPhone())
                        .hasLeader(hasLeader)
                        .leaderId(leaderId)
                        .storeId(storeId)
                        .build())
                .build();
    }

    /**
     * Resolve the current user context for GET /api/v1/me.
     */
    public CurrentUserResponse getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        // Check leader/store status
        boolean hasLeader = false;
        Long leaderId = null;
        Long storeId = null;

        CurrentUserResponse.CurrentUserResponseBuilder builder = CurrentUserResponse.builder();

        // Load leader if exists
        Leader leader = leaderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Leader>()
                        .eq(Leader::getUserId, userId));
        if (leader != null) {
            hasLeader = true;
            leaderId = leader.getId();
            builder.leader(CurrentUserResponse.LeaderSummary.builder()
                    .id(leader.getId())
                    .displayName(leader.getDisplayName())
                    .avatarUrl(leader.getAvatarUrl())
                    .bio(leader.getBio())
                    .memberCount(leader.getMemberCount())
                    .followerCount(leader.getFollowerCount())
                    .build());

            // Load store if exists
            Store store = storeMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Store>()
                            .eq(Store::getLeaderId, leader.getId()));
            if (store != null) {
                storeId = store.getId();
                builder.store(CurrentUserResponse.StoreSummary.builder()
                        .id(store.getId())
                        .name(store.getName())
                        .logoUrl(store.getLogoUrl())
                        .description(store.getDescription())
                        .defaultDeliveryType(store.getDefaultDeliveryType())
                        .status(store.getStatus())
                        .build());
            }
        }

        // Rebuild user summary with leader/store info
        builder.user(CurrentUserResponse.UserSummary.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .hasLeader(hasLeader)
                .leaderId(leaderId)
                .storeId(storeId)
                .build());

        return builder.build();
    }
}
