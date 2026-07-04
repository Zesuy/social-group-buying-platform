package com.example.groupshop.auth;

import com.example.groupshop.auth.dto.CurrentUserResponse;
import com.example.groupshop.auth.dto.MockLoginRequest;
import com.example.groupshop.auth.dto.MockLoginResponse;
import com.example.groupshop.auth.dto.PhoneCodeLoginRequest;
import com.example.groupshop.auth.dto.PhoneCodeRegisterRequest;
import com.example.groupshop.auth.dto.SendAuthCodeRequest;
import com.example.groupshop.auth.service.AuthService;
import com.example.groupshop.auth.service.AuthCodeService;
import com.example.groupshop.base.ServiceTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest extends ServiceTestBase {

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private AuthCodeService authCodeService;

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

    // ── Phone-code login / register ───────────────────────────────────

    @Test
    void sendCode_shouldReturnDemoCode() {
        SendAuthCodeRequest request = new SendAuthCodeRequest();
        request.setPhone("13800003001");
        request.setScene("register");

        var response = authCodeService.sendCode(request);

        assertThat(response.getExpiresInSeconds()).isEqualTo(300);
        assertThat(response.getDevCode()).isEqualTo("123456");
    }

    @Test
    void registerWithCode_shouldCreateUserAndReturnToken() {
        sendCode("13800003002", "register");

        PhoneCodeRegisterRequest request = new PhoneCodeRegisterRequest();
        request.setPhone("13800003002");
        request.setCode("123456");
        request.setNickname("验证码用户");

        MockLoginResponse response = authService.registerWithCode(request);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getUser().getPhone()).isEqualTo("13800003002");
        assertThat(response.getUser().getNickname()).isEqualTo("验证码用户");
        assertThat(response.getUser().getHasLeader()).isFalse();
    }

    @Test
    void registerWithCode_shouldRejectExistingPhone() {
        MockLoginRequest existing = new MockLoginRequest();
        existing.setPhone("13800003003");
        existing.setNickname("已有用户");
        authService.mockLogin(existing);
        sendCode("13800003003", "register");

        PhoneCodeRegisterRequest request = new PhoneCodeRegisterRequest();
        request.setPhone("13800003003");
        request.setCode("123456");
        request.setNickname("重复注册");

        assertThatThrownBy(() -> authService.registerWithCode(request))
                .hasMessageContaining("该手机号已注册");
    }

    @Test
    void loginWithCode_shouldReturnExistingUser() {
        MockLoginRequest existing = new MockLoginRequest();
        existing.setPhone("13800003004");
        existing.setNickname("登录用户");
        authService.mockLogin(existing);
        sendCode("13800003004", "login");

        PhoneCodeLoginRequest request = new PhoneCodeLoginRequest();
        request.setPhone("13800003004");
        request.setCode("123456");

        MockLoginResponse response = authService.loginWithCode(request);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getUser().getNickname()).isEqualTo("登录用户");
        assertThat(response.getUser().getPhone()).isEqualTo("13800003004");
    }

    @Test
    void loginWithCode_shouldRejectUnregisteredPhone() {
        sendCode("13800003005", "login");

        PhoneCodeLoginRequest request = new PhoneCodeLoginRequest();
        request.setPhone("13800003005");
        request.setCode("123456");

        assertThatThrownBy(() -> authService.loginWithCode(request))
                .hasMessageContaining("该手机号尚未注册");
    }

    @Test
    void loginWithCode_shouldRejectWrongCode() {
        sendCode("13800003006", "login");

        PhoneCodeLoginRequest request = new PhoneCodeLoginRequest();
        request.setPhone("13800003006");
        request.setCode("000000");

        assertThatThrownBy(() -> authService.loginWithCode(request))
                .hasMessageContaining("验证码不正确");
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

    private void sendCode(String phone, String scene) {
        SendAuthCodeRequest request = new SendAuthCodeRequest();
        request.setPhone(phone);
        request.setScene(scene);
        authCodeService.sendCode(request);
    }
}
