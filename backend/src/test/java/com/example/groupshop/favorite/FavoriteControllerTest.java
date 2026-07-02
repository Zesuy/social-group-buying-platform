package com.example.groupshop.favorite;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for FavoriteController endpoints.
 */
class FavoriteControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String STORES_URL = "/api/v1/stores";
    private static final String GROUP_BUYS_URL = "/api/v1/my/store/group-buys";
    private static final String PUBLIC_GROUP_BUYS_URL = "/api/v1/group-buys";
    private static final String MY_FAVORITES_URL = "/api/v1/my/favorites";

    private String leaderToken;
    private String buyerToken;
    private Long groupBuyId;

    @BeforeEach
    void setUp() throws Exception {
        // Create leader with store
        String leaderLoginBody = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"13800006001\"}"))
                .andReturn().getResponse().getContentAsString();
        leaderToken = extractToken(leaderLoginBody);

        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + leaderToken)
                .contentType("application/json")
                .content("{\"name\":\"团长店铺\",\"defaultDeliveryType\":\"express\"}"));

        // Create buyer user
        String buyerLoginBody = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"13800006002\"}"))
                .andReturn().getResponse().getContentAsString();
        buyerToken = extractToken(buyerLoginBody);

        // Create a public published group buy
        String gbResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "收藏测试团购",
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

    // ── POST /api/v1/group-buys/{groupBuyId}/favorite ───────────────────

    @Test
    void favorite_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/v1/group-buys/{groupBuyId}/favorite", groupBuyId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.groupBuyId").value(groupBuyId))
                .andExpect(jsonPath("$.data.favoritedAt").isString());
    }

    @Test
    void favorite_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/group-buys/{groupBuyId}/favorite", groupBuyId))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    // ── DELETE /api/v1/group-buys/{groupBuyId}/favorite ─────────────────

    @Test
    void cancelFavorite_shouldSucceed() throws Exception {
        // Favorite first
        mockMvc.perform(post("/api/v1/group-buys/{groupBuyId}/favorite", groupBuyId)
                .header("Authorization", "Bearer " + buyerToken));

        // Then cancel
        mockMvc.perform(delete("/api/v1/group-buys/{groupBuyId}/favorite", groupBuyId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── GET /api/v1/my/favorites ────────────────────────────────────────

    @Test
    void listMyFavorites_shouldRequireAuth() throws Exception {
        mockMvc.perform(get(MY_FAVORITES_URL))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void listMyFavorites_shouldReturnFavoritedItems() throws Exception {
        // Favorite the group buy
        mockMvc.perform(post("/api/v1/group-buys/{groupBuyId}/favorite", groupBuyId)
                .header("Authorization", "Bearer " + buyerToken));

        // List favorites
        mockMvc.perform(get(MY_FAVORITES_URL)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].groupBuyId").value(groupBuyId))
                .andExpect(jsonPath("$.data.items[0].title").value("收藏测试团购"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    // ── Favorite then public detail shows favorited=true ─────────────────

    @Test
    void getPublicGroupBuyDetail_shouldReturnFavoritedTrueWhenFavorited() throws Exception {
        // Favorite the group buy
        mockMvc.perform(post("/api/v1/group-buys/{groupBuyId}/favorite", groupBuyId)
                .header("Authorization", "Bearer " + buyerToken));

        // Get public detail with auth
        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL + "/" + groupBuyId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.viewer.subscribed").value(false))
                .andExpect(jsonPath("$.data.viewer.favorited").value(true));
    }

    @Test
    void getPublicGroupBuyDetail_shouldReturnFavoritedFalseWhenNotFavorited() throws Exception {
        mockMvc.perform(get(PUBLIC_GROUP_BUYS_URL + "/" + groupBuyId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.viewer.favorited").value(false));
    }
}
