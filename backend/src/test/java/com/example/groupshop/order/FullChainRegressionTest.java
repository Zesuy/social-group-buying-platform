package com.example.groupshop.order;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end MVP regression test for Batch 10.
 *
 * <p>Covers the full chain:
 * <ol>
 *   <li>mock login buyer + leader</li>
 *   <li>leader creates store and group buy</li>
 *   <li>buyer browses public list/detail, creates address, previews order, creates order, simulate-pays</li>
 *   <li>leader lists orders, gets detail, ships order</li>
 *   <li>buyer confirms receipt</li>
 *   <li>buyer subscribes/unsubscribes and checks member cards</li>
 * </ol>
 */
class FullChainRegressionTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String STORES_URL = "/api/v1/stores";
    private static final String GROUP_BUYS_URL = "/api/v1/my/store/group-buys";
    private static final String ADDRESSES_URL = "/api/v1/my/addresses";
    private static final String ORDER_PREVIEW_URL = "/api/v1/orders/preview";
    private static final String ORDERS_URL = "/api/v1/orders";
    private static final String MY_ORDERS_URL = "/api/v1/my/orders";
    private static final String STORE_ORDERS_URL = "/api/v1/my/store/orders";
    private static final String SUBSCRIPTION_URL = "/api/v1/leaders";
    private static final String MY_SUBSCRIPTIONS_URL = "/api/v1/my/subscriptions";
    private static final String MEMBER_CARDS_URL = "/api/v1/my/member-cards";

    private String buyerToken;
    private String leaderToken;
    private Long leaderId;
    private Long groupBuyId;
    private Long groupBuyItemId;
    private Long addressId;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Mock login
        buyerToken = loginAndGetToken("13800020001");
        leaderToken = loginAndGetToken("13800020002");

        // 2. Leader creates store
        String storeResponse = mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("{\"name\":\"全链路店铺\",\"defaultDeliveryType\":\"express\"}"))
                .andReturn().getResponse().getContentAsString();
        leaderId = Long.parseLong(storeResponse.split("\"leader\":\\{\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // 3. Leader creates group buy
        String gbResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "全链路测试团购",
                                    "deliveryType": "express",
                                    "startTime": "2026-06-24T12:00:00+08:00",
                                    "endTime": "2028-07-01T12:00:00+08:00",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "全链路商品",
                                                "basePriceAmount": 3000,
                                                "stock": 100
                                            },
                                            "displayName": "全链路商品",
                                            "groupPriceAmount": 2990,
                                            "groupStock": 50
                                        }
                                    ]
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        groupBuyId = Long.parseLong(gbResponse.split("\"groupBuy\":\\{\"id\":")[1].split(",")[0].replace("\"", "").trim());
        groupBuyItemId = Long.parseLong(gbResponse.split("\"items\":\\[\\{\"id\":")[1].split(",")[0].replace("\"", "").trim());
    }

    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }

    @Test
    void fullChainRegressionTest() throws Exception {
        // ── 3. Buyer browses public group buys ─────────────────────────
        mockMvc.perform(get("/api/v1/group-buys")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber());

        // Buyer browses group buy detail
        mockMvc.perform(get("/api/v1/group-buys/" + groupBuyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupBuy.id").value(String.valueOf(groupBuyId)));

        // ── 3. Buyer creates address ──────────────────────────────────
        String addrResponse = mockMvc.perform(post(ADDRESSES_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "receiverName": "全链路买家",
                                    "receiverPhone": "13800000099",
                                    "province": "浙江省",
                                    "city": "杭州市",
                                    "district": "西湖区",
                                    "detail": "全链路路 100 号"
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        addressId = Long.parseLong(addrResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // ── 3. Buyer previews order ───────────────────────────────────
        mockMvc.perform(post(ORDER_PREVIEW_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "groupBuyId": "%s",
                                    "addressId": "%s",
                                    "items": [
                                        {
                                            "groupBuyItemId": "%s",
                                            "quantity": 2
                                        }
                                    ]
                                }
                                """.formatted(groupBuyId, addressId, groupBuyItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(5980))
                .andExpect(jsonPath("$.data.payAmount").value(5980));

        // ── 3. Buyer creates order ────────────────────────────────────
        String orderResponse = mockMvc.perform(post(ORDERS_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "groupBuyId": "%s",
                                    "addressId": "%s",
                                    "remark": "全链路测试",
                                    "items": [
                                        {
                                            "groupBuyItemId": "%s",
                                            "quantity": 2
                                        }
                                    ]
                                }
                                """.formatted(groupBuyId, addressId, groupBuyItemId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long orderId = Long.parseLong(orderResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // ── 3. Buyer simulates payment ────────────────────────────────
        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/simulate-pay")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payStatus").value("paid"))
                .andExpect(jsonPath("$.data.orderStatus").value("paid"))
                .andExpect(jsonPath("$.data.paidAt").isNotEmpty());

        // ── 4. Leader lists store orders ──────────────────────────────
        mockMvc.perform(get(STORE_ORDERS_URL)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber());

        // ── 4. Leader gets store order detail ─────────────────────────
        mockMvc.perform(get(STORE_ORDERS_URL + "/" + orderId)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(String.valueOf(orderId)))
                .andExpect(jsonPath("$.data.orderStatus").value("paid"));

        // ── 4. Leader ships the order ─────────────────────────────────
        mockMvc.perform(post(STORE_ORDERS_URL + "/" + orderId + "/ship")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "deliveryType": "express",
                                    "logisticsCompany": "中通快递",
                                    "trackingNo": "ZT999888"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.orderStatus").value("shipped"))
                .andExpect(jsonPath("$.data.order.shippedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.shipment.id").isString())
                .andExpect(jsonPath("$.data.shipment.deliveryType").value("express"))
                .andExpect(jsonPath("$.data.shipment.logisticsCompany").value("中通快递"))
                .andExpect(jsonPath("$.data.shipment.trackingNo").value("ZT999888"));

        // ── 5. Buyer confirms receipt (complete) ──────────────────────
        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/complete")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("completed"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        // Verify buyer sees completed status in their orders
        mockMvc.perform(get(MY_ORDERS_URL + "/" + orderId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("completed"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        // ── 6. Buyer subscribes to leader ─────────────────────────────
        mockMvc.perform(post(SUBSCRIPTION_URL + "/" + leaderId + "/subscription")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("{\"source\": \"homepage\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active"));

        // Buyer checks subscriptions
        mockMvc.perform(get(MY_SUBSCRIPTIONS_URL)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].leaderId").value(String.valueOf(leaderId)));

        // Buyer unsubscribes
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/leaders/" + leaderId + "/subscription")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());

        // ── 6. Buyer checks member cards ─────────────────────────────
        mockMvc.perform(get(MEMBER_CARDS_URL)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].store.name").value("全链路店铺"));
    }
}
