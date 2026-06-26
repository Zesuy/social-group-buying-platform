package com.example.groupshop.contract;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiContractTest {

    private static final Path CONTRACT_PATH = Path.of(
            "..", "docs", "openapi", "groupshop-api.yaml"
    ).toAbsolutePath().normalize();

    @Test
    void openApiContract_shouldBeLoadable() {
        assertThat(Files.exists(CONTRACT_PATH)).isTrue();

        OpenApiInteractionValidator validator = OpenApiInteractionValidator
                .createFor(CONTRACT_PATH.toString())
                .build();

        assertThat(validator).isNotNull();
    }
}
