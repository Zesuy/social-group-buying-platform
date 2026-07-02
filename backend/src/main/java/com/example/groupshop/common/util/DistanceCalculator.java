package com.example.groupshop.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Haversine formula utility for calculating distances between GPS coordinates.
 *
 * <p>All distances are returned in meters (rounded to the nearest integer).
 * Null or invalid inputs return {@code null}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    /**
     * Calculate the great-circle distance between two GPS points using the
     * Haversine formula.
     *
     * @param lat1  latitude of point 1 (degrees), or null
     * @param lon1  longitude of point 1 (degrees), or null
     * @param lat2  latitude of point 2 (degrees), or null
     * @param lon2  longitude of point 2 (degrees), or null
     * @return distance in meters (rounded to the nearest integer), or {@code null}
     *         if any coordinate is null or out of valid range
     */
    public static Long haversineMeters(BigDecimal lat1, BigDecimal lon1,
                                       BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }
        if (!isValidLatitude(lat1) || !isValidLatitude(lat2)
                || !isValidLongitude(lon1) || !isValidLongitude(lon2)) {
            return null;
        }

        double dLat = toRadians(lat1.doubleValue() - lat2.doubleValue());
        double dLon = toRadians(lon1.doubleValue() - lon2.doubleValue());
        double aLat1 = toRadians(lat1.doubleValue());
        double aLat2 = toRadians(lat2.doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(aLat1) * Math.cos(aLat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double meters = EARTH_RADIUS_METERS * c;
        return Math.round(meters);
    }

    /**
     * Format distance as a human-readable text string.
     *
     * <ul>
     *   <li>Less than 1000 m → "800m"</li>
     *   <li>1000 m or more → "1km", "1.2km" (one decimal, trailing ".0" stripped)</li>
     * </ul>
     *
     * @param distanceMeters distance in meters, or null
     * @return formatted text, or {@code null} if input is null
     */
    public static String formatDistance(Long distanceMeters) {
        if (distanceMeters == null) {
            return null;
        }
        if (distanceMeters < 1000) {
            return distanceMeters + "m";
        }
        double km = distanceMeters / 1000.0;
        String formatted = BigDecimal.valueOf(km)
                .setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
        return formatted + "km";
    }

    private static boolean isValidLatitude(BigDecimal lat) {
        return lat.compareTo(BigDecimal.valueOf(-90)) >= 0
                && lat.compareTo(BigDecimal.valueOf(90)) <= 0;
    }

    private static boolean isValidLongitude(BigDecimal lon) {
        return lon.compareTo(BigDecimal.valueOf(-180)) >= 0
                && lon.compareTo(BigDecimal.valueOf(180)) <= 0;
    }

    private static double toRadians(double degrees) {
        return Math.toRadians(degrees);
    }
}
