package com.example.groupshop.common.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DistanceCalculator}.
 */
class DistanceCalculatorTest {

    // ── Haversine formula ────────────────────────────────────────────

    @Test
    void haversineMeters_shouldReturnNullForNullInput() {
        assertThat(DistanceCalculator.haversineMeters(null, null, null, null)).isNull();
        assertThat(DistanceCalculator.haversineMeters(
                BigDecimal.valueOf(30), null, BigDecimal.valueOf(31), BigDecimal.valueOf(121))).isNull();
        assertThat(DistanceCalculator.haversineMeters(
                BigDecimal.valueOf(30), BigDecimal.valueOf(120), null, BigDecimal.valueOf(121))).isNull();
    }

    @Test
    void haversineMeters_shouldReturnZeroForSamePoint() {
        Long distance = DistanceCalculator.haversineMeters(
                BigDecimal.valueOf(31.2304), BigDecimal.valueOf(121.4737),
                BigDecimal.valueOf(31.2304), BigDecimal.valueOf(121.4737));
        assertThat(distance).isZero();
    }

    @Test
    void haversineMeters_shouldCalculateTypicalCityDistance() {
        // Shanghai (31.2304, 121.4737) to Hangzhou (30.2741, 120.1551)
        // Expected ~165 km
        Long distance = DistanceCalculator.haversineMeters(
                BigDecimal.valueOf(31.2304), BigDecimal.valueOf(121.4737),
                BigDecimal.valueOf(30.2741), BigDecimal.valueOf(120.1551));
        assertThat(distance).isNotNull();
        // Should be between 150km and 180km
        assertThat(distance).isBetween(150_000L, 180_000L);
    }

    @Test
    void haversineMeters_shouldCalculateShortDistance() {
        // Two points ~100m apart in Shanghai
        // Point A: (31.2304, 121.4737)
        // Point B: ~0.001 degrees north ≈ 111m
        Long distance = DistanceCalculator.haversineMeters(
                BigDecimal.valueOf(31.2304), BigDecimal.valueOf(121.4737),
                BigDecimal.valueOf(31.2313), BigDecimal.valueOf(121.4737));
        assertThat(distance).isNotNull();
        // Should be around 100m
        assertThat(distance).isBetween(90L, 120L);
    }

    @Test
    void haversineMeters_shouldHandleNullCoordinates() {
        // One coordinate null, others valid
        assertThat(DistanceCalculator.haversineMeters(
                BigDecimal.valueOf(31.2304), BigDecimal.valueOf(121.4737),
                null, BigDecimal.valueOf(120.1551))).isNull();

        assertThat(DistanceCalculator.haversineMeters(
                BigDecimal.valueOf(31.2304), BigDecimal.valueOf(121.4737),
                BigDecimal.valueOf(30.2741), null)).isNull();
    }

    // ── formatDistance ───────────────────────────────────────────────

    @Test
    void formatDistance_shouldReturnNullForNullInput() {
        assertThat(DistanceCalculator.formatDistance(null)).isNull();
    }

    @Test
    void formatDistance_shouldFormatMeters() {
        assertThat(DistanceCalculator.formatDistance(0L)).isEqualTo("0m");
        assertThat(DistanceCalculator.formatDistance(500L)).isEqualTo("500m");
        assertThat(DistanceCalculator.formatDistance(999L)).isEqualTo("999m");
    }

    @Test
    void formatDistance_shouldFormatKilometers() {
        assertThat(DistanceCalculator.formatDistance(1000L)).isEqualTo("1km");
        assertThat(DistanceCalculator.formatDistance(1500L)).isEqualTo("1.5km");
        assertThat(DistanceCalculator.formatDistance(1200L)).isEqualTo("1.2km");
        assertThat(DistanceCalculator.formatDistance(10000L)).isEqualTo("10km");
        assertThat(DistanceCalculator.formatDistance(12345L)).isEqualTo("12.3km");
    }

    @Test
    void formatDistance_shouldStripTrailingZero() {
        // 1000 → "1km", not "1.0km"
        assertThat(DistanceCalculator.formatDistance(1000L)).isEqualTo("1km");
        assertThat(DistanceCalculator.formatDistance(2000L)).isEqualTo("2km");
        assertThat(DistanceCalculator.formatDistance(1500L)).isEqualTo("1.5km");
    }
}
