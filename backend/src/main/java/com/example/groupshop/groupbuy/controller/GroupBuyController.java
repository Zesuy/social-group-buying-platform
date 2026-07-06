package com.example.groupshop.groupbuy.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.groupbuy.dto.CreateDraftGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.GroupBuyAiPolishRequest;
import com.example.groupshop.groupbuy.dto.GroupBuyAiPolishResponse;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse;
import com.example.groupshop.groupbuy.dto.ShareCardResponse;
import com.example.groupshop.groupbuy.dto.UpdateGroupBuyPermissionRequest;
import com.example.groupshop.groupbuy.dto.UpdateGroupBuyRequest;
import com.example.groupshop.groupbuy.service.GroupBuyAiPolishService;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import com.example.groupshop.publicbrowsing.dto.GroupBuyDetailResponse;
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
    private final GroupBuyAiPolishService groupBuyAiPolishService;

    /**
     * Create and publish a group buy (MVP compatible — direct publish).
     * Supports inline product creation and reusing existing products.
     */
    @PostMapping("/my/store/group-buys")
    public ApiResponse<GroupBuyResponse> createGroupBuy(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody CreateGroupBuyRequest request) {
        GroupBuyResponse response = groupBuyService.createGroupBuy(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * Generate a local AI-style copy suggestion for the publish form.
     */
    @PostMapping("/my/store/group-buys/ai-polish")
    public ApiResponse<GroupBuyAiPolishResponse> polishGroupBuyCopy(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody(required = false) GroupBuyAiPolishRequest request) {
        GroupBuyAiPolishResponse response = groupBuyAiPolishService.polish(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * Create a group buy draft (status=draft).
     * Uses minimal validation — only title, deliveryType, and at least one valid item required.
     */
    @PostMapping("/my/store/group-buys/drafts")
    public ApiResponse<GroupBuyResponse> createDraft(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody CreateDraftGroupBuyRequest request) {
        GroupBuyResponse response = groupBuyService.createDraft(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * Publish a draft group buy (draft → published).
     * Validates publish-readiness including presale time constraints.
     */
    @PostMapping("/my/store/group-buys/{groupBuyId}/publish")
    public ApiResponse<GroupBuyResponse> publishGroupBuy(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long groupBuyId) {
        GroupBuyResponse response = groupBuyService.publishGroupBuy(userId, groupBuyId);
        return ApiResponse.success(response);
    }

    /**
     * Preview a group buy (any status) for the owning leader.
     * Returns the same detail structure as the public detail endpoint.
     */
    @GetMapping("/my/store/group-buys/{groupBuyId}/preview")
    public ApiResponse<GroupBuyDetailResponse> previewGroupBuy(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long groupBuyId) {
        GroupBuyDetailResponse response = groupBuyService.previewGroupBuy(userId, groupBuyId);
        return ApiResponse.success(response);
    }

    /**
     * Copy a group buy as a new draft.
     */
    @PostMapping("/my/store/group-buys/{groupBuyId}/copy")
    public ApiResponse<GroupBuyResponse> copyGroupBuy(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long groupBuyId) {
        GroupBuyResponse response = groupBuyService.copyGroupBuy(userId, groupBuyId);
        return ApiResponse.success(response);
    }

    /**
     * Update a group buy's visibility (public / hidden).
     * Ended or removed group buys are rejected.
     */
    @PatchMapping("/my/store/group-buys/{groupBuyId}/permission")
    public ApiResponse<GroupBuyResponse> updatePermission(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long groupBuyId,
            @Valid @RequestBody UpdateGroupBuyPermissionRequest request) {
        GroupBuyResponse response = groupBuyService.updatePermission(userId, groupBuyId, request);
        return ApiResponse.success(response);
    }

    /**
     * Get or create a share card with a share token for a group buy.
     * If an active token already exists, it is reused.
     */
    @PostMapping("/my/store/group-buys/{groupBuyId}/share-card")
    public ApiResponse<ShareCardResponse> getShareCard(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long groupBuyId) {
        ShareCardResponse response = groupBuyService.getOrCreateShareToken(userId, groupBuyId);
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
