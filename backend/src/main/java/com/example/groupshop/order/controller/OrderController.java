package com.example.groupshop.order.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.idempotency.IdempotencyService;
import com.example.groupshop.order.dto.CreateOrderRequest;
import com.example.groupshop.order.dto.OrderPreviewRequest;
import com.example.groupshop.order.dto.OrderPreviewResponse;
import com.example.groupshop.order.dto.OrderResponse;
import com.example.groupshop.order.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order controller — preview, create, list, detail, cancel, simulate-pay, complete.
 *
 * <p>Idempotency-Key support on all POST endpoints that change state.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    /**
     * Preview an order (no order created, no stock deducted).
     * No idempotency needed — preview is read-only.
     */
    @PostMapping("/orders/preview")
    public ApiResponse<OrderPreviewResponse> previewOrder(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody OrderPreviewRequest request) {
        return ApiResponse.success(orderService.previewOrder(userId, request));
    }

    /**
     * Create an order with idempotency support.
     */
    @PostMapping("/orders")
    public ApiResponse<OrderResponse> createOrder(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest servletRequest) {
        String body = toJson(request);
        OrderResponse result = idempotencyService.execute(
                userId, "POST", servletRequest.getRequestURI(), idempotencyKey, body,
                OrderResponse.class,
                () -> orderService.createOrder(userId, request));
        return ApiResponse.success(result);
    }

    /**
     * List my orders (read-only, no idempotency).
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
     * Get my order detail (read-only, no idempotency).
     */
    @GetMapping("/my/orders/{orderId}")
    public ApiResponse<OrderResponse> getMyOrder(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long orderId) {
        return ApiResponse.success(orderService.getMyOrder(userId, orderId));
    }

    /**
     * Cancel an order with idempotency support.
     */
    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long orderId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest) {
        OrderResponse result = idempotencyService.execute(
                userId, "POST", servletRequest.getRequestURI(), idempotencyKey, null,
                OrderResponse.class,
                () -> orderService.cancelOrder(userId, orderId));
        return ApiResponse.success(result);
    }

    /**
     * Simulate payment with idempotency support.
     */
    @PostMapping("/orders/{orderId}/simulate-pay")
    public ApiResponse<OrderResponse> simulatePay(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long orderId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest) {
        OrderResponse result = idempotencyService.execute(
                userId, "POST", servletRequest.getRequestURI(), idempotencyKey, null,
                OrderResponse.class,
                () -> orderService.simulatePay(userId, orderId));
        return ApiResponse.success(result);
    }

    /**
     * Complete (confirm receipt) with idempotency support.
     */
    @PostMapping("/orders/{orderId}/complete")
    public ApiResponse<OrderResponse> completeOrder(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long orderId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest) {
        OrderResponse result = idempotencyService.execute(
                userId, "POST", servletRequest.getRequestURI(), idempotencyKey, null,
                OrderResponse.class,
                () -> orderService.completeOrder(userId, orderId));
        return ApiResponse.success(result);
    }

    // ── Helper ────────────────────────────────────────────────────────

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "";
        }
    }
}
