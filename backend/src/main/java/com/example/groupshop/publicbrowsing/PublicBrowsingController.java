package com.example.groupshop.publicbrowsing;

import com.example.groupshop.auth.TokenStore;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
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

import java.math.BigDecimal;

/**
 * Public browsing endpoints — authentication optional.
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
     * Supports keyword, categoryId, and optional location-based filtering/sorting.
     * Does NOT accept status parameter — returns VALIDATION_ERROR if passed.
     */
    @GetMapping("/group-buys")
    public ApiResponse<PageResponse<PublicGroupBuyItem>> listGroupBuys(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) Long maxDistanceMeters,
            @RequestParam(required = false) String sort) {
        // Reject status parameter — public list does not support filtering by status
        if (status != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "公共列表不支持 status 参数");
        }
        // Validate location parameters
        if ((latitude != null || longitude != null) && (latitude == null || longitude == null)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "经纬度必须同时提供");
        }
        if (latitude != null && longitude != null) {
            validateCoordinateRange(latitude, longitude);
        }
        if (maxDistanceMeters != null && (latitude == null || longitude == null)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "距离筛选需要提供经纬度");
        }
        if (maxDistanceMeters != null && maxDistanceMeters <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "距离筛选必须为正整数");
        }
        if ("distance".equals(sort) && (latitude == null || longitude == null)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "距离排序需要提供经纬度");
        }
        return ApiResponse.success(groupBuyService.getPublicGroupBuys(
                page, pageSize, keyword, categoryId,
                latitude, longitude, maxDistanceMeters, sort));
    }

    /**
     * Get public group buy detail (团购详情).
     * Supports optional Authorization header for real viewer.subscribed and viewer.favorited.
     * Supports optional latitude/longitude for distance display.
     */
    @GetMapping("/group-buys/{groupBuyId}")
    public ApiResponse<GroupBuyDetailResponse> getGroupBuyDetail(
            @PathVariable Long groupBuyId,
            HttpServletRequest request,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude) {
        if ((latitude != null || longitude != null) && (latitude == null || longitude == null)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "经纬度必须同时提供");
        }
        if (latitude != null && longitude != null) {
            validateCoordinateRange(latitude, longitude);
        }
        Long viewerUserId = resolveOptionalUserId(request);
        return ApiResponse.success(groupBuyService.getPublicGroupBuyDetail(
                groupBuyId, viewerUserId, latitude, longitude));
    }

    /**
     * Get leader homepage (团长主页).
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

    /**
     * Validate that latitude is in [-90, 90] and longitude in [-180, 180].
     *
     * @throws BusinessException with VALIDATION_ERROR if out of range
     */
    private static void validateCoordinateRange(BigDecimal latitude, BigDecimal longitude) {
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "纬度必须在 -90 到 90 之间");
        }
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "经度必须在 -180 到 180 之间");
        }
    }
}
