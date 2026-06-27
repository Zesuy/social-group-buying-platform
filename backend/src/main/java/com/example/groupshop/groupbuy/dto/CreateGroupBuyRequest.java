package com.example.groupshop.groupbuy.dto;

import com.example.groupshop.common.enums.DeliveryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for creating and publishing a group buy.
 *
 * <p>Each item entry can either {@code productId} (reuse an existing product)
 * or {@code product} (inline-create a new product in the same transaction).
 */
@Data
public class CreateGroupBuyRequest {

    @NotBlank
    @Size(min = 1, max = 128)
    private String title;

    private String introduction;

    private String coverImageUrl;

    @NotNull
    private DeliveryType deliveryType;

    private String shippingTime;
    private String startTime;
    private String endTime;

    @NotEmpty
    @Size(min = 1)
    @Valid
    private List<ItemEntry> items;

    @Data
    public static class ItemEntry {

        /** Reuse an existing product owned by the current store. Mutually exclusive with {@code product}. */
        private Long productId;

        /** Inline-create a new product. Mutually exclusive with {@code productId}. */
        @Valid
        private InlineProduct product;

        @NotBlank
        @Size(min = 1, max = 128)
        private String displayName;

        @NotNull
        @Min(0)
        private Long groupPriceAmount;

        @NotNull
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

        @NotNull
        @Min(0)
        private Long basePriceAmount;

        @NotNull
        @Min(0)
        private Integer stock;
    }
}
