package com.example.groupshop.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.nio.file.Path;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Base class for MockMvc controller tests.
 * Subclasses should focus on request/response verification.
 *
 * The autowired MockMvc from {@link AutoConfigureMockMvc} is used directly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class MockMvcTestBase {

    private static final String OPENAPI_CONTRACT_PATH = Path.of(
            "..", "docs", "openapi", "groupshop-api.yaml"
    ).toAbsolutePath().normalize().toString();

    @Autowired
    protected MockMvc mockMvc;

    // ── Common response matchers ───────────────────────────────────

    /**
     * Assert that the response is a success with non-null data.
     */
    protected ResultMatcher successResult() {
        return result -> {
            jsonPath("$.success").value(true).match(result);
            jsonPath("$.traceId").exists().match(result);
        };
    }

    /**
     * Assert that the response is an error with the given code.
     */
    protected ResultMatcher errorResult(String code) {
        return result -> {
            jsonPath("$.success").value(false).match(result);
            jsonPath("$.error.code").value(code).match(result);
            jsonPath("$.error.message").exists().match(result);
            jsonPath("$.traceId").exists().match(result);
        };
    }

    /**
     * Assert that the request and response match the batch API contract.
     */
    protected ResultMatcher contractResult() {
        return openApi().isValid(OPENAPI_CONTRACT_PATH);
    }
}
