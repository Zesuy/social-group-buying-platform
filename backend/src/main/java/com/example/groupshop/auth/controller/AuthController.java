package com.example.groupshop.auth.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.auth.dto.CurrentUserResponse;
import com.example.groupshop.auth.dto.MockLoginRequest;
import com.example.groupshop.auth.dto.MockLoginResponse;
import com.example.groupshop.auth.dto.PhoneCodeLoginRequest;
import com.example.groupshop.auth.dto.PhoneCodeRegisterRequest;
import com.example.groupshop.auth.dto.SendAuthCodeRequest;
import com.example.groupshop.auth.dto.SendAuthCodeResponse;
import com.example.groupshop.auth.dto.UpdateCurrentUserRequest;
import com.example.groupshop.auth.service.AuthCodeService;
import com.example.groupshop.auth.service.AuthService;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.groupshop.common.enums.ErrorCode.UNAUTHORIZED;

/**
 * Auth controller: mock login and current user.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCodeService authCodeService;

    /**
     * Send a demo verification code for login or registration.
     */
    @PostMapping("/auth/codes")
    public ApiResponse<SendAuthCodeResponse> sendAuthCode(@Valid @RequestBody SendAuthCodeRequest request) {
        SendAuthCodeResponse response = authCodeService.sendCode(request);
        return ApiResponse.success(response);
    }

    /**
     * Login with phone + verification code.
     */
    @PostMapping("/auth/login")
    public ApiResponse<MockLoginResponse> login(@Valid @RequestBody PhoneCodeLoginRequest request) {
        MockLoginResponse response = authService.loginWithCode(request);
        return ApiResponse.success(response);
    }

    /**
     * Register with phone + verification code + profile summary.
     */
    @PostMapping("/auth/register")
    public ApiResponse<MockLoginResponse> register(@Valid @RequestBody PhoneCodeRegisterRequest request) {
        MockLoginResponse response = authService.registerWithCode(request);
        return ApiResponse.success(response);
    }

    /**
     * Mock login: accepts a phone number, finds or creates a user, and returns an access token.
     */
    @PostMapping("/auth/mock-login")
    public ApiResponse<MockLoginResponse> mockLogin(@Valid @RequestBody MockLoginRequest request) {
        MockLoginResponse response = authService.mockLogin(request);
        return ApiResponse.success(response);
    }

    /**
     * Get the current user context (user, leader, store summaries).
     * Requires a valid Bearer token.
     */
    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> getCurrentUser(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        CurrentUserResponse response = authService.getCurrentUser(userId);
        if (response == null) {
            throw new BusinessException(UNAUTHORIZED, "用户不存在");
        }
        return ApiResponse.success(response);
    }

    /**
     * Update the current user's profile summary.
     */
    @PatchMapping("/me")
    public ApiResponse<CurrentUserResponse> updateCurrentUser(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody UpdateCurrentUserRequest request) {
        return ApiResponse.success(authService.updateCurrentUser(userId, request));
    }
}
