package com.example.groupshop.groupbuy.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse;
import com.example.groupshop.groupbuy.dto.UpdateGroupBuyRequest;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing group buys under the current user's store.
 *
 * <p>All endpoints require authentication and a leader/store identity.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GroupBuyController {

    private final GroupBuyService groupBuyService;

    /**
     * Create and publish a group buy. Supports inline product creation
     * and reusing existing products.
     */
    @PostMapping("/my/store/group-buys")
    public ApiResponse<GroupBuyResponse> createGroupBuy(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody CreateGroupBuyRequest request) {
        GroupBuyResponse response = groupBuyService.createGroupBuy(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * List group buys for the current user's store.
     */
    @GetMapping("/my/store/group-buys")
    public ApiResponse<PageResponse<GroupBuyResponse.GroupBuyData>> getGroupBuys(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<GroupBuyResponse.GroupBuyData> response =
                groupBuyService.getMyStoreGroupBuys(userId, status, page, pageSize);
        return ApiResponse.success(response);
    }

    /**
     * Get a group buy with items.
     */
    @GetMapping("/my/store/group-buys/{groupBuyId}")
    public ApiResponse<GroupBuyResponse> getGroupBuy(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long groupBuyId) {
        GroupBuyResponse response = groupBuyService.getGroupBuy(userId, groupBuyId);
        return ApiResponse.success(response);
    }

    /**
     * Partial-update a group buy. Updates top-level fields and optionally item fields.
     * Ordered items have price protection.
     */
    @PatchMapping("/my/store/group-buys/{groupBuyId}")
    public ApiResponse<GroupBuyResponse> updateGroupBuy(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long groupBuyId,
            @Valid @RequestBody UpdateGroupBuyRequest request) {
        GroupBuyResponse response = groupBuyService.updateGroupBuy(userId, groupBuyId, request);
        return ApiResponse.success(response);
    }

    /**
     * End a published group buy. Only {@code published} group buys can be ended.
     */
    @PostMapping("/my/store/group-buys/{groupBuyId}/end")
    public ApiResponse<GroupBuyResponse> endGroupBuy(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long groupBuyId) {
        GroupBuyResponse response = groupBuyService.endGroupBuy(userId, groupBuyId);
        return ApiResponse.success(response);
    }
}
