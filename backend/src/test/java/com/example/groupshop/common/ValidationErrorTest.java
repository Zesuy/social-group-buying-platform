package com.example.groupshop.common;

import com.example.groupshop.base.MockMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for validation error response.
 *
 * Covers:
 * - @Valid annotated request body validation failure
 * - Malformed JSON body error
 * - Unknown route Not Found error
 * - Consistent error response structure
 */
class ValidationErrorTest extends MockMvcTestBase {

    @Test
    void validRequestBody_shouldSucceed() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/_test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("test"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void emptyRequestBody_shouldReturnValidationError() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/_test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("参数校验失败"))
                .andExpect(jsonPath("$.error.details.name").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void malformedJsonBody_shouldReturnValidationError() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/_test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void unknownRoute_shouldReturnNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").exists());
    }
}
