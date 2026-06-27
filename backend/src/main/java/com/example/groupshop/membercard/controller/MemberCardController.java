package com.example.groupshop.membercard.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.membercard.dto.MemberCardListResponse;
import com.example.groupshop.membercard.service.MemberCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Member card controller — list current user's member cards.
 *
 * <p>Batch 09.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemberCardController {

    private final MemberCardService memberCardService;

    /**
     * List current user's member cards.
     */
    @GetMapping("/my/member-cards")
    public ApiResponse<MemberCardListResponse> listMyMemberCards(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.success(memberCardService.listMyMemberCards(userId));
    }
}
