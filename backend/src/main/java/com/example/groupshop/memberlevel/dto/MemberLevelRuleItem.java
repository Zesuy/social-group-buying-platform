package com.example.groupshop.memberlevel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single member level rule item used in PUT requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberLevelRuleItem {

    @NotBlank
    private String levelName;

    @NotNull
    private Integer minGrowthValue;
}
