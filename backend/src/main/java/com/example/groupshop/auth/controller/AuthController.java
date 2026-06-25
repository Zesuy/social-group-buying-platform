package com.example.groupshop.auth.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.auth.dto.CurrentUserResponse;
import com.example.groupshop.auth.dto.MockLoginRequest;
import com.example.groupshop.auth.dto.MockLoginResponse;
import com.example.groupshop.auth.service.AuthService;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
}
