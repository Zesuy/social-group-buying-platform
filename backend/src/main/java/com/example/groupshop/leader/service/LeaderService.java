package com.example.groupshop.leader.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import com.example.groupshop.leader.dto.LeaderHomepageResponse;
import com.example.groupshop.leader.dto.LeaderHomepageResponse.LeaderInfo;
import com.example.groupshop.leader.dto.LeaderHomepageResponse.StoreInfo;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.mapper.GroupBuyMapper;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.publicbrowsing.dto.PublicGroupBuyItem;
import com.example.groupshop.publicbrowsing.dto.ViewerInfo;
import com.example.groupshop.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for leader-related public queries.
 */
@Service
@RequiredArgsConstructor
public class LeaderService {

    private final LeaderMapper leaderMapper;
    private final StoreMapper storeMapper;
    private final GroupBuyMapper groupBuyMapper;
    private final GroupBuyService groupBuyService;
    private final SubscriptionService subscriptionService;

    /**
     * Get leader homepage with store info and paginated public group buys.
     *
     * @param viewerUserId optional — if provided, checks real subscription status
     */
    public LeaderHomepageResponse getLeaderHomepage(Long leaderId, int page, int pageSize, Long viewerUserId) {
        Leader leader = leaderMapper.selectById(leaderId);
        if (leader == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        Store store = storeMapper.selectOne(
                new LambdaQueryWrapper<Store>()
                        .eq(Store::getLeaderId, leader.getId()));
        if (store == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        LeaderInfo leaderInfo = LeaderInfo.builder()
                .id(leader.getId())
                .displayName(leader.getDisplayName())
                .avatarUrl(leader.getAvatarUrl())
                .bio(leader.getBio())
                .memberCount(leader.getMemberCount())
                .followerCount(leader.getFollowerCount())
                .build();

        StoreInfo storeInfo = StoreInfo.builder()
                .id(store.getId())
                .name(store.getName())
                .logoUrl(store.getLogoUrl())
                .description(store.getDescription())
                .defaultDeliveryType(store.getDefaultDeliveryType())
                .build();

        // Get public group buys for this leader
        Page<GroupBuy> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<GroupBuy> wrapper = new LambdaQueryWrapper<GroupBuy>()
                .eq(GroupBuy::getLeaderId, leader.getId())
                .eq(GroupBuy::getStatus, "published")
                .eq(GroupBuy::getVisibility, "public")
                .orderByDesc(GroupBuy::getCreatedAt);

        Page<GroupBuy> result = groupBuyMapper.selectPage(pageObj, wrapper);
        List<PublicGroupBuyItem> groupBuyItems = result.getRecords().stream()
                .map(groupBuyService::toPublicGroupBuyItem)
                .collect(Collectors.toList());

        PageResponse<PublicGroupBuyItem> groupBuysPage = PageResponse.of(
                groupBuyItems, page, pageSize, result.getTotal());

        boolean subscribed = viewerUserId != null
                && subscriptionService.isSubscribed(viewerUserId, leaderId);

        return LeaderHomepageResponse.builder()
                .leader(leaderInfo)
                .store(storeInfo)
                .viewer(new ViewerInfo(subscribed))
                .groupBuys(groupBuysPage)
                .build();
    }
}
