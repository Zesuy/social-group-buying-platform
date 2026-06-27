package com.example.groupshop.publicbrowsing;

import com.example.groupshop.auth.TokenStore;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import com.example.groupshop.leader.dto.LeaderHomepageResponse;
import com.example.groupshop.leader.service.LeaderService;
import com.example.groupshop.publicbrowsing.dto.GroupBuyDetailResponse;
import com.example.groupshop.publicbrowsing.dto.PublicGroupBuyItem;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public browsing endpoints — authentication optional.
 *
 * <p>Batch 09 backfill: authenticated requests get real subscription status
 * in viewer.subscribed. Unauthenticated requests get {@code false}.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicBrowsingController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final GroupBuyService groupBuyService;
    private final LeaderService leaderService;
    private final TokenStore tokenStore;

    /**
     * List public published group buys (首页团购列表).
     * Note: this endpoint does NOT return per-item subscription status,
     * so optional auth is not needed here.
     */
    @GetMapping("/group-buys")
    public ApiResponse<PageResponse<PublicGroupBuyItem>> listGroupBuys(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(groupBuyService.getPublicGroupBuys(page, pageSize));
    }

    /**
     * Get public group buy detail (团购详情).
     * Supports optional Authorization header for real viewer.subscribed.
     */
    @GetMapping("/group-buys/{groupBuyId}")
    public ApiResponse<GroupBuyDetailResponse> getGroupBuyDetail(
            @PathVariable Long groupBuyId,
            HttpServletRequest request) {
        Long viewerUserId = resolveOptionalUserId(request);
        return ApiResponse.success(groupBuyService.getPublicGroupBuyDetail(groupBuyId, viewerUserId));
    }

    /**
     * Get leader homepage (团长主页).
     * Supports optional Authorization header for real viewer.subscribed.
     */
    @GetMapping("/leaders/{leaderId}/homepage")
    public ApiResponse<LeaderHomepageResponse> getLeaderHomepage(
            @PathVariable Long leaderId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        Long viewerUserId = resolveOptionalUserId(request);
        return ApiResponse.success(leaderService.getLeaderHomepage(leaderId, page, pageSize, viewerUserId));
    }

    /**
     * Try to resolve a user ID from the request's Bearer token.
     * Returns {@code null} if no valid token is present (not an error).
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
