package com.example.groupshop.memberlevel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for member level rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberLevelRulesResponse {

    private List<MemberLevelRuleItem> rules;
}
