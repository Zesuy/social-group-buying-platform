package com.example.groupshop.category;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for CategoryController endpoints.
 */
class CategoryControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String CATEGORIES_URL = "/api/v1/categories";

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        userToken = loginAndGetToken("13800005001");
    }

    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }

    @Test
    void listCategories_shouldReturn6Categories() throws Exception {
        mockMvc.perform(get(CATEGORIES_URL)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[0].name").value("生鲜水果"))
                .andExpect(jsonPath("$.data[0].code").value("fresh_fruit"))
                .andExpect(jsonPath("$.data[1].name").value("蔬菜食品"))
                .andExpect(jsonPath("$.data[5].name").value("其他"));
    }

    @Test
    void listCategories_shouldSucceedWithValidToken() throws Exception {
        mockMvc.perform(get(CATEGORIES_URL)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void listCategories_shouldHaveCorrectStructure() throws Exception {
        mockMvc.perform(get(CATEGORIES_URL)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data[0].id").isString())
                .andExpect(jsonPath("$.data[0].name").isString())
                .andExpect(jsonPath("$.data[0].code").isString())
                .andExpect(jsonPath("$.data[0].parentId").doesNotExist())
                .andExpect(jsonPath("$.data[0].level").value(1))
                .andExpect(jsonPath("$.data[0].sortOrder").isNumber())
                .andExpect(jsonPath("$.data[0].status").value("active"));
    }
}
