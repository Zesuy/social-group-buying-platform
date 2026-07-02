package com.example.groupshop.groupbuy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for updating a group buy's visibility (permission).
 */
@Data
public class UpdateGroupBuyPermissionRequest {

    @NotBlank
    private String visibility;
}
