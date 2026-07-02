package com.example.groupshop.browsing;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for BrowsingHistoryController endpoints.
 */
class BrowsingHistoryControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String STORES_URL = "/api/v1/stores";
    private static final String GROUP_BUYS_URL = "/api/v1/my/store/group-buys";
    private static final String BROWSING_HISTORIES_URL = "/api/v1/my/browsing-histories";

    private String leaderToken;
    private String buyerToken;
    private Long groupBuyId;

    @BeforeEach
    void setUp() throws Exception {
        leaderToken = loginAndGetToken("13800007001");
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + leaderToken)
                .contentType("application/json")
                .content("{\"name\":\"团长店铺\",\"defaultDeliveryType\":\"express\"}"));

        buyerToken = loginAndGetToken("13800007002");

        // Create a public published group buy
        String gbResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "浏览历史测试团购",
                                    "deliveryType": "express",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "商品",
                                                "basePriceAmount": 1000,
                                                "stock": 100
                                            },
                                            "displayName": "商品",
                                            "groupPriceAmount": 1990,
                                            "groupStock": 100
                                        }
                                    ]
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        groupBuyId = Long.parseLong(gbResponse.split("\"groupBuy\":\\{\"id\":")[1].split(",")[0].replace("\"", "").trim());
    }

    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }

    // ── GET /api/v1/my/browsing-histories ───────────────────────────────

    @Test
    void listMyHistories_shouldRequireAuth() throws Exception {
        mockMvc.perform(get(BROWSING_HISTORIES_URL))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void listMyHistories_shouldSucceed() throws Exception {
        // Trigger a browsing history record by viewing the group buy detail
        mockMvc.perform(get("/api/v1/group-buys/{groupId}", groupBuyId)
                        .header("Authorization", "Bearer " + buyerToken));

        mockMvc.perform(get(BROWSING_HISTORIES_URL)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.hasMore").isBoolean());
    }

    // ── DELETE /api/v1/my/browsing-histories/{historyId} ─────────────────

    @Test
    void deleteHistory_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete(BROWSING_HISTORIES_URL + "/1"))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void deleteHistory_shouldSucceed() throws Exception {
        // Trigger a browsing history record
        mockMvc.perform(get("/api/v1/group-buys/{groupId}", groupBuyId)
                .header("Authorization", "Bearer " + buyerToken));

        // Get the history ID
        String listResponse = mockMvc.perform(get(BROWSING_HISTORIES_URL)
                        .header("Authorization", "Bearer " + buyerToken))
                .andReturn().getResponse().getContentAsString();
        Long historyId = Long.parseLong(listResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Delete it
        mockMvc.perform(delete(BROWSING_HISTORIES_URL + "/" + historyId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteHistory_shouldReturnNotFoundWhenNonExistent() throws Exception {
        mockMvc.perform(delete(BROWSING_HISTORIES_URL + "/99999")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isNotFound())
                .andExpectAll(errorResult("RESOURCE_NOT_FOUND"));
    }

    @Test
    void deleteHistory_shouldReturnForbiddenForOtherUser() throws Exception {
        // Create browsing record as buyer
        mockMvc.perform(get("/api/v1/group-buys/{groupId}", groupBuyId)
                .header("Authorization", "Bearer " + buyerToken));

        // Get the history ID
        String listResponse = mockMvc.perform(get(BROWSING_HISTORIES_URL)
                        .header("Authorization", "Bearer " + buyerToken))
                .andReturn().getResponse().getContentAsString();
        Long historyId = Long.parseLong(listResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Try to delete as a different user
        String otherToken = loginAndGetToken("13800007003");
        mockMvc.perform(delete(BROWSING_HISTORIES_URL + "/" + historyId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("FORBIDDEN"));
    }
}
