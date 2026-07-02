package com.example.groupshop.memberlevel;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for MemberLevelRuleController endpoints.
 */
class MemberLevelRuleControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String STORES_URL = "/api/v1/stores";
    private static final String RULES_URL = "/api/v1/my/store/member-level-rules";

    private static int testCounter = 0;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        testCounter++;
        String phone = String.format("1380005%04d", testCounter);
        token = loginAndGetToken(phone);

        mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "等级规则店%d",
                                    "defaultDeliveryType": "express"
                                }
                                """.formatted(testCounter)));
    }

    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }

    // ── GET /api/v1/my/store/member-level-rules ───────────────────────────

    @Test
    void getRules_shouldReturnEmptyWhenNoRules() throws Exception {
        mockMvc.perform(get(RULES_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.rules").isArray())
                .andExpect(jsonPath("$.data.rules.length()").value(0));
    }

    @Test
    void getRules_shouldFailWhenNotStoreOwner() throws Exception {
        testCounter++;
        String otherPhone = String.format("1380005%04d", testCounter);
        String otherToken = loginAndGetToken(otherPhone);

        mockMvc.perform(get(RULES_URL)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("LEADER_REQUIRED"));
    }

    @Test
    void getRules_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get(RULES_URL))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    // ── PUT /api/v1/my/store/member-level-rules ───────────────────────────

    @Test
    void updateRules_shouldSucceed() throws Exception {
        mockMvc.perform(put(RULES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "rules": [
                                        {"levelName": "V0", "minGrowthValue": 0},
                                        {"levelName": "V1", "minGrowthValue": 1000},
                                        {"levelName": "V2", "minGrowthValue": 5000}
                                    ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.rules.length()").value(3))
                .andExpect(jsonPath("$.data.rules[0].levelName").value("V0"))
                .andExpect(jsonPath("$.data.rules[1].levelName").value("V1"))
                .andExpect(jsonPath("$.data.rules[2].levelName").value("V2"));
    }

    @Test
    void updateRules_shouldFailWhenNoBaseRule() throws Exception {
        mockMvc.perform(put(RULES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "rules": [
                                        {"levelName": "V1", "minGrowthValue": 1000}
                                    ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void updateRules_shouldFailWhenThresholdsNotAscending() throws Exception {
        mockMvc.perform(put(RULES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "rules": [
                                        {"levelName": "V0", "minGrowthValue": 0},
                                        {"levelName": "V1", "minGrowthValue": 5000},
                                        {"levelName": "V2", "minGrowthValue": 1000}
                                    ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void updateRules_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(put(RULES_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "rules": [
                                        {"levelName": "V0", "minGrowthValue": 0}
                                    ]
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void updateRules_thenGet_shouldReturnUpdatedRules() throws Exception {
        // Update rules
        mockMvc.perform(put(RULES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "rules": [
                                        {"levelName": "VIP0", "minGrowthValue": 0},
                                        {"levelName": "VIP1", "minGrowthValue": 100}
                                    ]
                                }
                                """))
                .andExpect(status().isOk());

        // Get should return the updated rules
        mockMvc.perform(get(RULES_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.rules.length()").value(2))
                .andExpect(jsonPath("$.data.rules[0].levelName").value("VIP0"))
                .andExpect(jsonPath("$.data.rules[1].levelName").value("VIP1"));
    }
}
