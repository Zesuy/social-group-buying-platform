package com.example.groupshop.favorite.controller;

import com.example.groupshop.auth.TokenStore;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.favorite.dto.FavoriteResponse;
import com.example.groupshop.favorite.service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for group buy favorites.
 *
 * <p>All endpoints require authentication. Because the auth interceptor
 * excludes {@code /api/v1/group-buys/**} (shared with public browsing),
 * we manually resolve the user from the Bearer token.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FavoriteController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final FavoriteService favoriteService;
    private final TokenStore tokenStore;

    /**
     * Favorite a group buy.
     */
    @PostMapping("/group-buys/{groupBuyId}/favorite")
    public ApiResponse<FavoriteResponse> favorite(
            @PathVariable Long groupBuyId,
            HttpServletRequest request) {
        Long userId = resolveRequiredUserId(request);
        return ApiResponse.success(favoriteService.favorite(userId, groupBuyId));
    }

    /**
     * Cancel a group buy favorite.
     */
    @DeleteMapping("/group-buys/{groupBuyId}/favorite")
    public ApiResponse<Void> cancelFavorite(
            @PathVariable Long groupBuyId,
            HttpServletRequest request) {
        Long userId = resolveRequiredUserId(request);
        favoriteService.cancelFavorite(userId, groupBuyId);
        return ApiResponse.success();
    }

    /**
     * List my active favorites.
     */
    @GetMapping("/my/favorites")
    public ApiResponse<PageResponse<FavoriteResponse>> listMyFavorites(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = resolveRequiredUserId(request);
        return ApiResponse.success(favoriteService.listMyFavorites(userId, page, pageSize));
    }

    /**
     * Resolve user ID from Bearer token. Throws UNAUTHORIZED if missing/invalid.
     */
    private Long resolveRequiredUserId(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = tokenStore.resolveUserId(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
