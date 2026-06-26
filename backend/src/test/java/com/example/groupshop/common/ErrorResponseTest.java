package com.example.groupshop.common;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for unified error response format.
 * Covers: health check success response structure.
 */
class ErrorResponseTest extends MockMvcTestBase {

    @Test
    void healthCheck_shouldReturnSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(contractResult())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }
}
