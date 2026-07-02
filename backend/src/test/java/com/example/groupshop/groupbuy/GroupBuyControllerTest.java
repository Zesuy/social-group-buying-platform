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
                                    "stock": 100,
                                    "categoryId": 1
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
                                    "stock": 10,
                                    "categoryId": 1
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

    // ── Draft endpoints ────────────────────────────────────────────────

    @Test
    void createDraft_shouldSucceed() throws Exception {
        mockMvc.perform(post(GROUP_BUYS_URL + "/drafts")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "测试草稿",
                                    "deliveryType": "express",
                                    "groupType": "normal",
                                    "visibility": "public",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "商品",
                                                "basePriceAmount": 1000,
                                                "stock": 10
                                            },
                                            "displayName": "商品",
                                            "groupPriceAmount": 0,
                                            "groupStock": 0
                                        }
                                    ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.groupBuy.status").value("draft"))
                .andExpect(jsonPath("$.data.groupBuy.groupType").value("normal"));
    }

    @Test
    void createDraft_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post(GROUP_BUYS_URL + "/drafts")
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "测试草稿",
                                    "deliveryType": "express",
                                    "items": []
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void createDraft_shouldFailWhenNotLeader() throws Exception {
        mockMvc.perform(post(GROUP_BUYS_URL + "/drafts")
                        .header("Authorization", "Bearer " + regularUserToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "测试草稿",
                                    "deliveryType": "express",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "商品",
                                                "basePriceAmount": 1000,
                                                "stock": 10
                                            },
                                            "displayName": "商品",
                                            "groupPriceAmount": 0,
                                            "groupStock": 0
                                        }
                                    ]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("LEADER_REQUIRED"));
    }

    // ── Publish endpoint ────────────────────────────────────────────────

    @Test
    void publishGroupBuy_shouldSucceed() throws Exception {
        // Create draft
        String draftResponse = mockMvc.perform(post(GROUP_BUYS_URL + "/drafts")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "可发布草稿",
                                    "deliveryType": "express",
                                    "startTime": "2026-07-10T12:00:00+08:00",
                                    "endTime": "2026-07-20T12:00:00+08:00",
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
        Long groupBuyId = Long.parseLong(draftResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(post(GROUP_BUYS_URL + "/" + groupBuyId + "/publish")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.groupBuy.status").value("published"));
    }

    @Test
    void publishGroupBuy_shouldFailWhenNotDraft() throws Exception {
        // Create and publish a group buy directly
        String createResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "已发布",
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

        // Try to publish an already-published group buy
        mockMvc.perform(post(GROUP_BUYS_URL + "/" + groupBuyId + "/publish")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpectAll(errorResult("BUSINESS_RULE_VIOLATION"));
    }

    // ── Preview endpoint ───────────────────────────────────────────────

    @Test
    void previewGroupBuy_shouldSucceedForDraft() throws Exception {
        // Create draft
        String draftResponse = mockMvc.perform(post(GROUP_BUYS_URL + "/drafts")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "预览草稿",
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
        Long groupBuyId = Long.parseLong(draftResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(get(GROUP_BUYS_URL + "/" + groupBuyId + "/preview")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.groupBuy.status").value("draft"))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void previewGroupBuy_shouldFailWhenCrossStore() throws Exception {
        // Create draft as leader
        String draftResponse = mockMvc.perform(post(GROUP_BUYS_URL + "/drafts")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "跨店预览",
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
        Long groupBuyId = Long.parseLong(draftResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Other leader tries to preview
        mockMvc.perform(get(GROUP_BUYS_URL + "/" + groupBuyId + "/preview")
                        .header("Authorization", "Bearer " + otherLeaderToken))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("STORE_FORBIDDEN"));
    }

    // ── Copy endpoint ──────────────────────────────────────────────────

    @Test
    void copyGroupBuy_shouldSucceed() throws Exception {
        String createResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "被复制团购",
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

        mockMvc.perform(post(GROUP_BUYS_URL + "/" + groupBuyId + "/copy")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.groupBuy.status").value("draft"))
                .andExpect(jsonPath("$.data.groupBuy.title").value("被复制团购"));
    }

    // ── Permission endpoint ────────────────────────────────────────────

    @Test
    void updatePermission_shouldChangeToHidden() throws Exception {
        String createResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "权限修改",
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

        mockMvc.perform(patch(GROUP_BUYS_URL + "/" + groupBuyId + "/permission")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("{\"visibility\":\"hidden\"}"))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.groupBuy.visibility").value("hidden"));
    }

    // ── Share card endpoint ─────────────────────────────────────────────

    @Test
    void getShareCard_shouldSucceed() throws Exception {
        String createResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "分享测试",
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

        mockMvc.perform(post(GROUP_BUYS_URL + "/" + groupBuyId + "/share-card")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.shareToken").isString())
                .andExpect(jsonPath("$.data.landingPath").isString())
                .andExpect(jsonPath("$.data.title").value("分享测试"));
    }

    // ── Hidden group buy not in public list / detail ────────────────────

    @Test
    void publicList_shouldNotIncludeHidden() throws Exception {
        // Create a hidden group buy
        mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "隐藏团购",
                                    "visibility": "hidden",
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
                                """));

        // Public list should NOT contain the hidden group buy by title
        mockMvc.perform(get("/api/v1/group-buys"))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items[*].title").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("隐藏团购"))));
    }

    // ── Presale ─────────────────────────────────────────────────────────

    @Test
    void createPresaleGroupBuy_shouldSucceedWithTimes() throws Exception {
        mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "预售团购",
                                    "deliveryType": "express",
                                    "groupType": "presale",
                                    "startTime": "2026-07-10T12:00:00+08:00",
                                    "endTime": "2026-07-20T12:00:00+08:00",
                                    "shippingTime": "2026-07-30T12:00:00+08:00",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "预售商品",
                                                "basePriceAmount": 2000,
                                                "stock": 50
                                            },
                                            "displayName": "预售商品",
                                            "groupPriceAmount": 1990,
                                            "groupStock": 50
                                        }
                                    ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.groupBuy.groupType").value("presale"))
                .andExpect(jsonPath("$.data.groupBuy.status").value("published"));
    }

    @Test
    void createPresaleGroupBuy_shouldFailWithoutShippingTime() throws Exception {
        mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "预售无发货",
                                    "deliveryType": "express",
                                    "groupType": "presale",
                                    "startTime": "2026-07-10T12:00:00+08:00",
                                    "endTime": "2026-07-20T12:00:00+08:00",
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
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }
}
