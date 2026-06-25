package com.example.groupshop.auth;

import com.example.groupshop.auth.dto.CurrentUserResponse;
import com.example.groupshop.auth.dto.MockLoginRequest;
import com.example.groupshop.auth.dto.MockLoginResponse;
import com.example.groupshop.auth.service.AuthService;
import com.example.groupshop.base.ServiceTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTest extends ServiceTestBase {

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenStore tokenStore;

    // ── Mock login ───────────────────────────────────────────────────

    @Test
    void mockLogin_shouldCreateUser() {
        MockLoginRequest request = new MockLoginRequest();
        request.setPhone("13800001001");
        request.setNickname("新用户");

        MockLoginResponse response = authService.mockLogin(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isPositive();
        assertThat(response.getUser().getNickname()).isEqualTo("新用户");
        assertThat(response.getUser().getPhone()).isEqualTo("13800001001");
        assertThat(response.getUser().getHasLeader()).isFalse();
    }

    @Test
    void mockLogin_shouldReturnExistingUser() {
        // First call creates
        MockLoginRequest request = new MockLoginRequest();
        request.setPhone("13800001002");
        request.setNickname("重复用户");
        MockLoginResponse first = authService.mockLogin(request);

        // Second call with same phone returns the same user
        MockLoginRequest request2 = new MockLoginRequest();
        request2.setPhone("13800001002");
        MockLoginResponse second = authService.mockLogin(request2);

        assertThat(second.getUser().getId()).isEqualTo(first.getUser().getId());
        assertThat(second.getUser().getNickname()).isEqualTo("重复用户");
    }

    @Test
    void mockLogin_shouldGenerateDefaultNickname() {
        MockLoginRequest request = new MockLoginRequest();
        request.setPhone("13800001003");

        MockLoginResponse response = authService.mockLogin(request);
        assertThat(response.getUser().getNickname()).isEqualTo("用户1003");
    }

    // ── Token store ──────────────────────────────────────────────────

    @Test
    void tokenStore_shouldRoundTrip() {
        Long userId = 999L;
        String token = tokenStore.createToken(userId);

        assertThat(token).isNotBlank();
        assertThat(tokenStore.resolveUserId(token)).isEqualTo(userId);
        assertThat(tokenStore.resolveUserId("nonexistent")).isNull();
    }

    // ── Get current user ─────────────────────────────────────────────

    @Test
    void getCurrentUser_shouldReturnUserWithoutLeader() {
        // First create a user
        MockLoginRequest request = new MockLoginRequest();
        request.setPhone("13800002001");
        request.setNickname("无店铺用户");
        MockLoginResponse login = authService.mockLogin(request);

        CurrentUserResponse current = authService.getCurrentUser(login.getUser().getId());

        assertThat(current).isNotNull();
        assertThat(current.getUser()).isNotNull();
        assertThat(current.getUser().getId()).isEqualTo(login.getUser().getId());
        assertThat(current.getUser().getNickname()).isEqualTo("无店铺用户");
        assertThat(current.getLeader()).isNull();
        assertThat(current.getStore()).isNull();
    }

    @Test
    void getCurrentUser_shouldReturnNullForUnknownUser() {
        CurrentUserResponse current = authService.getCurrentUser(-1L);
        assertThat(current).isNull();
    }
}
