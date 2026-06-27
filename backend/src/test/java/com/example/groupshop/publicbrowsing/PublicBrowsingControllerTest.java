package com.example.groupshop.publicbrowsing;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for public browsing endpoints.
 */
class PublicBrowsingControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String STORES_URL = "/api/v1/stores";
    private static final String GROUP_BUYS_URL = "/api/v1/my/store/group-buys";
    private static final String PUBLIC_GROUP_BUYS_URL = "/api/v1/group-buys";

    private String leaderToken;

    @BeforeEach
    void setUp() throws Exception {
        leaderToken = loginAndGetToken("13800007001");
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + leaderToken)
                .contentType("application/json")
                .content("{\"name\":\"团长店铺\",\"defaultDeliveryType\":\"express\"}"));
    }

    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }

    private Long createPublishedGroupBuy(String title) throws Exception {
        String response = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "%s",
                                    "deliveryType": "express",
                                    "startTime": "2026-06-24T12:00:00+08:00",
                                    "endTime": "2028-07-01T12:00:00+08:00",
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
                                """.formatted(title)))
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(response.split("\"groupBuy\":\\{\"id\":")[1].split(",")[0]);
    }

    // ── GET /api/v1/group-buys ───────────────────────────────────────────

    @Test
    void listPublicGroupBuys_shouldSucceedWithoutAuth() throws Exception {
        createPublishedGroupBuy("公开团购1");

        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL))
                .andExpect(status().isOk())
                
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.hasMore").isBoolean());
    }

    @Test
    void listPublicGroupBuys_shouldSucceedWithAuth() throws Exception {
        createPublishedGroupBuy("公开团购2");
        String buyerToken = loginAndGetToken("13800007002");

        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void listPublicGroupBuys_shouldReturnOnlyPublishedPublic() throws Exception {
        createPublishedGroupBuy("公开团购3");

        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.items[0].leader").exists())
                .andExpect(jsonPath("$.data.items[0].store").exists())
                .andExpect(jsonPath("$.data.items[0].minPriceAmount").isNumber())
                .andExpect(jsonPath("$.data.items[0].soldCount").isNumber());
    }

    @Test
    void endedGroupBuy_shouldBeExcludedFromPublicDetail() throws Exception {
        // Create a group buy and end it
        Long gbId = createPublishedGroupBuy("即将结束团购");
        mockMvc.perform(post("/api/v1/my/store/group-buys/" + gbId + "/end")
                .header("Authorization", "Bearer " + leaderToken));

        // Ended group buy should return 404 from public detail
        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL + "/" + gbId))
                .andExpect(status().isNotFound())
                .andExpectAll(errorResult("RESOURCE_NOT_FOUND"));
    }

    // ── GET /api/v1/group-buys/{groupBuyId} ─────────────────────────────

    @Test
    void getPublicGroupBuyDetail_shouldSucceed() throws Exception {
        Long gbId = createPublishedGroupBuy("详情团购");

        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL + "/" + gbId))
                .andExpect(status().isOk())
                
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.groupBuy.id").value(gbId))
                .andExpect(jsonPath("$.data.leader").exists())
                .andExpect(jsonPath("$.data.store").exists())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.viewer.subscribed").value(false));
    }

    @Test
    void getPublicGroupBuyDetail_shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL + "/99999"))
                .andExpect(status().isNotFound())
                .andExpectAll(errorResult("RESOURCE_NOT_FOUND"));
    }

    // ── GET /api/v1/leaders/{leaderId}/homepage ─────────────────────────

    @Test
    void getLeaderHomepage_shouldSucceed() throws Exception {
        createPublishedGroupBuy("团长主页团购");

        // Get leader ID from store creation
        String meResponse = mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + leaderToken))
                .andReturn().getResponse().getContentAsString();
        Long leaderId = Long.parseLong(meResponse.split("\"leaderId\":")[1].split(",")[0]);

        mockMvc.perform(get("/api/v1/leaders/" + leaderId + "/homepage"))
                .andExpect(status().isOk())

                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.leader.id").value(leaderId))
                .andExpect(jsonPath("$.data.store").exists())
                .andExpect(jsonPath("$.data.viewer.subscribed").value(false))
                .andExpect(jsonPath("$.data.groupBuys.items").isArray());
    }

    // ── Batch 09 backfill: authenticated viewer.subscribed ─────────────

    @Test
    void getPublicGroupBuyDetail_shouldReturnRealSubscribedWhenAuthenticated() throws Exception {
        Long gbId = createPublishedGroupBuy("已登录团购详情");

        // Login as a viewer
        String viewerToken = loginAndGetToken("13800012001");

        // Get leader ID to subscribe
        String meResponse = mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + leaderToken))
                .andReturn().getResponse().getContentAsString();
        Long leaderId = Long.parseLong(meResponse.split("\"leaderId\":")[1].split(",")[0]);

        // Subscribe to the leader
        mockMvc.perform(post("/api/v1/leaders/{leaderId}/subscription", leaderId)
                .header("Authorization", "Bearer " + viewerToken)
                .contentType("application/json")
                .content("{\"source\":\"homepage\"}"));

        // Now get group buy detail as the subscribed viewer
        mockMvc.perform(get("/api/v1/group-buys/" + gbId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.viewer.subscribed").value(true));
    }

    @Test
    void getPublicGroupBuyDetail_shouldReturnSubscribedFalseWhenNotLoggedIn() throws Exception {
        Long gbId = createPublishedGroupBuy("未登录团购详情");

        mockMvc.perform(get("/api/v1/group-buys/" + gbId))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.viewer.subscribed").value(false));
    }

    @Test
    void getLeaderHomepage_shouldReturnRealSubscribedWhenAuthenticated() throws Exception {
        createPublishedGroupBuy("已登录团长主页");

        String meResponse = mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + leaderToken))
                .andReturn().getResponse().getContentAsString();
        Long leaderId = Long.parseLong(meResponse.split("\"leaderId\":")[1].split(",")[0]);

        // Login as viewer and subscribe
        String viewerToken = loginAndGetToken("13800012002");
        mockMvc.perform(post("/api/v1/leaders/{leaderId}/subscription", leaderId)
                .header("Authorization", "Bearer " + viewerToken)
                .contentType("application/json")
                .content("{\"source\":\"homepage\"}"));

        // Get homepage as the subscribed viewer
        mockMvc.perform(get("/api/v1/leaders/" + leaderId + "/homepage")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.viewer.subscribed").value(true));
    }

    @Test
    void getLeaderHomepage_shouldReturnSubscribedFalseWhenNotLoggedIn() throws Exception {
        createPublishedGroupBuy("未登录团长主页");

        String meResponse = mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + leaderToken))
                .andReturn().getResponse().getContentAsString();
        Long leaderId = Long.parseLong(meResponse.split("\"leaderId\":")[1].split(",")[0]);

        mockMvc.perform(get("/api/v1/leaders/" + leaderId + "/homepage"))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.viewer.subscribed").value(false));
    }
}
