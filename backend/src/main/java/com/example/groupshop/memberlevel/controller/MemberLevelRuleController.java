package com.example.groupshop.memberlevel.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.memberlevel.dto.MemberLevelRulesResponse;
import com.example.groupshop.memberlevel.dto.UpdateMemberLevelRulesRequest;
import com.example.groupshop.memberlevel.service.MemberLevelRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for member level rule management.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemberLevelRuleController {

    private final MemberLevelRuleService memberLevelRuleService;

    /**
     * Get the current user's store member level rules.
     */
    @GetMapping("/my/store/member-level-rules")
    public ApiResponse<MemberLevelRulesResponse> getRules(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.success(memberLevelRuleService.getRules(userId));
    }

    /**
     * Full-replace the current user's store member level rules.
     * Request order determines sort order.
     * Must include at least one rule with minGrowthValue=0.
     */
    @PutMapping("/my/store/member-level-rules")
    public ApiResponse<MemberLevelRulesResponse> updateRules(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody UpdateMemberLevelRulesRequest request) {
        return ApiResponse.success(memberLevelRuleService.updateRules(userId, request));
    }
}
