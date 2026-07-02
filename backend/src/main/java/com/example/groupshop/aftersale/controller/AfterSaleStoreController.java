package com.example.groupshop.aftersale.controller;

import com.example.groupshop.aftersale.dto.AfterSaleResponse;
import com.example.groupshop.aftersale.dto.RejectAfterSaleRequest;
import com.example.groupshop.aftersale.service.AfterSaleService;
import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Store after-sale controller — list, detail, approve, reject, and complete refund.
 */
@RestController
@RequestMapping("/api/v1/my/store/after-sales")
@RequiredArgsConstructor
public class AfterSaleStoreController {

    private final AfterSaleService afterSaleService;

    /**
     * List after-sales for the current user's store.
     */
    @GetMapping
    public ApiResponse<PageResponse<AfterSaleResponse>> listStoreAfterSales(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(afterSaleService.getStoreAfterSales(userId, page, pageSize));
    }

    /**
     * Get an after-sale detail for the current user's store.
     */
    @GetMapping("/{afterSaleId}")
    public ApiResponse<AfterSaleResponse> getStoreAfterSale(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long afterSaleId) {
        return ApiResponse.success(afterSaleService.getStoreAfterSale(userId, afterSaleId));
    }

    /**
     * Approve an after-sale request (pending → approved).
     */
    @PostMapping("/{afterSaleId}/approve")
    public ApiResponse<AfterSaleResponse> approveAfterSale(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long afterSaleId) {
        return ApiResponse.success(afterSaleService.approveAfterSale(userId, afterSaleId));
    }

    /**
     * Reject an after-sale request (pending → rejected).
     */
    @PostMapping("/{afterSaleId}/reject")
    public ApiResponse<AfterSaleResponse> rejectAfterSale(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long afterSaleId,
            @Valid @RequestBody RejectAfterSaleRequest request) {
        return ApiResponse.success(afterSaleService.rejectAfterSale(userId, afterSaleId, request));
    }

    /**
     * Complete the refund for an approved after-sale (approved → completed).
     */
    @PostMapping("/{afterSaleId}/complete-refund")
    public ApiResponse<AfterSaleResponse> completeRefund(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long afterSaleId) {
        return ApiResponse.success(afterSaleService.completeRefund(userId, afterSaleId));
    }
}
