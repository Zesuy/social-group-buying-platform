package com.example.groupshop.groupbuy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for creating a group buy draft.
 *
 * <p>Draft uses minimal validation: title, deliveryType, and at least one valid
 * item entry (productId or inline product + displayName) are required.
 * Times, prices, stock are optional and can be filled before publishing.
 */
@Data
public class CreateDraftGroupBuyRequest {

    @NotBlank
    @Size(min = 1, max = 128)
    private String title;

    private String introduction;

    private String coverImageUrl;

    @NotBlank
    private String deliveryType;

    private String shippingTime;
    private String startTime;
    private String endTime;

    /** default "normal" if not set */
    private String groupType;

    /** default "public" if not set */
    private String visibility;

    @Valid
    @Size(min = 1)
    private List<ItemEntry> items;

    @Data
    public static class ItemEntry {

        private Long productId;

        @Valid
        private InlineProduct product;

        @NotBlank
        @Size(min = 1, max = 128)
        private String displayName;

        /** nullable for draft */
        @Min(0)
        private Long groupPriceAmount;

        /** nullable for draft */
        @Min(0)
        private Integer groupStock;

        private Integer sortOrder;
    }

    @Data
    public static class InlineProduct {

        @NotBlank
        @Size(min = 1, max = 128)
        private String name;

        private String description;

        private String coverImageUrl;

        @Min(0)
        private Long basePriceAmount;

        @Min(0)
        private Integer stock;
    }
}
