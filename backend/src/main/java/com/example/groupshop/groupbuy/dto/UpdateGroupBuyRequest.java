package com.example.groupshop.groupbuy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for partial-updating a group buy.
 *
 * <p>Only non-null top-level fields are updated.
 * Item updates are validated against order protection rules in the service.
 */
@Data
public class UpdateGroupBuyRequest {

    @Size(min = 1, max = 128)
    private String title;

    private String introduction;

    private String coverImageUrl;

    private String shippingTime;
    private String startTime;
    private String endTime;

    /** Optional list of item-level updates. Each must reference an existing item by id. */
    @Valid
    private List<UpdateItemEntry> items;

    @Data
    public static class UpdateItemEntry {

        @NotNull
        private Long id;

        private String displayName;

        @Min(0)
        private Long groupPriceAmount;

        @Min(0)
        private Integer groupStock;

        private Integer sortOrder;
    }
}
