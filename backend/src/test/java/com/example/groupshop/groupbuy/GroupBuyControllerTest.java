package com.example.groupshop.groupbuy;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for GroupBuyController endpoints.
 */
class GroupBuyControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String STORES_URL = "/api/v1/stores";
    private static final String PRODUCTS_URL = "/api/v1/my/store/products";
    private static final String GROUP_BUYS_URL = "/api/v1/my/store/group-buys";

    private String leaderToken;
    private String regularUserToken;
    private String otherLeaderToken;

    @BeforeEach
    void setUp() throws Exception {
        leaderToken = loginAndGetToken("13800006001");
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + leaderToken)
                .contentType("application/json")
                .content("{\"name\":\"团长店铺\",\"defaultDeliveryType\":\"express\"}"));

        regularUserToken = loginAndGetToken("13800006002");

        otherLeaderToken = loginAndGetToken("13800006003");
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + otherLeaderToken)
                .contentType("application/json")
                .content("{\"name\":\"其他店铺\",\"defaultDeliveryType\":\"express\"}"));
    }

    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }

    // ── POST /api/v1/my/store/group-buys ──────────────────────────────

    @Test
    void createGroupBuy_shouldSucceedWithInlineProduct() throws Exception {
        mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "山东蜜桃团购",
                                    "introduction": "产地直发，香甜多汁",
                                    "coverImageUrl": "https://example.com/cover.png",
                                    "deliveryType": "express",
                                    "shippingTime": "2026-06-30T18:00:00+08:00",
                                    "startTime": "2026-06-24T12:00:00+08:00",
                                    "endTime": "2026-07-01T12:00:00+08:00",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "白玉蜜桃",
                                                "description": "山东蒙阴产地直发",
                                                "coverImageUrl": "https://example.com/product.png",
                                                "basePriceAmount": 2990,
                                                "stock": 100
                                            },
                                            "displayName": "白玉蜜桃 5 斤装",
                                            "groupPriceAmount": 2990,
                                            "groupStock": 100,
                                            "sortOrder": 1
                                        }
                                    ]
                                }
                                """))
                .andExpect(status().isOk())
                                .andExpect(contractResult()).andExpectAll(successResult())
                .andExpect(jsonPath("$.data.groupBuy.id").isString())
                .andExpect(jsonPath("$.data.groupBuy.storeId").isString())
                .andExpect(jsonPath("$.data.groupBuy.title").value("山东蜜桃团购"))
                .andExpect(jsonPath("$.data.groupBuy.groupType").value("normal"))
                .andExpect(jsonPath("$.data.groupBuy.status").value("published"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].displayName").value("白玉蜜桃 5 斤装"))
                .andExpect(jsonPath("$.data.items[0].groupPriceAmount").value(2990))
                .andExpect(jsonPath("$.data.items[0].groupStock").value(100))
                .andExpect(jsonPath("$.data.items[0].soldCount").value(0));
    }

    @Test
    void createGroupBuy_shouldSucceedWithExistingProduct() throws Exception {
        // Create a product first
        String productResponse = mockMvc.perform(post(PRODUCTS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "蜜桃",
                                    "basePriceAmount": 2990,
                                    "stock": 100
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long productId = Long.parseLong(productResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "复用商品团购",
                                    "deliveryType": "express",
                                    "items": [
                                        {
                                            "productId": "%s",
                                            "displayName": "蜜桃 5 斤装",
                                            "groupPriceAmount": 2990,
                                            "groupStock": 50,
                                            "sortOrder": 1
                                        }
                                    ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                                .andExpect(contractResult()).andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items[0].productId").value(String.valueOf(productId)));
    }

    @Test
    void createGroupBuy_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post(GROUP_BUYS_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "团购",
                                    "deliveryType": "express",
                                    "items": []
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void createGroupBuy_shouldFailWhenNotLeader() throws Exception {
        mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + regularUserToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "团购",
                                    "deliveryType": "express",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "商品",
                                                "basePriceAmount": 1000,
                                                "stock": 10
                                            },
                                            "displayName": "商品",
                                            "groupPriceAmount": 1000,
                                            "groupStock": 10
                                        }
                                    ]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("LEADER_REQUIRED"));
    }

    @Test
    void createGroupBuy_shouldFailWhenCrossStoreProduct() throws Exception {
        // Create a product for other leader
        String otherProductResponse = mockMvc.perform(post(PRODUCTS_URL)
                        .header("Authorization", "Bearer " + otherLeaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "别人商品",
                                    "basePriceAmount": 1000,
                                    "stock": 10
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long otherProductId = Long.parseLong(otherProductResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Try to use it in current leader's group buy
        mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "跨店团购",
                                    "deliveryType": "express",
                                    "items": [
                                        {
                                            "productId": "%s",
                                            "displayName": "别人商品",
                                            "groupPriceAmount": 1000,
                                            "groupStock": 10
                                        }
                                    ]
                                }
                                """.formatted(otherProductId)))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("STORE_FORBIDDEN"));
    }

    @Test
    void createGroupBuy_shouldFailWhenGroupStockNegative() throws Exception {
        mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "库存错误",
                                    "deliveryType": "express",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "商品",
                                                "basePriceAmount": 1000,
                                                "stock": 10
                                            },
                                            "displayName": "商品",
                                            "groupPriceAmount": 1000,
                                            "groupStock": -1
                                        }
                                    ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    // ── GET /api/v1/my/store/group-buys ───────────────────────────────

    @Test
    void listGroupBuys_shouldSucceed() throws Exception {
        mockMvc.perform(get(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                                .andExpect(contractResult()).andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.hasMore").isBoolean());
    }

    // ── GET /api/v1/my/store/group-buys/{groupBuyId} ──────────────────

    @Test
    void getGroupBuyDetail_shouldSucceed() throws Exception {
        // Create a group buy first
        String createResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "详情团购",
                                    "deliveryType": "express",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "商品",
                                                "basePriceAmount": 1000,
                                                "stock": 10
                                            },
                                            "displayName": "商品",
                                            "groupPriceAmount": 1000,
                                            "groupStock": 10
                                        }
                                    ]
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long groupBuyId = Long.parseLong(createResponse.split("\"groupBuy\":\\{\"id\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(get(GROUP_BUYS_URL + "/" + groupBuyId)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                                .andExpect(contractResult()).andExpectAll(successResult())
                .andExpect(jsonPath("$.data.groupBuy.id").value(String.valueOf(groupBuyId)))
                .andExpect(jsonPath("$.data.groupBuy.title").value("详情团购"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void getGroupBuyDetail_shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get(GROUP_BUYS_URL + "/99999")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isNotFound())
                .andExpectAll(errorResult("RESOURCE_NOT_FOUND"));
    }

    // ── PATCH /api/v1/my/store/group-buys/{groupBuyId} ────────────────

    @Test
    void updateGroupBuy_shouldSucceed() throws Exception {
        // Create a group buy first
        String createResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "原始标题",
                                    "introduction": "原始介绍",
                                    "deliveryType": "express",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "商品",
                                                "basePriceAmount": 1000,
                                                "stock": 10
                                            },
                                            "displayName": "商品",
                                            "groupPriceAmount": 1000,
                                            "groupStock": 10
                                        }
                                    ]
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long groupBuyId = Long.parseLong(createResponse.split("\"groupBuy\":\\{\"id\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(patch(GROUP_BUYS_URL + "/" + groupBuyId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "新标题",
                                    "introduction": "新介绍"
                                }
                                """))
                .andExpect(status().isOk())
                                .andExpect(contractResult()).andExpectAll(successResult())
                .andExpect(jsonPath("$.data.groupBuy.title").value("新标题"))
                .andExpect(jsonPath("$.data.groupBuy.introduction").value("新介绍"));
    }

    // ── POST /api/v1/my/store/group-buys/{groupBuyId}/end ─────────────

    @Test
    void endGroupBuy_shouldSucceed() throws Exception {
        // Create a group buy first
        String createResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "可结束团购",
                                    "deliveryType": "express",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "商品",
                                                "basePriceAmount": 1000,
                                                "stock": 10
                                            },
                                            "displayName": "商品",
                                            "groupPriceAmount": 1000,
                                            "groupStock": 10
                                        }
                                    ]
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long groupBuyId = Long.parseLong(createResponse.split("\"groupBuy\":\\{\"id\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(post(GROUP_BUYS_URL + "/" + groupBuyId + "/end")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                                .andExpect(contractResult()).andExpectAll(successResult())
                .andExpect(jsonPath("$.data.groupBuy.status").value("ended"));
    }

    @Test
    void endGroupBuy_shouldFailWhenAlreadyEnded() throws Exception {
        // Create and end a group buy
        String createResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "已结束团购",
                                    "deliveryType": "express",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "商品",
                                                "basePriceAmount": 1000,
                                                "stock": 10
                                            },
                                            "displayName": "商品",
                                            "groupPriceAmount": 1000,
                                            "groupStock": 10
                                        }
                                    ]
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long groupBuyId = Long.parseLong(createResponse.split("\"groupBuy\":\\{\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // End first
        mockMvc.perform(post(GROUP_BUYS_URL + "/" + groupBuyId + "/end")
                .header("Authorization", "Bearer " + leaderToken));

        // End again should fail
        mockMvc.perform(post(GROUP_BUYS_URL + "/" + groupBuyId + "/end")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpectAll(errorResult("BUSINESS_RULE_VIOLATION"));
    }
}
