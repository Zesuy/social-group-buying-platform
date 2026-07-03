package com.example.groupshop.publicbrowsing;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for public browsing endpoints.
 */
class PublicBrowsingControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String STORES_URL = "/api/v1/stores";
    private static final String MY_STORE_URL = "/api/v1/my/store";
    private static final String GROUP_BUYS_URL = "/api/v1/my/store/group-buys";
    private static final String PUBLIC_GROUP_BUYS_URL = "/api/v1/group-buys";

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        return Long.parseLong(response.split("\"groupBuy\":\\{\"id\":")[1].split(",")[0].replace("\"", "").trim());
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
        Long leaderId = Long.parseLong(meResponse.split("\"leaderId\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(get("/api/v1/leaders/" + leaderId + "/homepage"))
                .andExpect(status().isOk())

                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.leader.id").value(String.valueOf(leaderId)))
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
        Long leaderId = Long.parseLong(meResponse.split("\"leaderId\":")[1].split(",")[0].replace("\"", "").trim());

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
        Long leaderId = Long.parseLong(meResponse.split("\"leaderId\":")[1].split(",")[0].replace("\"", "").trim());

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
        Long leaderId = Long.parseLong(meResponse.split("\"leaderId\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(get("/api/v1/leaders/" + leaderId + "/homepage"))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.viewer.subscribed").value(false));
    }

    // ── Batch P1: keyword, category, favorited backfill ───────

    @Test
    void listPublicGroupBuys_shouldRejectStatusParameter() throws Exception {
        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL)
                        .param("status", "published"))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void listPublicGroupBuys_shouldSupportKeywordFiltering() throws Exception {
        createPublishedGroupBuy("山东蜜桃团购");

        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL)
                        .param("keyword", "蜜桃"))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("山东蜜桃团购"));
    }

    @Test
    void listPublicGroupBuys_keywordFilter_shouldReturnEmptyWhenNoMatch() throws Exception {
        createPublishedGroupBuy("一些团购");

        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL)
                        .param("keyword", "不存在的关键词"))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void getPublicGroupBuyDetail_shouldReturnFavoritedWhenAuthenticated() throws Exception {
        Long gbId = createPublishedGroupBuy("收藏详情测试团购");

        // Login as a buyer and favorite
        // Note: The favorite endpoint requires requestAttr since AuthInterceptor
        // excludes /api/v1/group-buys/** paths
        String buyerLoginBody = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"13800013001\"}"))
                .andReturn().getResponse().getContentAsString();
        String buyerToken = extractToken(buyerLoginBody);
        Long buyerUserId = Long.parseLong(buyerLoginBody.split("\"user\":\\{\"id\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(post("/api/v1/group-buys/{groupBuyId}/favorite", gbId)
                .header("Authorization", "Bearer " + buyerToken)
                .requestAttr("currentUserId", buyerUserId));

        // Get detail as the favorited viewer
        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL + "/" + gbId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.viewer.favorited").value(true));
    }

    // ── Batch 06: Location / Distance ──────────────────────────────────

    @Test
    void listPublicGroupBuys_shouldReturnDistanceWithLocation() throws Exception {
        // Update store with coordinates (Shanghai)
        mockMvc.perform(patch(MY_STORE_URL)
                .header("Authorization", "Bearer " + leaderToken)
                .contentType("application/json")
                .content("{\"latitude\":31.2304,\"longitude\":121.4737}"));

        createPublishedGroupBuy("坐标团购");

        // Query from Hangzhou
        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL)
                        .param("latitude", "30.2741")
                        .param("longitude", "120.1551"))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items[0].store.latitude").value(31.2304))
                .andExpect(jsonPath("$.data.items[0].store.longitude").value(121.4737))
                .andExpect(jsonPath("$.data.items[0].store.distanceMeters").isNumber())
                .andExpect(jsonPath("$.data.items[0].store.distanceText").isString());
    }

    @Test
    void listPublicGroupBuys_shouldRejectMissingCoordinate() throws Exception {
        // Only latitude without longitude
        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL)
                        .param("latitude", "31.2304"))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void listPublicGroupBuys_shouldRejectMaxDistanceWithoutLocation() throws Exception {
        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL)
                        .param("maxDistanceMeters", "5000"))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void listPublicGroupBuys_shouldRejectSortDistanceWithoutLocation() throws Exception {
        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL)
                        .param("sort", "distance"))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void listPublicGroupBuys_shouldRejectOutOfRangeLatitude() throws Exception {
        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL)
                        .param("latitude", "100")
                        .param("longitude", "121"))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void listPublicGroupBuys_shouldRejectOutOfRangeLongitude() throws Exception {
        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL)
                        .param("latitude", "30")
                        .param("longitude", "200"))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void getPublicGroupBuyDetail_shouldRejectOutOfRangeLatitude() throws Exception {
        Long gbId = createPublishedGroupBuy("范围详情测试");
        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL + "/" + gbId)
                        .param("latitude", "-100")
                        .param("longitude", "0"))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void getPublicGroupBuyDetail_shouldReturnDistanceWithLocation() throws Exception {
        // Update store with coordinates (Shanghai)
        mockMvc.perform(patch(MY_STORE_URL)
                .header("Authorization", "Bearer " + leaderToken)
                .contentType("application/json")
                .content("{\"latitude\":31.2304,\"longitude\":121.4737}"));

        Long gbId = createPublishedGroupBuy("详情坐标测试");

        // Query from Hangzhou
        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL + "/" + gbId)
                        .param("latitude", "30.2741")
                        .param("longitude", "120.1551"))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.store.latitude").value(31.2304))
                .andExpect(jsonPath("$.data.store.longitude").value(121.4737))
                .andExpect(jsonPath("$.data.store.distanceMeters").isNumber())
                .andExpect(jsonPath("$.data.store.distanceText").isString());
    }

    @Test
    void getPublicGroupBuyDetail_shouldReturnNullDistanceWithoutLocation() throws Exception {
        Long gbId = createPublishedGroupBuy("无位置详情测试");

        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL + "/" + gbId))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.store.distanceMeters").doesNotExist())
                .andExpect(jsonPath("$.data.store.distanceText").doesNotExist());
    }

    // ── Content fields & featuredItem ──────────────────────────────────

    @Test
    void getPublicGroupBuyDetail_shouldReturnContentFieldsAndFeaturedItem() throws Exception {
        Long gbId = createPublishedGroupBuy("内容团购测试");

        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL + "/" + gbId))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.groupBuy.galleryImageUrls").exists())
                .andExpect(jsonPath("$.data.groupBuy.contentBlocks").exists())
                .andExpect(jsonPath("$.data.featuredItem").exists())
                .andExpect(jsonPath("$.data.items[0].product").exists())
                .andExpect(jsonPath("$.data.items[0].product.id").isString())
                .andExpect(jsonPath("$.data.items[0].product.name").isString())
                .andExpect(jsonPath("$.data.items[0].product.detailImageUrls").exists());
    }
}
