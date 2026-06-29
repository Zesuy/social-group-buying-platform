package com.example.groupshop.order.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.order.dto.CreateOrderRequest;
import com.example.groupshop.order.dto.OrderPreviewRequest;
import com.example.groupshop.order.dto.OrderPreviewResponse;
import com.example.groupshop.order.dto.OrderResponse;
import com.example.groupshop.order.service.OrderService;
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
 * Order controller — preview, create, list, detail, cancel, simulate-pay, complete.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Preview an order (no order created, no stock deducted).
     */
    @PostMapping("/orders/preview")
    public ApiResponse<OrderPreviewResponse> previewOrder(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody OrderPreviewRequest request) {
        return ApiResponse.success(orderService.previewOrder(userId, request));
    }

    /**
     * Create an order.
     */
    @PostMapping("/orders")
    public ApiResponse<OrderResponse> createOrder(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.createOrder(userId, request));
    }

    /**
     * List my orders.
     */
    @GetMapping("/my/orders")
    public ApiResponse<PageResponse<OrderResponse>> listMyOrders(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(orderService.getMyOrders(userId, status, page, pageSize));
    }

    /**
     * Get my order detail.
     */
    @GetMapping("/my/orders/{orderId}")
    public ApiResponse<OrderResponse> getMyOrder(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long orderId) {
        return ApiResponse.success(orderService.getMyOrder(userId, orderId));
    }

    /**
     * Cancel an order (only pending-pay, unpaid orders).
     */
    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long orderId) {
        return ApiResponse.success(orderService.cancelOrder(userId, orderId));
    }

    /**
     * Simulate payment for an order (Batch 07).
     * Only the order owner can pay. Deducts stock and creates/updates member relation.
     */
    @PostMapping("/orders/{orderId}/simulate-pay")
    public ApiResponse<OrderResponse> simulatePay(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long orderId) {
        return ApiResponse.success(orderService.simulatePay(userId, orderId));
    }

    /**
     * Complete (confirm receipt for) an order (Batch 10).
     * Only the order owner can complete. Only shipped orders can be completed.
     */
    @PostMapping("/orders/{orderId}/complete")
    public ApiResponse<OrderResponse> completeOrder(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long orderId) {
        return ApiResponse.success(orderService.completeOrder(userId, orderId));
    }
}
