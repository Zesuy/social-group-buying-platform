package com.example.groupshop.publicbrowsing;

import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import com.example.groupshop.leader.dto.LeaderHomepageResponse;
import com.example.groupshop.leader.service.LeaderService;
import com.example.groupshop.publicbrowsing.dto.GroupBuyDetailResponse;
import com.example.groupshop.publicbrowsing.dto.PublicGroupBuyItem;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public browsing endpoints — no authentication required.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicBrowsingController {

    private final GroupBuyService groupBuyService;
    private final LeaderService leaderService;

    /**
     * List public published group buys (首页团购列表).
     */
    @GetMapping("/group-buys")
    public ApiResponse<PageResponse<PublicGroupBuyItem>> listGroupBuys(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(groupBuyService.getPublicGroupBuys(page, pageSize));
    }

    /**
     * Get public group buy detail (团购详情).
     */
    @GetMapping("/group-buys/{groupBuyId}")
    public ApiResponse<GroupBuyDetailResponse> getGroupBuyDetail(
            @PathVariable Long groupBuyId) {
        return ApiResponse.success(groupBuyService.getPublicGroupBuyDetail(groupBuyId));
    }

    /**
     * Get leader homepage (团长主页).
     */
    @GetMapping("/leaders/{leaderId}/homepage")
    public ApiResponse<LeaderHomepageResponse> getLeaderHomepage(
            @PathVariable Long leaderId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(leaderService.getLeaderHomepage(leaderId, page, pageSize));
    }
}
