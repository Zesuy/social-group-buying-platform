package com.example.groupshop.product.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.product.dto.CreateProductRequest;
import com.example.groupshop.product.dto.ProductResponse;
import com.example.groupshop.product.dto.ProductUsageResponse;
import com.example.groupshop.product.dto.UpdateProductRequest;
import com.example.groupshop.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Controller for managing products under the current user's store.
 *
 * <p>All endpoints require authentication and a leader/store identity.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/my/store/products")
    public ApiResponse<PageResponse<ProductResponse>> getProducts(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<ProductResponse> response = productService.getMyStoreProducts(
                userId, keyword, categoryId, status, page, pageSize);
        return ApiResponse.success(response);
    }

    @PostMapping("/my/store/products")
    public ApiResponse<ProductResponse> createProduct(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.createProduct(userId, request);
        return ApiResponse.success(response);
    }

    @GetMapping("/my/store/products/{productId}")
    public ApiResponse<ProductResponse> getProduct(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long productId) {
        ProductResponse response = productService.getProduct(userId, productId);
        return ApiResponse.success(response);
    }

    @PatchMapping("/my/store/products/{productId}")
    public ApiResponse<ProductResponse> updateProduct(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductResponse response = productService.updateProduct(userId, productId, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/my/store/products/{productId}")
    public ApiResponse<Void> deleteProduct(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long productId) {
        productService.deleteProduct(userId, productId);
        return ApiResponse.success();
    }

    /**
     * Get product usage in group buys for the current store.
     */
    @GetMapping("/my/store/products/{productId}/usages")
    public ApiResponse<PageResponse<ProductUsageResponse>> getProductUsages(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<ProductUsageResponse> response = productService.getProductUsages(
                userId, productId, page, pageSize);
        return ApiResponse.success(response);
    }
}
