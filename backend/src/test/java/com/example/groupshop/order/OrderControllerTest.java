package com.example.groupshop.order;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for OrderController endpoints.
 */
class OrderControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String STORES_URL = "/api/v1/stores";
    private static final String GROUP_BUYS_URL = "/api/v1/my/store/group-buys";
    private static final String ADDRESSES_URL = "/api/v1/my/addresses";
    private static final String ORDER_PREVIEW_URL = "/api/v1/orders/preview";
    private static final String ORDERS_URL = "/api/v1/orders";
    private static final String MY_ORDERS_URL = "/api/v1/my/orders";

    private String buyerToken;
    private String leaderToken;
    private Long groupBuyId;
    private Long groupBuyItemId;
    private Long addressId;

    @BeforeEach
    void setUp() throws Exception {
        buyerToken = loginAndGetToken("13800009001");

        leaderToken = loginAndGetToken("13800009002");
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + leaderToken)
                .contentType("application/json")
                .content("{\"name\":\"订单测试店铺\",\"defaultDeliveryType\":\"express\"}"));

        // Create a group buy
        String gbResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "订单测试团购",
                                    "deliveryType": "express",
                                    "startTime": "2026-06-24T12:00:00+08:00",
                                    "endTime": "2028-07-01T12:00:00+08:00",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "测试商品A",
                                                "basePriceAmount": 2000,
                                                "stock": 100
                                            },
                                            "displayName": "测试商品A",
                                            "groupPriceAmount": 1990,
                                            "groupStock": 100
                                        }
                                    ]
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        groupBuyId = Long.parseLong(gbResponse.split("\"groupBuy\":\\{\"id\":")[1].split(",")[0]);
        groupBuyItemId = Long.parseLong(gbResponse.split("\"items\":\\[\\{\"id\":")[1].split(",")[0]);

        // Create address for buyer
        String addrResponse = mockMvc.perform(post(ADDRESSES_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "receiverName": "买家张三",
                                    "receiverPhone": "13800000001",
                                    "province": "浙江省",
                                    "city": "杭州市",
                                    "district": "西湖区",
                                    "detail": "某某路 1 号"
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        addressId = Long.parseLong(addrResponse.split("\"id\":")[1].split(",")[0]);
    }

    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }

    // ── POST /api/v1/orders/preview ─────────────────────────────────────

    @Test
    void previewOrder_shouldSucceed() throws Exception {
        mockMvc.perform(post(ORDER_PREVIEW_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "groupBuyId": %d,
                                    "addressId": %d,
                                    "items": [
                                        {
                                            "groupBuyItemId": %d,
                                            "quantity": 2
                                        }
                                    ]
                                }
                                """.formatted(groupBuyId, addressId, groupBuyItemId)))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.groupBuyId").value(groupBuyId))
                .andExpect(jsonPath("$.data.totalAmount").value(3980))
                .andExpect(jsonPath("$.data.discountAmount").value(0))
                .andExpect(jsonPath("$.data.payAmount").value(3980))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.address").exists());
    }

    @Test
    void previewOrder_shouldFailWhenGroupBuyEnded() throws Exception {
        // Create an ended group buy
        String gbResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "已结束团购",
                                    "deliveryType": "express",
                                    "startTime": "2026-06-24T12:00:00+08:00",
                                    "endTime": "2026-06-25T12:00:00+08:00",
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
        Long endedGbId = Long.parseLong(gbResponse.split("\"groupBuy\":\\{\"id\":")[1].split(",")[0]);
        Long endedItemId = Long.parseLong(gbResponse.split("\"items\":\\[\\{\"id\":")[1].split(",")[0]);

        mockMvc.perform(post(ORDER_PREVIEW_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "groupBuyId": %d,
                                    "addressId": %d,
                                    "items": [
                                        {
                                            "groupBuyItemId": %d,
                                            "quantity": 1
                                        }
                                    ]
                                }
                                """.formatted(endedGbId, addressId, endedItemId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpectAll(errorResult("GROUP_BUY_ENDED"));
    }

    @Test
    void previewOrder_shouldFailWhenAddressForbidden() throws Exception {
        mockMvc.perform(post(ORDER_PREVIEW_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "groupBuyId": %d,
                                    "addressId": %d,
                                    "items": [
                                        {
                                            "groupBuyItemId": %d,
                                            "quantity": 1
                                        }
                                    ]
                                }
                                """.formatted(groupBuyId, addressId, groupBuyItemId)))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("ADDRESS_FORBIDDEN"));
    }

    @Test
    void previewOrder_shouldFailWhenInsufficientStock() throws Exception {
        mockMvc.perform(post(ORDER_PREVIEW_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "groupBuyId": %d,
                                    "addressId": %d,
                                    "items": [
                                        {
                                            "groupBuyItemId": %d,
                                            "quantity": 999
                                        }
                                    ]
                                }
                                """.formatted(groupBuyId, addressId, groupBuyItemId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpectAll(errorResult("INSUFFICIENT_STOCK"));
    }

    // ── POST /api/v1/orders ─────────────────────────────────────────────

    @Test
    void createOrder_shouldSucceed() throws Exception {
        mockMvc.perform(post(ORDERS_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "groupBuyId": %d,
                                    "addressId": %d,
                                    "remark": "请尽快发货",
                                    "items": [
                                        {
                                            "groupBuyItemId": %d,
                                            "quantity": 1
                                        }
                                    ]
                                }
                                """.formatted(groupBuyId, addressId, groupBuyItemId)))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.orderNo").isString())
                .andExpect(jsonPath("$.data.totalAmount").value(1990))
                .andExpect(jsonPath("$.data.payAmount").value(1990))
                .andExpect(jsonPath("$.data.orderStatus").value("pendingPay"))
                .andExpect(jsonPath("$.data.payStatus").value("unpaid"))
                .andExpect(jsonPath("$.data.receiverName").value("买家张三"))
                .andExpect(jsonPath("$.data.fullAddress").isString())
                .andExpect(jsonPath("$.data.items[0].productName").value("测试商品A"))
                .andExpect(jsonPath("$.data.items[0].skuName").value(""))
                .andExpect(jsonPath("$.data.items[0].unitPriceAmount").value(1990))
                .andExpect(jsonPath("$.data.items[0].quantity").value(1))
                .andExpect(jsonPath("$.data.items[0].totalAmount").value(1990));
    }

    @Test
    void createOrder_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post(ORDERS_URL)
                        .contentType("application/json")
                        .content("{\"groupBuyId\":1,\"addressId\":1,\"items\":[]}"))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    // ── GET /api/v1/my/orders ───────────────────────────────────────────

    @Test
    void listMyOrders_shouldSucceed() throws Exception {
        // Create an order first
        mockMvc.perform(post(ORDERS_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "groupBuyId": %d,
                                    "addressId": %d,
                                    "items": [{"groupBuyItemId": %d, "quantity": 1}]
                                }
                                """.formatted(groupBuyId, addressId, groupBuyItemId)));

        mockMvc.perform(get(MY_ORDERS_URL)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    void listMyOrders_shouldFilterByStatus() throws Exception {
        mockMvc.perform(get(MY_ORDERS_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .param("status", "pendingPay"))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray());
    }

    // ── GET /api/v1/my/orders/{orderId} ─────────────────────────────────

    @Test
    void getMyOrderDetail_shouldSucceed() throws Exception {
        String createResponse = mockMvc.perform(post(ORDERS_URL)
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
        Long orderId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0]);

        mockMvc.perform(get(MY_ORDERS_URL + "/" + orderId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.items[0].productName").value("测试商品A"));
    }

    @Test
    void getMyOrderDetail_shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get(MY_ORDERS_URL + "/99999")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isNotFound())
                .andExpectAll(errorResult("RESOURCE_NOT_FOUND"));
    }

    // ── POST /api/v1/orders/{orderId}/cancel ────────────────────────────

    @Test
    void cancelOrder_shouldSucceed() throws Exception {
        String createResponse = mockMvc.perform(post(ORDERS_URL)
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
        Long orderId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0]);

        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.orderStatus").value("canceled"));
    }

    @Test
    void cancelOrder_shouldFailWhenAlreadyCanceled() throws Exception {
        String createResponse = mockMvc.perform(post(ORDERS_URL)
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
        Long orderId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0]);

        // Cancel first
        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/cancel")
                .header("Authorization", "Bearer " + buyerToken));

        // Cancel again should fail
        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpectAll(errorResult("ORDER_NOT_CANCELABLE"));
    }
}
