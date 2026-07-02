package com.example.groupshop.coupon;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for CouponController endpoints.
 */
class CouponControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String STORES_URL = "/api/v1/stores";
    private static final String STORE_COUPONS_URL = "/api/v1/my/store/coupons";
    private static final String MY_COUPONS_URL = "/api/v1/my/coupons";

    private static int testCounter = 0;

    private String leaderToken;
    private String buyerToken;

    // ── Helpers ──────────────────────────────────────────────────────

    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }

    /**
     * Create a store coupon with a unique name and return its ID.
     */
    private Long createUniqueCoupon() throws Exception {
        testCounter++;
        String name = "测试券" + testCounter;
        String response = mockMvc.perform(post(STORE_COUPONS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "%s",
                                    "couponType": "amount",
                                    "amount": 2000,
                                    "thresholdAmount": 10000,
                                    "totalQuantity": 100,
                                    "perUserLimit": 1,
                                    "startTime": "2026-07-01T00:00:00",
                                    "endTime": "2026-08-01T00:00:00"
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return Long.parseLong(response.split("\"id\":\"")[1].split("\"")[0]);
    }

    @BeforeEach
    void setUp() throws Exception {
        // Create leader with store
        String leaderPhone = "13800030001";
        leaderToken = loginAndGetToken(leaderPhone);

        mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "优惠券测试店铺",
                                    "defaultDeliveryType": "express"
                                }
                                """));

        // Create buyer
        String buyerPhone = "13800030002";
        buyerToken = loginAndGetToken(buyerPhone);
    }

    // ── Coupon CRUD ─────────────────────────────────────────────────────

    @Test
    void createCoupon_shouldSucceed() throws Exception {
        Long id = createUniqueCoupon();
    }

    @Test
    void createCoupon_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post(STORE_COUPONS_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "测试券",
                                    "couponType": "amount",
                                    "amount": 1000,
                                    "totalQuantity": 10,
                                    "startTime": "2026-07-01T00:00:00",
                                    "endTime": "2026-08-01T00:00:00"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void createCoupon_shouldFailWhenInvalidCouponType() throws Exception {
        mockMvc.perform(post(STORE_COUPONS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "测试券",
                                    "couponType": "invalid",
                                    "amount": 1000,
                                    "totalQuantity": 10,
                                    "startTime": "2026-07-01T00:00:00",
                                    "endTime": "2026-08-01T00:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void getStoreCoupons_shouldReturnList() throws Exception {
        createUniqueCoupon();

        mockMvc.perform(get(STORE_COUPONS_URL)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data[0].name").exists());
    }

    @Test
    void getStoreCoupons_shouldFailWhenNotStoreOwner() throws Exception {
        mockMvc.perform(get(STORE_COUPONS_URL)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("LEADER_REQUIRED"));
    }

    @Test
    void updateCoupon_shouldPartialUpdate() throws Exception {
        Long couponId = createUniqueCoupon();

        mockMvc.perform(patch(STORE_COUPONS_URL + "/" + couponId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "新名称",
                                    "amount": 3000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.name").value("新名称"))
                .andExpect(jsonPath("$.data.amount").value(3000));
    }

    @Test
    void disableCoupon_shouldSucceed() throws Exception {
        Long couponId = createUniqueCoupon();

        mockMvc.perform(post(STORE_COUPONS_URL + "/" + couponId + "/disable")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult());
    }

    // ── Claim Coupons ───────────────────────────────────────────────────

    @Test
    void claimCoupon_shouldSucceed() throws Exception {
        Long couponId = createUniqueCoupon();

        mockMvc.perform(post("/api/v1/coupons/" + couponId + "/claim")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.couponId").value(couponId))
                .andExpect(jsonPath("$.data.status").value("unused"));
    }

    @Test
    void claimCoupon_shouldFailWhenNotAuthenticated() throws Exception {
        Long couponId = createUniqueCoupon();

        mockMvc.perform(post("/api/v1/coupons/" + couponId + "/claim"))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void claimCoupon_shouldFailWhenAlreadyClaimed() throws Exception {
        Long couponId = createUniqueCoupon();
        // Claim once
        mockMvc.perform(post("/api/v1/coupons/" + couponId + "/claim")
                .header("Authorization", "Bearer " + buyerToken));

        // Claim again
        mockMvc.perform(post("/api/v1/coupons/" + couponId + "/claim")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isConflict())
                .andExpectAll(errorResult("COUPON_ALREADY_CLAIMED"));
    }

    @Test
    void claimCoupon_shouldFailWhenOutOfStock() throws Exception {
        // Create a coupon with positive quantity
        Long couponId = createUniqueCoupon();

        // Drain the stock by claiming it with a different user
        // First, create a second buyer to drain the stock faster
        String otherBuyerPhone = "13800030999";
        String otherBuyerToken = loginAndGetToken(otherBuyerPhone);

        // For simplicity, set claimedQuantity = totalQuantity directly via MockMvc
        // Actually, claim all stock: totalQuantity=100 with perUserLimit=1 means we need 100 users
        // Instead, use a coupon with totalQuantity=1
        testCounter++;
        String name = "一库存券" + testCounter;
        String resp = mockMvc.perform(post(STORE_COUPONS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "%s",
                                    "couponType": "amount",
                                    "amount": 500,
                                    "totalQuantity": 1,
                                    "perUserLimit": 1,
                                    "startTime": "2026-07-01T00:00:00",
                                    "endTime": "2026-08-01T00:00:00"
                                }
                                """.formatted(name)))
                .andReturn().getResponse().getContentAsString();
        Long singleCouponId = Long.parseLong(resp.split("\"id\":\"")[1].split("\"")[0]);

        // Claim the only stock
        mockMvc.perform(post("/api/v1/coupons/" + singleCouponId + "/claim")
                .header("Authorization", "Bearer " + buyerToken));

        // Try to claim again — should be out of stock
        mockMvc.perform(post("/api/v1/coupons/" + singleCouponId + "/claim")
                        .header("Authorization", "Bearer " + otherBuyerToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpectAll(errorResult("COUPON_OUT_OF_STOCK"));
    }

    // ── My Coupons ──────────────────────────────────────────────────────

    @Test
    void getMyCoupons_shouldReturnClaimedCoupons() throws Exception {
        Long couponId = createUniqueCoupon();
        mockMvc.perform(post("/api/v1/coupons/" + couponId + "/claim")
                .header("Authorization", "Bearer " + buyerToken));

        mockMvc.perform(get(MY_COUPONS_URL)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data[0].status").value("unused"));
    }

    @Test
    void getMyCoupons_shouldFilterByStatus() throws Exception {
        Long couponId = createUniqueCoupon();
        mockMvc.perform(post("/api/v1/coupons/" + couponId + "/claim")
                .header("Authorization", "Bearer " + buyerToken));

        mockMvc.perform(get(MY_COUPONS_URL + "?status=unused")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data[0].status").value("unused"));

        mockMvc.perform(get(MY_COUPONS_URL + "?status=used")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void getMyCoupons_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get(MY_COUPONS_URL))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    // ── Group Buy Coupons ───────────────────────────────────────────────

    @Test
    void getGroupBuyCoupons_shouldFailForNonExistentGroupBuy() throws Exception {
        mockMvc.perform(get("/api/v1/group-buys/99999/coupons"))
                .andExpect(status().isNotFound())
                .andExpectAll(errorResult("RESOURCE_NOT_FOUND"));
    }
}
