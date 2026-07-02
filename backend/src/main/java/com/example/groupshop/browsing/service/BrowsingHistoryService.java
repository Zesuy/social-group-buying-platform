package com.example.groupshop.browsing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.groupshop.browsing.dto.BrowsingHistoryResponse;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.model.entity.BrowsingHistory;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.mapper.BrowsingHistoryMapper;
import com.example.groupshop.model.mapper.GroupBuyMapper;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for browsing history.
 */
@Service
@RequiredArgsConstructor
public class BrowsingHistoryService {

    private static final Logger log = LoggerFactory.getLogger(BrowsingHistoryService.class);
    private static final String TARGET_TYPE_GROUP_BUY = "group_buy";

    private final BrowsingHistoryMapper browsingHistoryMapper;
    private final GroupBuyMapper groupBuyMapper;
    private final GroupBuyItemMapper groupBuyItemMapper;

    /**
     * Record a group buy detail view. Uses upsert — insert or update viewed_at.
     *
     * <p>This is an auxiliary action. Failures are logged and swallowed
     * to not affect the detail response.
     */
    @Transactional
    public void recordView(Long userId, Long groupBuyId) {
        try {
            BrowsingHistory existing = browsingHistoryMapper.selectOne(
                    new LambdaQueryWrapper<BrowsingHistory>()
                            .eq(BrowsingHistory::getUserId, userId)
                            .eq(BrowsingHistory::getTargetType, TARGET_TYPE_GROUP_BUY)
                            .eq(BrowsingHistory::getTargetId, groupBuyId));

            LocalDateTime now = LocalDateTime.now();

            if (existing != null) {
                existing.setViewedAt(now);
                browsingHistoryMapper.updateById(existing);
            } else {
                BrowsingHistory history = new BrowsingHistory();
                history.setUserId(userId);
                history.setTargetType(TARGET_TYPE_GROUP_BUY);
                history.setTargetId(groupBuyId);
                history.setViewedAt(now);
                browsingHistoryMapper.insert(history);
            }
        } catch (Exception e) {
            log.error("Failed to record browsing history for user {} groupBuy {}", userId, groupBuyId, e);
        }
    }

    /**
     * List browsing history for the current user, ordered by viewed_at desc.
     */
    public PageResponse<BrowsingHistoryResponse> listMyHistories(Long userId, int page, int pageSize) {
        Page<BrowsingHistory> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<BrowsingHistory> wrapper = new LambdaQueryWrapper<BrowsingHistory>()
                .eq(BrowsingHistory::getUserId, userId)
                .eq(BrowsingHistory::getTargetType, TARGET_TYPE_GROUP_BUY)
                .orderByDesc(BrowsingHistory::getViewedAt);

        Page<BrowsingHistory> result = browsingHistoryMapper.selectPage(pageObj, wrapper);

        List<BrowsingHistoryResponse> items = result.getRecords().stream()
                .map(h -> {
                    GroupBuy gb = groupBuyMapper.selectById(h.getTargetId());
                    return toResponse(h, gb);
                })
                .collect(Collectors.toList());

        return PageResponse.of(items, page, pageSize, result.getTotal());
    }

    /**
     * Delete a browsing history entry. Only the owner can delete.
     */
    public void deleteHistory(Long userId, Long historyId) {
        BrowsingHistory history = browsingHistoryMapper.selectById(historyId);
        if (history == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!history.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能删除他人的浏览记录");
        }
        browsingHistoryMapper.deleteById(historyId);
    }

    private BrowsingHistoryResponse toResponse(BrowsingHistory history, GroupBuy gb) {
        return BrowsingHistoryResponse.builder()
                .id(history.getId())
                .groupBuyId(gb != null ? gb.getId() : history.getTargetId())
                .title(gb != null ? gb.getTitle() : null)
                .coverImageUrl(gb != null ? gb.getCoverImageUrl() : null)
                .viewedAt(history.getViewedAt() != null ? history.getViewedAt().toString() : null)
                .build();
    }
}
