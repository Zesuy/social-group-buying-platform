package com.example.groupshop.address;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for AddressController endpoints.
 */
class AddressControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String ADDRESSES_URL = "/api/v1/my/addresses";

    private String buyerToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() throws Exception {
        buyerToken = loginAndGetToken("13800008001");
        otherUserToken = loginAndGetToken("13800008002");
    }

    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }

    private Long createAddress(String token, String name) throws Exception {
        String response = mockMvc.perform(post(ADDRESSES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                    "receiverName": "%s",
                                    "receiverPhone": "13800000001",
                                    "province": "浙江省",
                                    "city": "杭州市",
                                    "district": "西湖区",
                                    "detail": "某某路 1 号"
                                }
                                """.formatted(name)))
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(response.split("\"id\":")[1].split(",")[0]);
    }

    // ── Unauthenticated ─────────────────────────────────────────────────

    @Test
    void addressEndpoints_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get(ADDRESSES_URL))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));

        mockMvc.perform(post(ADDRESSES_URL)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    // ── Create ──────────────────────────────────────────────────────────

    @Test
    void createAddress_shouldSucceed() throws Exception {
        mockMvc.perform(post(ADDRESSES_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "receiverName": "张三",
                                    "receiverPhone": "13800000001",
                                    "province": "浙江省",
                                    "city": "杭州市",
                                    "district": "西湖区",
                                    "detail": "某某路 1 号"
                                }
                                """))
                .andExpect(status().isOk())
                
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.receiverName").value("张三"))
                .andExpect(jsonPath("$.data.fullAddress").isString())
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    void createAddress_shouldFailWhenValidationFails() throws Exception {
        mockMvc.perform(post(ADDRESSES_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "receiverName": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    // ── List ────────────────────────────────────────────────────────────

    @Test
    void listAddresses_shouldReturnList() throws Exception {
        createAddress(buyerToken, "张三");
        createAddress(buyerToken, "李四");

        mockMvc.perform(get(ADDRESSES_URL)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    // ── Update ──────────────────────────────────────────────────────────

    @Test
    void updateAddress_shouldSucceed() throws Exception {
        Long addrId = createAddress(buyerToken, "张三");

        mockMvc.perform(patch(ADDRESSES_URL + "/" + addrId)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("{\"receiverName\":\"张四\",\"isDefault\":true}"))
                .andExpect(status().isOk())
                
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.receiverName").value("张四"));
    }

    @Test
    void updateAddress_shouldFailWhenForbidden() throws Exception {
        Long addrId = createAddress(buyerToken, "张三");

        mockMvc.perform(patch(ADDRESSES_URL + "/" + addrId)
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType("application/json")
                        .content("{\"receiverName\":\"李四\"}"))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("ADDRESS_FORBIDDEN"));
    }

    // ── Delete ──────────────────────────────────────────────────────────

    @Test
    void deleteAddress_shouldSucceed() throws Exception {
        Long addrId = createAddress(buyerToken, "张三");

        mockMvc.perform(delete(ADDRESSES_URL + "/" + addrId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                
                .andExpect(contractResult())
                .andExpectAll(successResult());
    }

    @Test
    void deleteAddress_shouldFailWhenNotFound() throws Exception {
        mockMvc.perform(delete(ADDRESSES_URL + "/99999")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isNotFound())
                .andExpectAll(errorResult("RESOURCE_NOT_FOUND"));
    }
}
