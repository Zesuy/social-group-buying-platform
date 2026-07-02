package com.example.groupshop.memberlevel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for updating member level rules (full replacement).
 */
@Data
public class UpdateMemberLevelRulesRequest {

    @Valid
    @NotEmpty(message = "等级规则不能为空")
    private List<MemberLevelRuleItem> rules;
}
