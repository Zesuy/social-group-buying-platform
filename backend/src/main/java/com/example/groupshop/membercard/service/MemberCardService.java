package com.example.groupshop.membercard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.groupshop.membercard.dto.MemberCardListResponse;
import com.example.groupshop.membercard.dto.MemberCardResponse;
import com.example.groupshop.memberlevel.service.MemberLevelRuleService;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.MemberRelation;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.MemberRelationMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for member card queries.
 *
 * <p>Batch 09: list member cards for the current user.
 */
@Service
@RequiredArgsConstructor
public class MemberCardService {

    private final MemberRelationMapper memberRelationMapper;
    private final LeaderMapper leaderMapper;
    private final StoreMapper storeMapper;
    private final MemberLevelRuleService memberLevelRuleService;

    /**
     * List current user's member cards with enriched leader/store info.
     */
    public MemberCardListResponse listMyMemberCards(Long userId) {
        List<MemberRelation> relations = memberRelationMapper.selectList(
                new LambdaQueryWrapper<MemberRelation>()
                        .eq(MemberRelation::getUserId, userId)
                        .orderByDesc(MemberRelation::getLastOrderAt));

        List<MemberCardResponse> items = relations.stream()
                .map(this::toMemberCardResponse)
                .collect(Collectors.toList());

        return MemberCardListResponse.builder()
                .items(items)
                .build();
    }

    private MemberCardResponse toMemberCardResponse(MemberRelation relation) {
        MemberCardResponse.MemberCardResponseBuilder builder = MemberCardResponse.builder()
                .id(relation.getId())
                .levelName(relation.getLevelName())
                .growthValue(relation.getGrowthValue())
                .totalOrderAmount(relation.getTotalOrderAmount())
                .totalOrders(relation.getTotalOrders())
                .lastOrderAt(relation.getLastOrderAt() != null
                        ? relation.getLastOrderAt().toString() : null);

        // Add next level info
        MemberLevelRuleService.NextLevelInfo nextLevelInfo =
                memberLevelRuleService.getNextLevelInfo(relation.getStoreId(), relation.getGrowthValue());
        if (nextLevelInfo != null) {
            builder.nextLevelName(nextLevelInfo.getNextLevelName())
                    .nextLevelGrowthValue(nextLevelInfo.getNextLevelGrowthValue())
                    .growthToNextLevel(nextLevelInfo.getGrowthToNextLevel());
        }

        // Enrich with nested leader info
        Leader leader = leaderMapper.selectById(relation.getLeaderId());
        if (leader != null) {
            builder.leader(MemberCardResponse.LeaderInfo.builder()
                    .id(leader.getId())
                    .displayName(leader.getDisplayName())
                    .avatarUrl(leader.getAvatarUrl())
                    .build());
        }

        // Enrich with nested store info
        Store store = storeMapper.selectById(relation.getStoreId());
        if (store != null) {
            builder.store(MemberCardResponse.StoreInfo.builder()
                    .id(store.getId())
                    .name(store.getName())
                    .logoUrl(store.getLogoUrl())
                    .build());
        }

        return builder.build();
    }
}
