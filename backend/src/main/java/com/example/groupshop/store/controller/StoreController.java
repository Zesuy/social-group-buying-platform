package com.example.groupshop.store.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.store.dto.CreateStoreRequest;
import com.example.groupshop.store.dto.StoreResponse;
import com.example.groupshop.store.dto.StoreWorkbenchSummaryResponse;
import com.example.groupshop.store.dto.UpdateStoreRequest;
import com.example.groupshop.store.service.StoreService;
import com.example.groupshop.store.service.StoreWorkbenchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for store management.
 *
 * <p>All endpoints require authentication (AuthInterceptor).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final StoreWorkbenchService storeWorkbenchService;

    /**
     * Create a store for the current user.
     * On success, a leader is created (or reused) and a store is created.
     */
    @PostMapping("/stores")
    public ApiResponse<StoreResponse> createStore(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody CreateStoreRequest request) {
        StoreResponse response = storeService.createStore(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * Get the current user's store with leader info.
     *
     * <p>Returns a success response with no {@code data} field if the user has no store.
     * When a store exists, returns both leader and store summaries.
     */
    @GetMapping("/my/store")
    public ApiResponse<StoreResponse> getMyStore(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        StoreResponse response = storeService.getMyStore(userId);
        if (response == null) {
            return ApiResponse.success();
        }
        return ApiResponse.success(response);
    }

    @GetMapping("/my/store/workbench-summary")
    public ApiResponse<StoreWorkbenchSummaryResponse> getWorkbenchSummary(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.success(storeWorkbenchService.getSummary(userId));
    }

    /**
     * Partial update the current user's store.
     * Only the provided fields are updated.
     */
    @PatchMapping("/my/store")
    public ApiResponse<StoreResponse> updateMyStore(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody UpdateStoreRequest request) {
        StoreResponse response = storeService.updateMyStore(userId, request);
        return ApiResponse.success(response);
    }
}
