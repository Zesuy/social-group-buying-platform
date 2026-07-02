package com.example.groupshop.browsing.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.browsing.dto.BrowsingHistoryResponse;
import com.example.groupshop.browsing.service.BrowsingHistoryService;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for browsing history.
 *
 * <p>All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BrowsingHistoryController {

    private final BrowsingHistoryService browsingHistoryService;

    /**
     * List my browsing histories (complex path /api/v1/my/browsing-histories).
     */
    @GetMapping("/my/browsing-histories")
    public ApiResponse<PageResponse<BrowsingHistoryResponse>> listMyHistories(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(browsingHistoryService.listMyHistories(userId, page, pageSize));
    }

    /**
     * Delete a browsing history entry. Only the owner can delete.
     */
    @DeleteMapping("/my/browsing-histories/{historyId}")
    public ApiResponse<Void> deleteHistory(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long historyId) {
        browsingHistoryService.deleteHistory(userId, historyId);
        return ApiResponse.success();
    }
}
