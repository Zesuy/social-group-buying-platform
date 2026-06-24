package com.example.groupshop.testconfig;

import com.example.groupshop.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * Test-only controller for verifying validation error response behavior.
 * Only active in the "test" profile.
 */
@Profile("test")
@RestController
@RequestMapping("/api/v1/_test")
public class TestValidationController {

    @PostMapping("/validate")
    public ApiResponse<TestResponse> validate(@Valid @RequestBody TestRequest request) {
        return ApiResponse.success(new TestResponse(request.getName()));
    }

    @Data
    public static class TestRequest {
        @NotBlank(message = "名称不能为空")
        private String name;
    }

    @Data
    @AllArgsConstructor
    public static class TestResponse {
        private String name;
    }
}
