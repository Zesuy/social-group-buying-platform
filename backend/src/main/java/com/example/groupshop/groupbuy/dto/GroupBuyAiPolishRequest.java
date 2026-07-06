package com.example.groupshop.groupbuy.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

/**
 * Context sent from the publish form for local AI-style copy polishing.
 */
@Data
public class GroupBuyAiPolishRequest {

    private String title;
    private String introduction;
    private String deliveryType;
    private String startTime;
    private String endTime;
    private String shippingTime;

    @Valid
    private List<ItemContext> items;

    @Data
    public static class ItemContext {
        private String productId;
        private String displayName;
        private Long groupPriceAmount;
        private Integer groupStock;
        private String description;
    }
}
