package com.example.groupshop.memberlevel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.util.CurrentStoreHelper;
import com.example.groupshop.memberlevel.dto.MemberLevelRuleItem;
import com.example.groupshop.memberlevel.dto.MemberLevelRulesResponse;
import com.example.groupshop.memberlevel.dto.UpdateMemberLevelRulesRequest;
import com.example.groupshop.model.entity.MemberLevelRule;
import com.example.groupshop.model.entity.MemberRelation;
import com.example.groupshop.model.mapper.MemberLevelRuleMapper;
import com.example.groupshop.model.mapper.MemberRelationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Service for member level rule management.
 *
 * <p>Handles GET/PUT of store member level rules, level name resolution,
 * and recalculation after rule changes.
 */
@Service
@RequiredArgsConstructor
public class MemberLevelRuleService {

    private final MemberLevelRuleMapper memberLevelRuleMapper;
    private final MemberRelationMapper memberRelationMapper;
    private final CurrentStoreHelper currentStoreHelper;

    /**
     * Get the current user's store member level rules, sorted by minGrowthValue ascending.
     */
    public MemberLevelRulesResponse getRules(Long userId) {
        Long storeId = currentStoreHelper.getLeaderAndStore(userId).getStore().getId();

        List<MemberLevelRule> rules = memberLevelRuleMapper.selectList(
                new LambdaQueryWrapper<MemberLevelRule>()
                        .eq(MemberLevelRule::getStoreId, storeId)
                        .orderByAsc(MemberLevelRule::getSortOrder));

        List<MemberLevelRuleItem> items = rules.stream()
                .map(r -> MemberLevelRuleItem.builder()
                        .levelName(r.getLevelName())
                        .minGrowthValue(r.getMinGrowthValue())
                        .build())
                .collect(Collectors.toList());

        return MemberLevelRulesResponse.builder()
                .rules(items)
                .build();
    }

    /**
     * Full-replace the current user's store member level rules.
     *
     * <p>Validates that at least one rule has minGrowthValue=0.
     * Assigns sortOrder based on request list order.
     * After replacement, recalculates all existing member relations for this store.
     */
    @Transactional
    public MemberLevelRulesResponse updateRules(Long userId, UpdateMemberLevelRulesRequest request) {
        Long storeId = currentStoreHelper.getLeaderAndStore(userId).getStore().getId();

        List<MemberLevelRuleItem> items = request.getRules();

        // Validate: must have at least one rule with minGrowthValue=0
        boolean hasBaseRule = items.stream().anyMatch(item -> item.getMinGrowthValue() == 0);
        if (!hasBaseRule) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "必须至少包含一条 minGrowthValue=0 的等级规则");
        }

        // Validate: minGrowthValue must be strictly ascending
        // This ensures sortOrder order matches threshold order and
        // resolveLevelName behaves correctly regardless of storage ordering.
        for (int i = 1; i < items.size(); i++) {
            if (items.get(i).getMinGrowthValue() <= items.get(i - 1).getMinGrowthValue()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "minGrowthValue 必须严格递增");
            }
        }

        // Full replacement: delete all existing rules, then insert
        memberLevelRuleMapper.deleteByStoreId(storeId);

        List<MemberLevelRule> newRules = IntStream.range(0, items.size())
                .mapToObj(i -> {
                    MemberLevelRuleItem item = items.get(i);
                    MemberLevelRule rule = new MemberLevelRule();
                    rule.setStoreId(storeId);
                    rule.setLevelName(item.getLevelName());
                    rule.setMinGrowthValue(item.getMinGrowthValue());
                    rule.setSortOrder(i); // Use list order as sort order
                    return rule;
                })
                .collect(Collectors.toList());

        // Insert rules one by one (no batch insert support in base mapper without XML)
        for (MemberLevelRule rule : newRules) {
            memberLevelRuleMapper.insert(rule);
        }

        // Recalculate all existing member relations for this store
        recalculateMemberLevels(storeId, newRules);

        return MemberLevelRulesResponse.builder()
                .rules(items)
                .build();
    }

    /**
     * Resolve the level name for a given growth value using store rules.
     * Returns the highest matching level (max minGrowthValue <= growthValue).
     * Falls back to "V0" if no rules exist.
     */
    public String resolveLevelName(Long storeId, Integer growthValue) {
        List<MemberLevelRule> rules = memberLevelRuleMapper.selectList(
                new LambdaQueryWrapper<MemberLevelRule>()
                        .eq(MemberLevelRule::getStoreId, storeId)
                        .orderByAsc(MemberLevelRule::getMinGrowthValue));

        if (rules.isEmpty()) {
            return "V0";
        }

        // Walk thresholds in ascending minGrowthValue; the last match wins
        // (highest threshold the user has reached)
        String matchedLevel = "V0";
        for (MemberLevelRule rule : rules) {
            if (rule.getMinGrowthValue() <= growthValue) {
                matchedLevel = rule.getLevelName();
            }
        }
        return matchedLevel;
    }

    /**
     * Get the next level info for a member card display.
     *
     * @param storeId     the store ID
     * @param growthValue current growth value
     * @return NextLevelInfo (null if already at max level or no rules)
     */
    public NextLevelInfo getNextLevelInfo(Long storeId, Integer growthValue) {
        List<MemberLevelRule> rules = memberLevelRuleMapper.selectList(
                new LambdaQueryWrapper<MemberLevelRule>()
                        .eq(MemberLevelRule::getStoreId, storeId)
                        .orderByAsc(MemberLevelRule::getMinGrowthValue));

        if (rules.isEmpty()) {
            return null;
        }

        // Find the current level's max index
        int currentLevelIndex = -1;
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getMinGrowthValue() <= growthValue) {
                currentLevelIndex = i;
            }
        }

        // If current is the last level, no next level
        if (currentLevelIndex >= rules.size() - 1) {
            return null;
        }

        MemberLevelRule nextRule = rules.get(currentLevelIndex + 1);
        return new NextLevelInfo(
                nextRule.getLevelName(),
                nextRule.getMinGrowthValue(),
                nextRule.getMinGrowthValue() - growthValue
        );
    }

    /**
     * Recalculate level names for all member relations of a store.
     * Called after PUT rules or after upserting a member relation.
     */
    public void recalculateMemberLevels(Long storeId, List<MemberLevelRule> rules) {
        // Sort rules by minGrowthValue ascending
        List<MemberLevelRule> sorted = rules.stream()
                .sorted(Comparator.comparingInt(MemberLevelRule::getMinGrowthValue))
                .collect(Collectors.toList());

        List<MemberRelation> relations = memberRelationMapper.selectList(
                new LambdaQueryWrapper<MemberRelation>()
                        .eq(MemberRelation::getStoreId, storeId));

        for (MemberRelation relation : relations) {
            String newLevel = resolveLevelName(storeId, relation.getGrowthValue());
            relation.setLevelName(newLevel);
            memberRelationMapper.updateById(relation);
        }
    }

    /**
     * Resolve and update level name for a single member relation.
     * Called after order payment to sync level.
     */
    public void recalculateMemberLevel(Long storeId, Long memberRelationId, Integer growthValue) {
        String newLevel = resolveLevelName(storeId, growthValue);

        memberRelationMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MemberRelation>()
                        .eq(MemberRelation::getId, memberRelationId)
                        .set(MemberRelation::getLevelName, newLevel));
    }

    /**
     * Next level info for member card display.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class NextLevelInfo {
        private String nextLevelName;
        private Integer nextLevelGrowthValue;
        private Integer growthToNextLevel;
    }
}
