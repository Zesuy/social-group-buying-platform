package com.example.groupshop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Jackson Long ID serialization configuration.
 * <p>
 * Verifies that Long fields named {@code "id"} or ending with {@code "Id"}
 * are serialized as JSON strings, while other Long fields (amounts, counts)
 * remain as JSON numbers.
 * <p>
 * Key requirement: a snowflake ID exceeding JavaScript's
 * {@code Number.MAX_SAFE_INTEGER} (9,007,199,254,740,991) must be serialized
 * as a string to prevent frontend precision loss.
 */
@SpringBootTest
@ActiveProfiles("test")
class JacksonLongIdSerializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * A DTO that mirrors real API response shape — ID-like Long fields
     * alongside amount-like Long fields.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestDto {
        private Long id;
        private Long storeId;
        private Long leaderId;
        private Long totalAmount;
        private Long payAmount;
    }

    /**
     * A snowflake ID typical of MyBatis-Plus ASSIGN_ID, well beyond
     * JavaScript's safe integer range.
     */
    private static final long SNOWFLAKE_ID = 2071642363297644546L;
    private static final long AMOUNT_VALUE = 2990L;

    @Test
    void idField_shouldSerializeAsString() throws Exception {
        TestDto dto = TestDto.builder()
                .id(SNOWFLAKE_ID)
                .storeId(SNOWFLAKE_ID + 1)
                .leaderId(SNOWFLAKE_ID + 2)
                .totalAmount(AMOUNT_VALUE)
                .payAmount(AMOUNT_VALUE)
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":\"2071642363297644546\"");
        assertThat(json).contains("\"storeId\":\"2071642363297644547\"");
        assertThat(json).contains("\"leaderId\":\"2071642363297644548\"");
    }

    @Test
    void amountField_shouldSerializeAsNumber() throws Exception {
        TestDto dto = TestDto.builder()
                .id(SNOWFLAKE_ID)
                .totalAmount(AMOUNT_VALUE)
                .payAmount(AMOUNT_VALUE)
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"totalAmount\":2990");
        assertThat(json).contains("\"payAmount\":2990");
    }

    @Test
    void largeId_shouldNotLosePrecision() throws Exception {
        // This is the exact value from the bug report
        long problematicId = 2071642363297644546L;
        // Verify it's beyond Number.MAX_SAFE_INTEGER
        assertThat(problematicId).isGreaterThan(9007199254740991L);

        TestDto dto = TestDto.builder()
                .id(problematicId)
                .build();

        String json = objectMapper.writeValueAsString(dto);

        // The string representation must be exact, not a rounded number.
        // Null fields are omitted per default-property-inclusion: non_null.
        assertThat(json).isEqualTo("{\"id\":\"2071642363297644546\"}");
    }
}
