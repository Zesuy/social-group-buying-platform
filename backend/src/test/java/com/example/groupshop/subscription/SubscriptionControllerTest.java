package com.example.groupshop.subscription;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for SubscriptionController endpoints.
 */
class SubscriptionControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String STORES_URL = "/api/v1/stores";

    private String userToken;
    private String leaderToken;
    private Long leaderId;

    @BeforeEach
    void setUp() throws Exception {
        userToken = loginAndGetToken("13800011001");
        leaderToken = loginAndGetToken("13800011002");

        // Create store (creates leader)
        String storeResponse = mockMvc.perform(post(STORES_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("{\"name\":\"订阅测试店铺\",\"defaultDeliveryType\":\"express\"}"))
                .andReturn().getResponse().getContentAsString();
        // Extract leaderId from me endpoint
        String meResponse = mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + leaderToken))
                .andReturn().getResponse().getContentAsString();
        leaderId = Long.parseLong(meResponse.split("\"leaderId\":")[1].split(",")[0]);
    }

    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }

    private static final String SUBSCRIPTION_PATH = "/api/v1/leaders/{leaderId}/subscription";

    // ── POST /api/v1/leaders/{leaderId}/subscription ──────────────────────

    @Test
    void subscribe_shouldSucceed() throws Exception {
        mockMvc.perform(post(SUBSCRIPTION_PATH, leaderId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content("{\"source\":\"homepage\"}"))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.leaderId").value(leaderId));
    }

    @Test
    void subscribe_shouldBeIdempotent() throws Exception {
        // Subscribe twice
        mockMvc.perform(post(SUBSCRIPTION_PATH, leaderId)
                .header("Authorization", "Bearer " + userToken)
                .contentType("application/json")
                .content("{\"source\":\"homepage\"}"));

        mockMvc.perform(post(SUBSCRIPTION_PATH, leaderId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content("{\"source\":\"homepage\"}"))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.status").value("active"));
    }

    @Test
    void subscribe_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post(SUBSCRIPTION_PATH, leaderId)
                        .contentType("application/json")
                        .content("{\"source\":\"homepage\"}"))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void subscribe_shouldFailWhenLeaderNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/leaders/99999/subscription")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content("{\"source\":\"homepage\"}"))
                .andExpect(status().isNotFound())
                .andExpect(contractResult())
                .andExpectAll(errorResult("RESOURCE_NOT_FOUND"));
    }

    // ── DELETE /api/v1/leaders/{leaderId}/subscription ────────────────────

    @Test
    void cancelSubscription_shouldSucceed() throws Exception {
        // Subscribe first
        mockMvc.perform(post(SUBSCRIPTION_PATH, leaderId)
                .header("Authorization", "Bearer " + userToken)
                .contentType("application/json")
                .content("{\"source\":\"homepage\"}"));

        // Cancel
        mockMvc.perform(delete(SUBSCRIPTION_PATH, leaderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void cancelSubscription_shouldBeIdempotent() throws Exception {
        // Cancel without subscribing first
        mockMvc.perform(delete(SUBSCRIPTION_PATH, leaderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── GET /api/v1/my/subscriptions ──────────────────────────────────────

    @Test
    void listMySubscriptions_shouldReturnActive() throws Exception {
        // Subscribe to the leader
        mockMvc.perform(post(SUBSCRIPTION_PATH, leaderId)
                .header("Authorization", "Bearer " + userToken)
                .contentType("application/json")
                .content("{\"source\":\"homepage\"}"));

        mockMvc.perform(get("/api/v1/my/subscriptions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].leaderId").value(leaderId));
    }

    @Test
    void listMySubscriptions_shouldReturnEmptyWhenNone() throws Exception {
        mockMvc.perform(get("/api/v1/my/subscriptions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void listMySubscriptions_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/my/subscriptions"))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }
}
