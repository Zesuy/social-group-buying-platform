package com.example.groupshop.product;

import com.example.groupshop.base.MockMvcTestBase;
import com.example.groupshop.common.enums.DeliveryType;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for ProductController endpoints.
 */
class ProductControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String PRODUCTS_URL = "/api/v1/my/store/products";
    private static final String STORES_URL = "/api/v1/stores";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaderMapper leaderMapper;

    @Autowired
    private StoreMapper storeMapper;

    private String leaderToken;
    private String regularUserToken;

    @BeforeEach
    void setUp() throws Exception {
        // Create a leader with store
        leaderToken = loginAndGetToken("13800005001");
        mockMvc.perform(post(STORES_URL)
                .header("Authorization", "Bearer " + leaderToken)
                .contentType("application/json")
                .content("{\"name\":\"测试店铺\",\"defaultDeliveryType\":\"express\"}"));

        // Create a regular user without store
        regularUserToken = loginAndGetToken("13800005002");
    }

    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }

    // ── POST /api/v1/my/store/products ────────────────────────────────

    @Test
    void createProduct_shouldSucceed() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "白玉蜜桃",
                                    "description": "山东蒙阴产地直发",
                                    "coverImageUrl": "https://example.com/product.png",
                                    "basePriceAmount": 2990,
                                    "stock": 100
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.storeId").isString())
                .andExpect(jsonPath("$.data.name").value("白玉蜜桃"))
                .andExpect(jsonPath("$.data.description").value("山东蒙阴产地直发"))
                .andExpect(jsonPath("$.data.basePriceAmount").value(2990))
                .andExpect(jsonPath("$.data.stock").value(100))
                .andExpect(jsonPath("$.data.status").value("active"));
    }

    @Test
    void createProduct_shouldFailWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "商品",
                                    "basePriceAmount": 1000,
                                    "stock": 10
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void createProduct_shouldFailWhenNotLeader() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL)
                        .header("Authorization", "Bearer " + regularUserToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "商品",
                                    "basePriceAmount": 1000,
                                    "stock": 10
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpectAll(errorResult("LEADER_REQUIRED"));
    }

    @Test
    void createProduct_shouldFailWhenNameBlank() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "",
                                    "basePriceAmount": 1000,
                                    "stock": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void createProduct_shouldFailWhenBasePriceAmountNegative() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "商品",
                                    "basePriceAmount": -1,
                                    "stock": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void createProduct_shouldFailWhenStockNegative() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "商品",
                                    "basePriceAmount": 1000,
                                    "stock": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    // ── GET /api/v1/my/store/products ─────────────────────────────────

    @Test
    void listProducts_shouldSucceed() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.hasMore").isBoolean());
    }

    // ── GET /api/v1/my/store/products/{productId} ─────────────────────

    @Test
    void getProductDetail_shouldSucceed() throws Exception {
        // Create a product first
        String createResponse = mockMvc.perform(post(PRODUCTS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "详情测试商品",
                                    "basePriceAmount": 2990,
                                    "stock": 50
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long productId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(get(PRODUCTS_URL + "/" + productId)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.id").value(String.valueOf(productId)))
                .andExpect(jsonPath("$.data.name").value("详情测试商品"))
                .andExpect(jsonPath("$.data.status").value("active"));
    }

    @Test
    void getProductDetail_shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL + "/99999")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isNotFound())
                .andExpectAll(errorResult("RESOURCE_NOT_FOUND"));
    }

    // ── PATCH /api/v1/my/store/products/{productId} ───────────────────

    @Test
    void updateProduct_shouldSucceed() throws Exception {
        // Create a product first
        String createResponse = mockMvc.perform(post(PRODUCTS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "原始名称",
                                    "description": "原始简介",
                                    "basePriceAmount": 1000,
                                    "stock": 10
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long productId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        mockMvc.perform(patch(PRODUCTS_URL + "/" + productId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "新名称",
                                    "basePriceAmount": 2000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.name").value("新名称"))
                .andExpect(jsonPath("$.data.basePriceAmount").value(2000))
                .andExpect(jsonPath("$.data.description").value("原始简介"))
                .andExpect(jsonPath("$.data.stock").value(10))
                .andExpect(jsonPath("$.data.status").value("active"));
    }

    @Test
    void updateProduct_shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(patch(PRODUCTS_URL + "/99999")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {"name": "新名称"}
                                """))
                .andExpect(status().isNotFound())
                .andExpectAll(errorResult("RESOURCE_NOT_FOUND"));
    }

    // ── DELETE /api/v1/my/store/products/{productId} ──────────────────

    @Test
    void deleteProduct_shouldSucceed() throws Exception {
        // Create a product first
        String createResponse = mockMvc.perform(post(PRODUCTS_URL)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "待删除商品",
                                    "basePriceAmount": 1000,
                                    "stock": 10
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        Long productId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0].replace("\"", "").trim());

        // Delete
        mockMvc.perform(delete(PRODUCTS_URL + "/" + productId)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpect(jsonPath("$.success").value(true));

        // Verify product is soft-deleted via detail endpoint
        mockMvc.perform(get(PRODUCTS_URL + "/" + productId)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(jsonPath("$.data.status").value("deleted"));
    }

    @Test
    void deleteProduct_shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(delete(PRODUCTS_URL + "/99999")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isNotFound())
                .andExpectAll(errorResult("RESOURCE_NOT_FOUND"));
    }
}
