package com.example.groupshop.aftersale;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for after-sale endpoints (buyer + store).
 *
 * <p>Covers: apply, duplicate apply, buyer list/detail, store list/detail,
 * approve, reject, complete refund, duplicate complete refund,
 * unauthorised, non-order user, non-leader, cross-store reject.
 */
class AfterSaleControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";

    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }
    private static final String STORES_URL = "/api/v1/stores";
    private static final String GROUP_BUYS_URL = "/api/v1/my/store/group-buys";
    private static final String ADDRESSES_URL = "/api/v1/my/addresses";
    private static final String ORDERS_URL = "/api/v1/orders";
    private static final String MY_ORDERS_URL = "/api/v1/my/orders";
    private static final String AFTER_SALES_URL = "/api/v1/my/after-sales";
    private static final String STORE_AFTER_SALES_URL = "/api/v1/my/store/after-sales";

    private String buyerToken;
    private String leaderToken;
    private Long orderId;

    @BeforeEach
    void setUp() throws Exception {
        buyerToken = loginAndGetToken("13800030001");
        leaderToken = loginAndGetToken("13800030002");

        // Create store
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + leaderToken)
                .contentType("application/json")
                .content("{\"name\":\"售后测试店铺\",\"defaultDeliveryType\":\"express\"}"));

        // Create group buy
        String gbResponse = mockMvc.perform(post(GROUP_BUYS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "title": "售后测试团购",
                                    "deliveryType": "express",
                                    "startTime": "2026-06-24T12:00:00+08:00",
                                    "endTime": "2028-07-01T12:00:00+08:00",
                                    "items": [
                                        {
                                            "product": {
                                                "name": "售后商品",
                                                "basePriceAmount": 2000,
                                                "stock": 100
                                            },
                                            "displayName": "售后商品",
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
                                    "receiverName": "售后买家",
                                    "receiverPhone": "13800000001",
                                    "province": "浙江省",
                                    "city": "杭州市",
                                    "district": "西湖区",
                                    "detail": "某某路 1 号"
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long addressId = Long.parseLong(addrResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Create and pay order
        String orderResponse = mockMvc.perform(post(ORDERS_URL)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "groupBuyId": "%s",
                                    "addressId": "%s",
                                    "items": [{"groupBuyItemId": "%s", "quantity": 2}]
                                }
                                """.formatted(groupBuyId, addressId, groupBuyItemId)))
                .andReturn().getResponse().getContentAsString();
        orderId = Long.parseLong(orderResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Pay the order
        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/simulate-pay")
                .header("Authorization", "Bearer " + buyerToken));
    }

    // ── Buyer: Apply After-Sale ──────────────────────────────────────────

    @Test
    void applyAfterSale_shouldSucceed() throws Exception {
        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("{\"type\":\"refund\",\"reason\":\"商品质量问题\"}"))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.type").value("refund"))
                .andExpect(jsonPath("$.data.amount").isNumber())
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    void applyAfterSale_shouldFailWhenDuplicate() throws Exception {
        // First application
        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                .header("Authorization", "Bearer " + buyerToken)
                .contentType("application/json")
                .content("{\"type\":\"refund\",\"reason\":\"质量问题\"}"));

        // Second application should fail
        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("{\"type\":\"refund\",\"reason\":\"再次申请\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void applyAfterSale_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                        .contentType("application/json")
                        .content("{\"type\":\"refund\",\"reason\":\"问题\"}"))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void applyAfterSale_shouldFailWhenNotOwnOrder() throws Exception {
        String otherToken = loginAndGetToken("13800030003");

        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType("application/json")
                        .content("{\"type\":\"refund\",\"reason\":\"问题\"}"))
                .andExpect(status().isNotFound())
                .andExpectAll(errorResult("RESOURCE_NOT_FOUND"));
    }

    // ── Buyer: List / Detail ─────────────────────────────────────────────

    @Test
    void listMyAfterSales_shouldSucceed() throws Exception {
        // Create an after-sale first
        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                .header("Authorization", "Bearer " + buyerToken)
                .contentType("application/json")
                .content("{\"type\":\"refund\",\"reason\":\"质量问题\"}"));

        mockMvc.perform(get(AFTER_SALES_URL)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    void getMyAfterSale_shouldSucceed() throws Exception {
        String createResponse = mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("{\"type\":\"refund\",\"reason\":\"质量问题\"}"))
                .andReturn().getResponse().getContentAsString();
        Long afterSaleId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(get(AFTER_SALES_URL + "/" + afterSaleId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.id").value(String.valueOf(afterSaleId)))
                .andExpect(jsonPath("$.data.orderNo").isString())
                .andExpect(jsonPath("$.data.orderStatus").isString());
    }

    // ── Store: List / Detail ─────────────────────────────────────────────

    @Test
    void listStoreAfterSales_shouldSucceed() throws Exception {
        // Create an after-sale first
        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                .header("Authorization", "Bearer " + buyerToken)
                .contentType("application/json")
                .content("{\"type\":\"refund\",\"reason\":\"问题\"}"));

        mockMvc.perform(get(STORE_AFTER_SALES_URL)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void listStoreAfterSales_shouldFilterByStatus() throws Exception {
        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                .header("Authorization", "Bearer " + buyerToken)
                .contentType("application/json")
                .content("{\"type\":\"refund\",\"reason\":\"问题\"}"));

        mockMvc.perform(get(STORE_AFTER_SALES_URL)
                        .param("status", "pending")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items[0].status").value("pending"));
    }

    @Test
    void getStoreAfterSale_shouldSucceed() throws Exception {
        String createResponse = mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("{\"type\":\"refund\",\"reason\":\"质量问题\"}"))
                .andReturn().getResponse().getContentAsString();
        Long afterSaleId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(get(STORE_AFTER_SALES_URL + "/" + afterSaleId)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.id").value(String.valueOf(afterSaleId)));
    }

    // ── Store: Approve ───────────────────────────────────────────────────

    @Test
    void approveAfterSale_shouldSucceed() throws Exception {
        String createResponse = mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("{\"type\":\"refund\",\"reason\":\"问题\"}"))
                .andReturn().getResponse().getContentAsString();
        Long afterSaleId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(post(STORE_AFTER_SALES_URL + "/" + afterSaleId + "/approve")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.approvedAt").isNotEmpty());
    }

    // ── Store: Reject ────────────────────────────────────────────────────

    @Test
    void rejectAfterSale_shouldSucceed() throws Exception {
        String createResponse = mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("{\"type\":\"refund\",\"reason\":\"问题\"}"))
                .andReturn().getResponse().getContentAsString();
        Long afterSaleId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(post(STORE_AFTER_SALES_URL + "/" + afterSaleId + "/reject")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("{\"rejectReason\":\"商品完好，不予退款\"}"))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.status").value("rejected"))
                .andExpect(jsonPath("$.data.rejectReason").value("商品完好，不予退款"));
    }

    // ── Store: Complete Refund ───────────────────────────────────────────

    @Test
    void completeRefund_shouldSucceed() throws Exception {
        // Create → approve → complete refund
        String createResponse = mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("{\"type\":\"refund\",\"reason\":\"问题\"}"))
                .andReturn().getResponse().getContentAsString();
        Long afterSaleId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Approve
        mockMvc.perform(post(STORE_AFTER_SALES_URL + "/" + afterSaleId + "/approve")
                .header("Authorization", "Bearer " + leaderToken));

        // Complete refund
        mockMvc.perform(post(STORE_AFTER_SALES_URL + "/" + afterSaleId + "/complete-refund")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.status").value("completed"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());
    }

    @Test
    void completeRefund_shouldBeIdempotent() throws Exception {
        String createResponse = mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("{\"type\":\"refund\",\"reason\":\"问题\"}"))
                .andReturn().getResponse().getContentAsString();
        Long afterSaleId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Approve
        mockMvc.perform(post(STORE_AFTER_SALES_URL + "/" + afterSaleId + "/approve")
                .header("Authorization", "Bearer " + leaderToken));

        // First complete-refund
        mockMvc.perform(post(STORE_AFTER_SALES_URL + "/" + afterSaleId + "/complete-refund")
                .header("Authorization", "Bearer " + leaderToken))
                .andExpect(jsonPath("$.data.status").value("completed"));

        // Second complete-refund — idempotent
        mockMvc.perform(post(STORE_AFTER_SALES_URL + "/" + afterSaleId + "/complete-refund")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.status").value("completed"));
    }

    // ── Cross-store reject ───────────────────────────────────────────────

    @Test
    void rejectAfterSale_shouldFailForCrossStore() throws Exception {
        // Create admin for another store
        String otherLeaderToken = loginAndGetToken("13800030004");

        // Create another store
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + otherLeaderToken)
                .contentType("application/json")
                .content("{\"name\":\"其他店铺\",\"defaultDeliveryType\":\"express\"}"));

        // Create after-sale on first store's order
        String createResponse = mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("{\"type\":\"refund\",\"reason\":\"问题\"}"))
                .andReturn().getResponse().getContentAsString();
        Long afterSaleId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Other store leader tries to reject
        mockMvc.perform(post(STORE_AFTER_SALES_URL + "/" + afterSaleId + "/reject")
                        .header("Authorization", "Bearer " + otherLeaderToken)
                        .contentType("application/json")
                        .content("{\"rejectReason\":\"无权操作\"}"))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("STORE_FORBIDDEN"));
    }

    // ── Non-leader access ────────────────────────────────────────────────

    @Test
    void storeEndpoints_shouldFailForNonLeader() throws Exception {
        String createResponse = mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("{\"type\":\"refund\",\"reason\":\"问题\"}"))
                .andReturn().getResponse().getContentAsString();
        Long afterSaleId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Buyer (non-leader) tries store endpoints
        mockMvc.perform(get(STORE_AFTER_SALES_URL)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("LEADER_REQUIRED"));

        mockMvc.perform(post(STORE_AFTER_SALES_URL + "/" + afterSaleId + "/approve")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("LEADER_REQUIRED"));

        mockMvc.perform(post(STORE_AFTER_SALES_URL + "/" + afterSaleId + "/reject")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType("application/json")
                        .content("{\"rejectReason\":\"无权限\"}"))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("LEADER_REQUIRED"));

        mockMvc.perform(post(STORE_AFTER_SALES_URL + "/" + afterSaleId + "/complete-refund")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("LEADER_REQUIRED"));
    }

    // ── Order response includes afterSale ─────────────────────────────────

    @Test
    void orderDetail_shouldIncludeAfterSaleSummary() throws Exception {
        // Create after-sale
        mockMvc.perform(post(ORDERS_URL + "/" + orderId + "/after-sales")
                .header("Authorization", "Bearer " + buyerToken)
                .contentType("application/json")
                .content("{\"type\":\"refund\",\"reason\":\"质量问题\"}"));

        // Order detail should include afterSale summary
        mockMvc.perform(get(MY_ORDERS_URL + "/" + orderId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.afterSale").exists())
                .andExpect(jsonPath("$.data.afterSale.status").value("pending"))
                .andExpect(jsonPath("$.data.afterSale.type").value("refund"))
                .andExpect(jsonPath("$.data.afterSale.amount").isNumber());
    }

    @Test
    void orderDetail_afterSaleShouldBeNullWhenNotExists() throws Exception {
        // No after-sale created — afterSale should be absent/null
        mockMvc.perform(get(MY_ORDERS_URL + "/" + orderId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.afterSale").doesNotExist());
    }
}
