package com.example.groupshop.auth;

import com.example.groupshop.auth.dto.MockLoginResponse;
import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String AUTH_CODES_URL = "/api/v1/auth/codes";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String ME_URL = "/api/v1/me";

    // ── POST /api/v1/auth/codes ──────────────────────────────────────

    @Test
    void sendAuthCode_shouldReturnDemoCode() throws Exception {
        mockMvc.perform(post(AUTH_CODES_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "phone": "13800004001",
                                    "scene": "register"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.expiresInSeconds").value(300))
                .andExpect(jsonPath("$.data.devCode").value("123456"));
    }

    @Test
    void sendAuthCode_shouldFailWhenSceneInvalid() throws Exception {
        mockMvc.perform(post(AUTH_CODES_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "phone": "13800004002",
                                    "scene": "reset"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    // ── POST /api/v1/auth/register ───────────────────────────────────

    @Test
    void register_shouldCreateUserAndReturnToken() throws Exception {
        sendCode("13800004003", "register");

        mockMvc.perform(post(REGISTER_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "phone": "13800004003",
                                    "code": "123456",
                                    "nickname": "注册用户"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.phone").value("13800004003"))
                .andExpect(jsonPath("$.data.user.nickname").value("注册用户"));
    }

    @Test
    void register_shouldFailWhenCodeIsWrong() throws Exception {
        sendCode("13800004004", "register");

        mockMvc.perform(post(REGISTER_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "phone": "13800004004",
                                    "code": "000000",
                                    "nickname": "验证码错误"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpectAll(errorResult("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void register_shouldFailWhenPhoneExists() throws Exception {
        mockMvc.perform(post(MOCK_LOGIN_URL)
                .contentType("application/json")
                .content("""
                        {
                            "phone": "13800004005",
                            "nickname": "已有用户"
                        }
                        """));
        sendCode("13800004005", "register");

        mockMvc.perform(post(REGISTER_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "phone": "13800004005",
                                    "code": "123456",
                                    "nickname": "重复用户"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpectAll(errorResult("RESOURCE_CONFLICT"));
    }

    // ── POST /api/v1/auth/login ──────────────────────────────────────

    @Test
    void login_shouldReturnExistingUser() throws Exception {
        mockMvc.perform(post(MOCK_LOGIN_URL)
                .contentType("application/json")
                .content("""
                        {
                            "phone": "13800004006",
                            "nickname": "验证码登录用户"
                        }
                        """));
        sendCode("13800004006", "login");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "phone": "13800004006",
                                    "code": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.nickname").value("验证码登录用户"))
                .andExpect(jsonPath("$.data.user.phone").value("13800004006"));
    }

    @Test
    void login_shouldFailWhenUserNotRegistered() throws Exception {
        sendCode("13800004007", "login");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "phone": "13800004007",
                                    "code": "123456"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpectAll(errorResult("RESOURCE_NOT_FOUND"));
    }

    // ── POST /api/v1/auth/mock-login ─────────────────────────────────

    @Test
    void mockLogin_shouldCreateUserAndReturnToken() throws Exception {
        mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "phone": "13800000001",
                                    "nickname": "测试用户1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.id").isString())
                .andExpect(jsonPath("$.data.user.nickname").value("测试用户1"))
                .andExpect(jsonPath("$.data.user.phone").value("13800000001"))
                .andExpect(jsonPath("$.data.user.hasLeader").value(false));
    }

    @Test
    void mockLogin_shouldReturnExistingUserOnSamePhone() throws Exception {
        // First login creates the user
        ResultActions first = mockMvc.perform(post(MOCK_LOGIN_URL)
                .contentType("application/json")
                .content("""
                        {
                            "phone": "13800000002",
                            "nickname": "已有用户"
                        }
                        """));

        String firstToken = first.andReturn().getResponse().getContentAsString();

        // Second login with same phone reuses the user (different token)
        mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "phone": "13800000002"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.id").isString())
                .andExpect(jsonPath("$.data.user.nickname").value("已有用户"))
                .andExpect(jsonPath("$.data.user.phone").value("13800000002"));
    }

    @Test
    void mockLogin_shouldGenerateNicknameWhenNotProvided() throws Exception {
        mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "phone": "13900000001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.user.nickname").value("用户0001"));
    }

    @Test
    void mockLogin_shouldFailWhenPhoneIsBlank() throws Exception {
        mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "phone": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void mockLogin_shouldFailWhenPhoneIsMissing() throws Exception {
        mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "nickname": "测试"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    // ── GET /api/v1/me ───────────────────────────────────────────────

    @Test
    void getMe_shouldReturnCurrentUser() throws Exception {
        // First login to get a token
        String response = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "phone": "13800000100",
                                    "nickname": "当前用户"
                                }
                                """))
                .andReturn().getResponse().getContentAsString();

        // Extract token using simple JSON path
        String token = response.split("\"accessToken\":\"")[1].split("\"")[0];

        // Get /me with the token
        mockMvc.perform(get(ME_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.user.id").isString())
                .andExpect(jsonPath("$.data.user.nickname").value("当前用户"))
                .andExpect(jsonPath("$.data.user.phone").value("13800000100"))
                .andExpect(jsonPath("$.data.leader").doesNotExist())
                .andExpect(jsonPath("$.data.store").doesNotExist());
    }

    @Test
    void getMe_shouldFailWhenNoToken() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void getMe_shouldFailWhenInvalidToken() throws Exception {
        mockMvc.perform(get(ME_URL)
                        .header("Authorization", "Bearer invalid_token_xxx"))
                .andExpect(status().isUnauthorized())
                .andExpect(contractResult())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void getMe_shouldFailWhenEmptyToken() throws Exception {
        mockMvc.perform(get(ME_URL)
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(contractResult())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void getMe_shouldFailWhenWrongAuthFormat() throws Exception {
        mockMvc.perform(get(ME_URL)
                        .header("Authorization", "Basic token123"))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    private void sendCode(String phone, String scene) throws Exception {
        mockMvc.perform(post(AUTH_CODES_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "phone": "%s",
                                    "scene": "%s"
                                }
                                """.formatted(phone, scene)))
                .andExpect(status().isOk());
    }
}
