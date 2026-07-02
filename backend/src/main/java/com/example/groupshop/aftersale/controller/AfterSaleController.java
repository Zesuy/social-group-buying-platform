package com.example.groupshop.aftersale.controller;

import com.example.groupshop.aftersale.dto.AfterSaleResponse;
import com.example.groupshop.aftersale.dto.CreateAfterSaleRequest;
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
 * Buyer after-sale controller — apply, list, and view after-sale requests.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    /**
     * Apply for an after-sale (refund) on an order.
     */
    @PostMapping("/orders/{orderId}/after-sales")
    public ApiResponse<AfterSaleResponse> createAfterSale(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long orderId,
            @Valid @RequestBody CreateAfterSaleRequest request) {
        return ApiResponse.success(afterSaleService.createAfterSale(userId, orderId, request));
    }

    /**
     * List my after-sale requests.
     */
    @GetMapping("/my/after-sales")
    public ApiResponse<PageResponse<AfterSaleResponse>> listMyAfterSales(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(afterSaleService.getMyAfterSales(userId, page, pageSize));
    }

    /**
     * Get my after-sale detail.
     */
    @GetMapping("/my/after-sales/{afterSaleId}")
    public ApiResponse<AfterSaleResponse> getMyAfterSale(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long afterSaleId) {
        return ApiResponse.success(afterSaleService.getMyAfterSale(userId, afterSaleId));
    }
}
