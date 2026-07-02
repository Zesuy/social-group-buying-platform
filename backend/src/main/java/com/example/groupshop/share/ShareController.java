package com.example.groupshop.share;

import com.example.groupshop.auth.TokenStore;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import com.example.groupshop.publicbrowsing.dto.GroupBuyDetailResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for share token-based access to group buys.
 *
 * <p>These endpoints allow access to hidden group buys via valid share tokens.
 */
@RestController
@RequestMapping("/api/v1/share")
@RequiredArgsConstructor
public class ShareController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final GroupBuyService groupBuyService;
    private final TokenStore tokenStore;

    /**
     * Get group buy detail via share token.
     * Validates the token and returns the corresponding published group buy detail.
     * Supports hidden group buys (which are not accessible through public endpoints).
     */
    @GetMapping("/group-buys/{shareToken}")
    public ApiResponse<GroupBuyDetailResponse> getGroupBuyByShareToken(
            @PathVariable String shareToken,
            HttpServletRequest request) {
        Long viewerUserId = resolveOptionalUserId(request);
        GroupBuyDetailResponse response = groupBuyService.getPublicGroupBuyDetailByShareToken(shareToken, viewerUserId);
        return ApiResponse.success(response);
    }

    /**
     * Try to resolve a user ID from the request's Bearer token.
     */
    private Long resolveOptionalUserId(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            return null;
        }
        return tokenStore.resolveUserId(token);
    }
}
