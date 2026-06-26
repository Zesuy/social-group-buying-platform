package com.example.groupshop.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Delivery type enum mapping API string values: {@code express}, {@code pickup}, {@code local_delivery}.
 *
 * <p>Request bodies use this enum (serialized via {@link JsonValue} / {@link JsonCreator}),
 * while the entity field {@code Store.defaultDeliveryType} stores the raw String for flexibility.
 */
@Getter
@AllArgsConstructor
public enum DeliveryType {

    EXPRESS("express"),
    PICKUP("pickup"),
    LOCAL_DELIVERY("local_delivery");

    private final String value;

    /**
     * Serialize to the API string value.
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Deserialize from the API string value.
     */
    @JsonCreator
    public static DeliveryType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (DeliveryType dt : values()) {
            if (dt.value.equals(value)) {
                return dt;
            }
        }
        throw new IllegalArgumentException("Unknown delivery type: " + value);
    }
}
