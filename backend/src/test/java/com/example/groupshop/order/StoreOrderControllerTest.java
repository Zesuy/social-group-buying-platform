package com.example.groupshop.order;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for StoreOrderController endpoints.
 */
class StoreOrderControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String STORES_URL = "/api/v1/stores";
    private static final String GROUP_BUYS_URL = "/api/v1/my/store/group-buys";
    private static final String ADDRESSES_URL = "/api/v1/my/addresses";
    private static final String ORDERS_URL = "/api/v1/orders";
    private static final String STORE_ORDERS_URL = "/api/v1/my/store/orders";

    private String buyerToken;
    private String leaderToken;
    private Long orderId;

    @BeforeEach
    void setUp() throws Exception {
        buyerToken = loginAndGetToken("13800010001");
        leaderToken = loginAndGetToken("13800010002");

        // Create store
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + leaderToken)
                .contentType("application/json")
                .content("{\"name\":\"发货店铺\",\"defaultDeliveryType\":\"express\"}"));

        // Create group buy
        String gbResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "发货测试团购",
                                    "deliveryType": "express",
                                    "startTime": "2026-06-24T12:00:00+08:00",
                                    "endTime": "2028-07-01T12:00:00+08:00",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "发货测试商品",
                                                "basePriceAmount": 2000,
                                                "stock": 100
                                            },
                                            "displayName": "发货测试商品",
                                            "groupPriceAmount": 1990,
                                            "groupStock": 100
                                        }
                                    ]
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long groupBuyId = Long.parseLong(gbResponse.split("\"groupBuy\":\\{\"id\":")[1].split(",")[0].replace("\"", "").trim());
        Long groupBuyItemId = Long.parseLong(gbResponse.split("\"items\":\\[\\{\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Create address for buyer
        String addrResponse = mockMvc.perform(post(ADDRESSES_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "receiverName": "收货人",
                                    "receiverPhone": "13800000003",
                                    "province": "浙江省",
                                    "city": "杭州市",
                                    "district": "西湖区",
                                    "detail": "某某路 2 号"
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long addressId = Long.parseLong(addrResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Create order as buyer
        String orderResponse = mockMvc.perform(post(ORDERS_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "groupBuyId": "%s",
                                    "addressId": "%s",
                                    "items": [{"groupBuyItemId": "%s", "quantity": 1}]
                                }
                                """.formatted(groupBuyId, addressId, groupBuyItemId)))
                .andReturn().getResponse().getContentAsString();
        orderId = Long.parseLong(orderResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Pay the order
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

    // ── GET /api/v1/my/store/orders ───────────────────────────────────────

    @Test
    void listStoreOrders_shouldSucceed() throws Exception {
        mockMvc.perform(get(STORE_ORDERS_URL)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    void listStoreOrders_shouldFilterByKeyword() throws Exception {
        mockMvc.perform(get(STORE_ORDERS_URL)
                        .param("keyword", "收货人")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(orderId)));
    }

    @Test
    void listStoreOrders_shouldFailForNonLeader() throws Exception {
        mockMvc.perform(get(STORE_ORDERS_URL)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden())
                .andExpect(contractResult())
                .andExpectAll(errorResult("LEADER_REQUIRED"));
    }

    // ── GET /api/v1/my/store/orders/{orderId} ─────────────────────────────

    @Test
    void getStoreOrder_shouldSucceed() throws Exception {
        mockMvc.perform(get(STORE_ORDERS_URL + "/" + orderId)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.id").value(String.valueOf(orderId)))
                .andExpect(jsonPath("$.data.payStatus").value("paid"));
    }

    @Test
    void getStoreOrder_shouldFailForNonLeader() throws Exception {
        mockMvc.perform(get(STORE_ORDERS_URL + "/" + orderId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden())
                .andExpect(contractResult())
                .andExpectAll(errorResult("LEADER_REQUIRED"));
    }

    // ── POST /api/v1/my/store/orders/{orderId}/ship ───────────────────────

    @Test
    void shipOrder_shouldSucceed() throws Exception {
        mockMvc.perform(post(STORE_ORDERS_URL + "/" + orderId + "/ship")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "deliveryType": "express",
                                    "logisticsCompany": "顺丰速运",
                                    "trackingNo": "SF123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.order.id").value(String.valueOf(orderId)))
                .andExpect(jsonPath("$.data.order.orderStatus").value("shipped"))
                .andExpect(jsonPath("$.data.order.shippedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.shipment.id").isString())
                .andExpect(jsonPath("$.data.shipment.deliveryType").value("express"))
                .andExpect(jsonPath("$.data.shipment.logisticsCompany").value("顺丰速运"))
                .andExpect(jsonPath("$.data.shipment.trackingNo").value("SF123456"));
    }

    @Test
    void shipOrder_shouldFailForNonLeader() throws Exception {
        mockMvc.perform(post(STORE_ORDERS_URL + "/" + orderId + "/ship")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("{\"deliveryType\": \"express\"}"))
                .andExpect(status().isForbidden())
                .andExpect(contractResult())
                .andExpectAll(errorResult("LEADER_REQUIRED"));
    }

    @Test
    void shipOrder_shouldFailWhenAlreadyShipped() throws Exception {
        // Ship first
        mockMvc.perform(post(STORE_ORDERS_URL + "/" + orderId + "/ship")
                .header("Authorization", "Bearer " + leaderToken)
                .contentType("application/json")
                .content("{\"deliveryType\": \"express\"}"));

        // Ship again
        mockMvc.perform(post(STORE_ORDERS_URL + "/" + orderId + "/ship")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("{\"deliveryType\": \"express\"}"))
                .andExpect(status().isConflict())
                .andExpect(contractResult())
                .andExpectAll(errorResult("ORDER_ALREADY_SHIPPED"));
    }

    @Test
    void shipOrder_shouldFailWhenOrderNotPaid() throws Exception {
        // Create another store for a new order
        String otherToken = loginAndGetToken("13800010003");
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + otherToken)
                .contentType("application/json")
                .content("{\"name\":\"其他店铺\",\"defaultDeliveryType\":\"express\"}"));

        // Use buyer as leader since buyer is not a leader initially
        // Instead, create a new unpaid order under the leader's store
        // Create a 2nd group buy
        String gb2Response = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "未支付团购",
                                    "deliveryType": "express",
                                    "startTime": "2026-06-24T12:00:00+08:00",
                                    "endTime": "2028-07-01T12:00:00+08:00",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "未支付商品",
                                                "basePriceAmount": 1000,
                                                "stock": 100
                                            },
                                            "displayName": "未支付商品",
                                            "groupPriceAmount": 990,
                                            "groupStock": 100
                                        }
                                    ]
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long gb2Id = Long.parseLong(gb2Response.split("\"groupBuy\":\\{\"id\":")[1].split(",")[0].replace("\"", "").trim());
        Long gb2ItemId = Long.parseLong(gb2Response.split("\"items\":\\[\\{\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Create address for buyer
        String addr2Response = mockMvc.perform(post(ADDRESSES_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "receiverName": "收货人",
                                    "receiverPhone": "13800000004",
                                    "province": "浙江省",
                                    "city": "杭州市",
                                    "district": "西湖区",
                                    "detail": "某某路 3 号"
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long addr2Id = Long.parseLong(addr2Response.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Create unpaid order
        String order2Response = mockMvc.perform(post(ORDERS_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "groupBuyId": "%s",
                                    "addressId": "%s",
                                    "items": [{"groupBuyItemId": "%s", "quantity": 1}]
                                }
                                """.formatted(gb2Id, addr2Id, gb2ItemId)))
                .andReturn().getResponse().getContentAsString();
        Long unpaidOrderId = Long.parseLong(order2Response.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Try to ship unpaid order
        mockMvc.perform(post(STORE_ORDERS_URL + "/" + unpaidOrderId + "/ship")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("{\"deliveryType\": \"express\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(contractResult())
                .andExpectAll(errorResult("ORDER_NOT_SHIPPABLE"));
    }
}
