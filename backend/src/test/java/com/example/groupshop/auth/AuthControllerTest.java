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
    private static final String ME_URL = "/api/v1/me";

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
}
