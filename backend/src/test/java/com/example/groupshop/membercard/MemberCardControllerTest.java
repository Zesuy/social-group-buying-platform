package com.example.groupshop.membercard;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for MemberCardController endpoints.
 */
class MemberCardControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String STORES_URL = "/api/v1/stores";
    private static final String ORDERS_URL = "/api/v1/orders";
    private static final String GROUP_BUYS_URL = "/api/v1/my/store/group-buys";
    private static final String ADDRESSES_URL = "/api/v1/my/addresses";

    private String buyerToken;
    private String leaderToken;

    @BeforeEach
    void setUp() throws Exception {
        buyerToken = loginAndGetToken("13800013001");
        leaderToken = loginAndGetToken("13800013002");

        // Create store
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + leaderToken)
                .contentType("application/json")
                .content("{\"name\":\"会员卡测试店铺\",\"defaultDeliveryType\":\"express\"}"));

        // Create group buy
        String gbResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "会员卡测试团购",
                                    "deliveryType": "express",
                                    "startTime": "2026-06-24T12:00:00+08:00",
                                    "endTime": "2028-07-01T12:00:00+08:00",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "会员卡测试商品",
                                                "basePriceAmount": 2000,
                                                "stock": 100
                                            },
                                            "displayName": "会员卡测试商品",
                                            "groupPriceAmount": 1990,
                                            "groupStock": 100
                                        }
                                    ]
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long groupBuyId = Long.parseLong(gbResponse.split("\"groupBuy\":\\{\"id\":")[1].split(",")[0]);
        Long groupBuyItemId = Long.parseLong(gbResponse.split("\"items\":\\[\\{\"id\":")[1].split(",")[0]);

        // Create address for buyer
        String addrResponse = mockMvc.perform(post(ADDRESSES_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "receiverName": "会员买家",
                                    "receiverPhone": "13800000005",
                                    "province": "浙江省",
                                    "city": "杭州市",
                                    "district": "西湖区",
                                    "detail": "某某路 4 号"
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long addressId = Long.parseLong(addrResponse.split("\"id\":")[1].split(",")[0]);

        // Create and pay an order (creates a member relation)
        String orderResponse = mockMvc.perform(post(ORDERS_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "groupBuyId": %d,
                                    "addressId": %d,
                                    "items": [{"groupBuyItemId": %d, "quantity": 1}]
                                }
                                """.formatted(groupBuyId, addressId, groupBuyItemId)))
                .andReturn().getResponse().getContentAsString();
        Long orderId = Long.parseLong(orderResponse.split("\"id\":")[1].split(",")[0]);

        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/simulate-pay")
                .header("Authorization", "Bearer " + buyerToken));
    }

    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }

    // ── GET /api/v1/my/member-cards ───────────────────────────────────────

    @Test
    void listMyMemberCards_shouldReturnCards() throws Exception {
        mockMvc.perform(get("/api/v1/my/member-cards")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].id").isNumber())
                .andExpect(jsonPath("$.data.items[0].levelName").value("V0"))
                .andExpect(jsonPath("$.data.items[0].growthValue").isNumber())
                .andExpect(jsonPath("$.data.items[0].totalOrderAmount").isNumber())
                .andExpect(jsonPath("$.data.items[0].totalOrders").isNumber())
                // Nested leader object
                .andExpect(jsonPath("$.data.items[0].leader.id").isNumber())
                .andExpect(jsonPath("$.data.items[0].leader.displayName").isString())
                // Nested store object
                .andExpect(jsonPath("$.data.items[0].store.id").isNumber())
                .andExpect(jsonPath("$.data.items[0].store.name").isString());
    }

    @Test
    void listMyMemberCards_shouldReturnEmptyWhenNoCards() throws Exception {
        String otherToken = loginAndGetToken("13800013003");

        mockMvc.perform(get("/api/v1/my/member-cards")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void listMyMemberCards_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/my/member-cards"))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }
}
