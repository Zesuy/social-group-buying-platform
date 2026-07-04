package com.example.groupshop.auth.service;

import com.example.groupshop.auth.TokenStore;
import com.example.groupshop.auth.dto.CurrentUserResponse;
import com.example.groupshop.auth.dto.MockLoginRequest;
import com.example.groupshop.auth.dto.MockLoginResponse;
import com.example.groupshop.auth.dto.PhoneCodeLoginRequest;
import com.example.groupshop.auth.dto.PhoneCodeRegisterRequest;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import com.example.groupshop.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.groupshop.common.enums.ErrorCode.RESOURCE_CONFLICT;
import static com.example.groupshop.common.enums.ErrorCode.RESOURCE_NOT_FOUND;

/**
 * Service for MVP authentication: mock login and current user resolution.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TokenStore tokenStore;
    private final AuthCodeService authCodeService;
    private final UserMapper userMapper;
    private final LeaderMapper leaderMapper;
    private final StoreMapper storeMapper;

    /**
     * Mock login: find or create a user by phone, generate a token.
     */
    @Transactional
    public MockLoginResponse mockLogin(MockLoginRequest request) {
        User user = findUserByPhone(request.getPhone());

        if (user == null) {
            user = new User();
            user.setPhone(request.getPhone());
            user.setNickname(request.getNickname() != null ? request.getNickname() : "用户" + request.getPhone().substring(Math.max(0, request.getPhone().length() - 4)));
            user.setAvatarUrl(request.getAvatarUrl());
            user.setStatus("normal");
            userMapper.insert(user);
        }

        return buildLoginResponse(user);
    }

    /**
     * Phone-code login: requires an existing user.
     */
    public MockLoginResponse loginWithCode(PhoneCodeLoginRequest request) {
        authCodeService.verify(request.getPhone(), "login", request.getCode());
        User user = findUserByPhone(request.getPhone());
        if (user == null) {
            throw new BusinessException(RESOURCE_NOT_FOUND, "该手机号尚未注册，请先注册");
        }
        return buildLoginResponse(user);
    }

    /**
     * Phone-code registration: creates a new user and returns a token.
     */
    @Transactional
    public MockLoginResponse registerWithCode(PhoneCodeRegisterRequest request) {
        authCodeService.verify(request.getPhone(), "register", request.getCode());
        User existing = findUserByPhone(request.getPhone());
        if (existing != null) {
            throw new BusinessException(RESOURCE_CONFLICT, "该手机号已注册，请直接登录");
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname().trim());
        user.setAvatarUrl(null);
        user.setStatus("normal");
        userMapper.insert(user);

        return buildLoginResponse(user);
    }

    private User findUserByPhone(String phone) {
        return userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone));
    }

    private MockLoginResponse buildLoginResponse(User user) {
        String token = tokenStore.createToken(user.getId());
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
