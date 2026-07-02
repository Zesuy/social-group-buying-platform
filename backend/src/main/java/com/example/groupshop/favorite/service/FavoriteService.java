package com.example.groupshop.favorite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.favorite.dto.FavoriteResponse;
import com.example.groupshop.model.entity.FavoriteItem;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.mapper.FavoriteItemMapper;
import com.example.groupshop.model.mapper.GroupBuyMapper;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for group buy favorites.
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private static final String TARGET_TYPE_GROUP_BUY = "group_buy";

    private final FavoriteItemMapper favoriteItemMapper;
    private final GroupBuyMapper groupBuyMapper;
    private final GroupBuyItemMapper groupBuyItemMapper;

    /**
     * Favorite a group buy.
     *
     * <p>Only public, published group buys can be favorited.
     * If already active, returns idempotently.
     * If canceled, reactivates.
     */
    @Transactional
    public FavoriteResponse favorite(Long userId, Long groupBuyId) {
        // Validate group buy exists and is public + published
        GroupBuy groupBuy = groupBuyMapper.selectById(groupBuyId);
        if (groupBuy == null || !"published".equals(groupBuy.getStatus()) || !"public".equals(groupBuy.getVisibility())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "团购不存在或不可收藏");
        }

        // Check existing favorite
        FavoriteItem existing = favoriteItemMapper.selectOne(
                new LambdaQueryWrapper<FavoriteItem>()
                        .eq(FavoriteItem::getUserId, userId)
                        .eq(FavoriteItem::getTargetType, TARGET_TYPE_GROUP_BUY)
                        .eq(FavoriteItem::getTargetId, groupBuyId));

        LocalDateTime now = LocalDateTime.now();

        if (existing != null) {
            if ("active".equals(existing.getStatus())) {
                // Idempotent: return existing
                return toResponse(existing, groupBuy);
            }
            // Reactivate canceled
            existing.setStatus("active");
            existing.setFavoritedAt(now);
            existing.setCanceledAt(null);
            favoriteItemMapper.updateById(existing);
            return toResponse(existing, groupBuy);
        }

        // Create new favorite
        FavoriteItem item = new FavoriteItem();
        item.setUserId(userId);
        item.setTargetType(TARGET_TYPE_GROUP_BUY);
        item.setTargetId(groupBuyId);
        item.setStatus("active");
        item.setFavoritedAt(now);
        favoriteItemMapper.insert(item);

        return toResponse(item, groupBuy);
    }

    /**
     * Cancel a favorite. Idempotent.
     */
    @Transactional
    public void cancelFavorite(Long userId, Long groupBuyId) {
        FavoriteItem existing = favoriteItemMapper.selectOne(
                new LambdaQueryWrapper<FavoriteItem>()
                        .eq(FavoriteItem::getUserId, userId)
                        .eq(FavoriteItem::getTargetType, TARGET_TYPE_GROUP_BUY)
                        .eq(FavoriteItem::getTargetId, groupBuyId));

        if (existing == null || "canceled".equals(existing.getStatus())) {
            return; // Idempotent
        }

        existing.setStatus("canceled");
        existing.setCanceledAt(LocalDateTime.now());
        favoriteItemMapper.updateById(existing);
    }

    /**
     * List active favorites with group buy summary, paginated.
     */
    public PageResponse<FavoriteResponse> listMyFavorites(Long userId, int page, int pageSize) {
        Page<FavoriteItem> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<FavoriteItem> wrapper = new LambdaQueryWrapper<FavoriteItem>()
                .eq(FavoriteItem::getUserId, userId)
                .eq(FavoriteItem::getTargetType, TARGET_TYPE_GROUP_BUY)
                .eq(FavoriteItem::getStatus, "active")
                .orderByDesc(FavoriteItem::getFavoritedAt);

        Page<FavoriteItem> result = favoriteItemMapper.selectPage(pageObj, wrapper);

        List<FavoriteResponse> items = result.getRecords().stream()
                .map(fav -> {
                    GroupBuy gb = groupBuyMapper.selectById(fav.getTargetId());
                    return toResponse(fav, gb);
                })
                .collect(Collectors.toList());

        return PageResponse.of(items, page, pageSize, result.getTotal());
    }

    /**
     * Check if a user has an active favorite for a group buy.
     */
    public boolean isFavorited(Long userId, Long groupBuyId) {
        if (userId == null) {
            return false;
        }
        Long count = favoriteItemMapper.selectCount(
                new LambdaQueryWrapper<FavoriteItem>()
                        .eq(FavoriteItem::getUserId, userId)
                        .eq(FavoriteItem::getTargetType, TARGET_TYPE_GROUP_BUY)
                        .eq(FavoriteItem::getTargetId, groupBuyId)
                        .eq(FavoriteItem::getStatus, "active"));
        return count != null && count > 0;
    }

    private FavoriteResponse toResponse(FavoriteItem fav, GroupBuy gb) {
        if (gb == null) {
            return FavoriteResponse.builder()
                    .id(fav.getId())
                    .groupBuyId(fav.getTargetId())
                    .favoritedAt(fav.getFavoritedAt() != null ? fav.getFavoritedAt().toString() : null)
                    .build();
        }

        List<GroupBuyItem> gbItems = groupBuyItemMapper.selectList(
                new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, gb.getId()));

        long minPriceAmount = gbItems.stream()
                .mapToLong(GroupBuyItem::getGroupPriceAmount)
                .min().orElse(0);

        int soldCount = gbItems.stream()
                .mapToInt(GroupBuyItem::getSoldCount)
                .sum();

        return FavoriteResponse.builder()
                .id(fav.getId())
                .groupBuyId(gb.getId())
                .title(gb.getTitle())
                .coverImageUrl(gb.getCoverImageUrl())
                .minPriceAmount(minPriceAmount)
                .soldCount(soldCount)
                .endTime(gb.getEndTime() != null ? gb.getEndTime().toString() : null)
                .favoritedAt(fav.getFavoritedAt() != null ? fav.getFavoritedAt().toString() : null)
                .build();
    }
}
