package com.example.groupshop.order.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.idempotency.IdempotencyService;
import com.example.groupshop.order.dto.OrderResponse;
import com.example.groupshop.order.dto.ShipOrderRequest;
import com.example.groupshop.order.dto.ShipOrderResponse;
import com.example.groupshop.order.service.StoreOrderService;
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
 * Store order controller — list, detail, and ship orders in the current user's store.
 *
 * <p>Idempotency-Key support on the ship endpoint.
 */
@RestController
@RequestMapping("/api/v1/my/store/orders")
@RequiredArgsConstructor
public class StoreOrderController {

    private final StoreOrderService storeOrderService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    /**
     * List orders for the current user's store (read-only).
     */
    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> listStoreOrders(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(storeOrderService.getStoreOrders(userId, status, keyword, page, pageSize));
    }

    /**
     * Get order detail for the current user's store (read-only).
     */
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getStoreOrder(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long orderId) {
        return ApiResponse.success(storeOrderService.getStoreOrder(userId, orderId));
    }

    /**
     * Ship an order with idempotency support.
     */
    @PostMapping("/{orderId}/ship")
    public ApiResponse<ShipOrderResponse> shipOrder(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long orderId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ShipOrderRequest request,
            HttpServletRequest servletRequest) {
        String body = toJson(request);
        ShipOrderResponse result = idempotencyService.execute(
                userId, "POST", servletRequest.getRequestURI(), idempotencyKey, body,
                ShipOrderResponse.class,
                () -> storeOrderService.shipOrder(userId, orderId, request));
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
