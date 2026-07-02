package com.example.groupshop.store;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for StoreController endpoints.
 */
class StoreControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String STORES_URL = "/api/v1/stores";
    private static final String MY_STORE_URL = "/api/v1/my/store";

    // ── Helpers ──────────────────────────────────────────────────────

    /**
     * Login with a given phone and return the Bearer token value.
     */
    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }

    // ── POST /api/v1/stores ──────────────────────────────────────────

    @Test
    void createStore_shouldSucceed() throws Exception {
        String token = loginAndGetToken("13800001001");

        mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "某某的小店",
                                    "logoUrl": "https://example.com/logo.png",
                                    "description": "店铺简介",
                                    "defaultDeliveryType": "express"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.leader.id").isString())
                .andExpect(jsonPath("$.data.leader.displayName").value("某某的小店"))
                .andExpect(jsonPath("$.data.leader.avatarUrl").value("https://example.com/logo.png"))
                .andExpect(jsonPath("$.data.store.id").isString())
                .andExpect(jsonPath("$.data.store.leaderId").isString())
                .andExpect(jsonPath("$.data.store.name").value("某某的小店"))
                .andExpect(jsonPath("$.data.store.logoUrl").value("https://example.com/logo.png"))
                .andExpect(jsonPath("$.data.store.description").value("店铺简介"))
                .andExpect(jsonPath("$.data.store.defaultDeliveryType").value("express"))
                .andExpect(jsonPath("$.data.store.distributionEnabled").value(false))
                .andExpect(jsonPath("$.data.store.status").value("active"));
    }

    @Test
    void createStore_afterCreate_shouldUpdateMeEndpoint() throws Exception {
        String token = loginAndGetToken("13800001002");

        // Create store
        mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "测试店铺",
                                    "defaultDeliveryType": "express"
                                }
                                """));

        // Verify /me now has hasLeader=true, leaderId, storeId, and store status
        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpect(jsonPath("$.data.user.hasLeader").value(true))
                .andExpect(jsonPath("$.data.user.leaderId").isString())
                .andExpect(jsonPath("$.data.user.storeId").isString())
                .andExpect(jsonPath("$.data.leader").exists())
                .andExpect(jsonPath("$.data.store").exists())
                .andExpect(jsonPath("$.data.store.status").value("active"));
    }

    @Test
    void createStore_shouldReturn409WhenStoreAlreadyExists() throws Exception {
        String token = loginAndGetToken("13800001003");

        // First create succeeds
        mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "唯一店铺",
                                    "defaultDeliveryType": "express"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(contractResult());

        // Second create returns 409
        mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "另一个店铺",
                                    "defaultDeliveryType": "pickup"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(contractResult())
                .andExpectAll(errorResult("STORE_ALREADY_EXISTS"));
    }

    @Test
    void createStore_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post(STORES_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "测试店铺",
                                    "defaultDeliveryType": "express"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void createStore_shouldFailWhenNameIsBlank() throws Exception {
        String token = loginAndGetToken("13800001004");

        mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "",
                                    "defaultDeliveryType": "express"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void createStore_shouldFailWhenDefaultDeliveryTypeMissing() throws Exception {
        String token = loginAndGetToken("13800001005");

        mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "测试店铺"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void createStore_shouldFailWhenDefaultDeliveryTypeInvalid() throws Exception {
        String token = loginAndGetToken("13800001006");

        mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "测试店铺",
                                    "defaultDeliveryType": "invalid_type"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    // ── GET /api/v1/my/store ─────────────────────────────────────────

    @Test
    void getMyStore_shouldReturn200WithoutDataWhenNoStore() throws Exception {
        String token = loginAndGetToken("13800002001");

        mockMvc.perform(get(MY_STORE_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getMyStore_shouldReturnStoreWhenExists() throws Exception {
        String token = loginAndGetToken("13800002002");

        // Create store first
        mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "查询测试店铺",
                                    "defaultDeliveryType": "express"
                                }
                                """));

        // Get my store — returns { leader, store }
        mockMvc.perform(get(MY_STORE_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.leader.id").isString())
                .andExpect(jsonPath("$.data.leader.displayName").value("查询测试店铺"))
                .andExpect(jsonPath("$.data.store.name").value("查询测试店铺"))
                .andExpect(jsonPath("$.data.store.defaultDeliveryType").value("express"))
                .andExpect(jsonPath("$.data.store.status").value("active"))
                .andExpect(jsonPath("$.data.store.distributionEnabled").value(false));
    }

    @Test
    void getMyStore_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get(MY_STORE_URL))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    // ── PATCH /api/v1/my/store ───────────────────────────────────────

    @Test
    void updateMyStore_shouldPartialUpdate() throws Exception {
        String token = loginAndGetToken("13800003001");

        // Create store first
        mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "原始名称",
                                    "logoUrl": "https://example.com/old-logo.png",
                                    "description": "原始简介",
                                    "defaultDeliveryType": "express"
                                }
                                """));

        // Partial update: only name and description
        mockMvc.perform(patch(MY_STORE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "新名称",
                                    "description": "新简介"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.store.name").value("新名称"))
                .andExpect(jsonPath("$.data.store.logoUrl").value("https://example.com/old-logo.png"))
                .andExpect(jsonPath("$.data.store.description").value("新简介"))
                .andExpect(jsonPath("$.data.store.defaultDeliveryType").value("express"))
                .andExpect(jsonPath("$.data.leader.displayName").value("新名称"))
                .andExpect(jsonPath("$.data.leader.avatarUrl").value("https://example.com/old-logo.png"));
    }

    @Test
    void updateMyStore_shouldReturn403WhenNoStore() throws Exception {
        String token = loginAndGetToken("13800003002");

        mockMvc.perform(patch(MY_STORE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "新名称"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(contractResult())
                .andExpectAll(errorResult("LEADER_REQUIRED"));
    }

    @Test
    void updateMyStore_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(patch(MY_STORE_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "新名称"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void updateMyStore_shouldFailWhenLogoUrlIsEmpty() throws Exception {
        String token = loginAndGetToken("13800004001");

        // Create store first
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                        {
                            "name": "测试店铺",
                            "defaultDeliveryType": "express"
                        }
                        """));

        // PATCH with empty logoUrl
        mockMvc.perform(patch(MY_STORE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "logoUrl": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void updateMyStore_shouldFailWhenDescriptionIsEmpty() throws Exception {
        String token = loginAndGetToken("13800004002");

        // Create store first
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                        {
                            "name": "测试店铺",
                            "defaultDeliveryType": "express"
                        }
                        """));

        // PATCH with empty description
        mockMvc.perform(patch(MY_STORE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "description": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void updateMyStore_shouldFailWhenNameIsEmpty() throws Exception {
        String token = loginAndGetToken("13800004003");

        // Create store first
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                        {
                            "name": "测试店铺",
                            "defaultDeliveryType": "express"
                        }
                        """));

        // PATCH with empty name
        mockMvc.perform(patch(MY_STORE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    // ── Batch 06: Store Location ──────────────────────────────────────

    @Test
    void createStore_shouldSucceedWithLatitudeLongitude() throws Exception {
        String token = loginAndGetToken("13800005001");

        mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "坐标店铺",
                                    "defaultDeliveryType": "express",
                                    "latitude": 31.2304,
                                    "longitude": 121.4737
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.store.latitude").value(31.2304))
                .andExpect(jsonPath("$.data.store.longitude").value(121.4737));
    }

    @Test
    void createStore_shouldFailWhenLatitudeOutOfRange() throws Exception {
        String token = loginAndGetToken("13800005002");

        mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "无效纬度",
                                    "defaultDeliveryType": "express",
                                    "latitude": 100,
                                    "longitude": 121
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void updateMyStore_shouldSetLatitudeLongitude() throws Exception {
        String token = loginAndGetToken("13800005003");

        // Create store first
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                        {
                            "name": "坐标更新店铺",
                            "defaultDeliveryType": "express"
                        }
                        """));

        // Update with coordinates
        mockMvc.perform(patch(MY_STORE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "latitude": 30.2741,
                                    "longitude": 120.1551
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.store.latitude").value(30.2741))
                .andExpect(jsonPath("$.data.store.longitude").value(120.1551));
    }
}
