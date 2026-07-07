package com.example.groupshop.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.groupshop.common.util.CurrentStoreHelper;
import com.example.groupshop.model.entity.AfterSale;
import com.example.groupshop.model.entity.ChatConversation;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.mapper.AfterSaleMapper;
import com.example.groupshop.model.mapper.ChatConversationMapper;
import com.example.groupshop.model.mapper.GroupBuyMapper;
import com.example.groupshop.model.mapper.OrderMapper;
import com.example.groupshop.store.dto.StoreWorkbenchSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StoreWorkbenchService {

    private final CurrentStoreHelper currentStoreHelper;
    private final OrderMapper orderMapper;
    private final AfterSaleMapper afterSaleMapper;
    private final GroupBuyMapper groupBuyMapper;
    private final ChatConversationMapper chatConversationMapper;

    public StoreWorkbenchSummaryResponse getSummary(Long userId) {
        var leaderAndStore = currentStoreHelper.getLeaderAndStore(userId);
        Leader leader = leaderAndStore.getLeader();
        Store store = leaderAndStore.getStore();

        Map<String, Long> orderCounts = countOrders(store.getId());
        Map<String, Long> afterSaleCounts = countAfterSales(store.getId());
        Map<String, Long> groupBuyCounts = countGroupBuys(store.getId());

        return StoreWorkbenchSummaryResponse.builder()
                .store(StoreWorkbenchSummaryResponse.StoreInfo.builder()
                        .id(store.getId())
                        .name(store.getName())
                        .logoUrl(store.getLogoUrl())
                        .status(store.getStatus())
                        .build())
                .leader(StoreWorkbenchSummaryResponse.LeaderInfo.builder()
                        .id(leader.getId())
                        .displayName(leader.getDisplayName())
                        .avatarUrl(leader.getAvatarUrl())
                        .build())
                .todos(StoreWorkbenchSummaryResponse.Todos.builder()
                        .paidOrders(orderCounts.get("paid"))
                        .pendingAfterSales(afterSaleCounts.get("pending"))
                        .unreadLeaderChats(sumUnreadLeaderChats(leader.getUserId(), store.getId()))
                        .publishedGroupBuys(groupBuyCounts.get("published"))
                        .build())
                .statusCounts(StoreWorkbenchSummaryResponse.StatusCounts.builder()
                        .orders(orderCounts)
                        .afterSales(afterSaleCounts)
                        .groupBuys(groupBuyCounts)
                        .build())
                .build();
    }

    private Map<String, Long> countOrders(Long storeId) {
        Map<String, Long> counts = initializedCounts(List.of("paid", "shipped", "completed", "afterSale", "canceled"));
        counts.put("paid", countOrderStatus(storeId, "paid"));
        counts.put("shipped", countOrderStatus(storeId, "shipped"));
        counts.put("completed", countOrderStatus(storeId, "completed"));
        counts.put("afterSale", countOrderStatus(storeId, "after_sale"));
        counts.put("canceled", countOrderStatus(storeId, "canceled"));
        return counts;
    }

    private Map<String, Long> countAfterSales(Long storeId) {
        Map<String, Long> counts = initializedCounts(List.of("pending", "approved", "rejected", "completed"));
        counts.replaceAll((status, ignored) -> afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getStoreId, storeId)
                .eq(AfterSale::getStatus, status)));
        return counts;
    }

    private Map<String, Long> countGroupBuys(Long storeId) {
        Map<String, Long> counts = initializedCounts(List.of("draft", "published", "ended"));
        counts.replaceAll((status, ignored) -> groupBuyMapper.selectCount(new LambdaQueryWrapper<GroupBuy>()
                .eq(GroupBuy::getStoreId, storeId)
                .eq(GroupBuy::getStatus, status)));
        return counts;
    }

    private Long countOrderStatus(Long storeId, String status) {
        return orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getStoreId, storeId)
                .eq(Order::getOrderStatus, status));
    }

    private Long sumUnreadLeaderChats(Long leaderUserId, Long storeId) {
        List<ChatConversation> conversations = chatConversationMapper.selectList(
                new LambdaQueryWrapper<ChatConversation>()
                        .eq(ChatConversation::getLeaderUserId, leaderUserId)
                        .eq(ChatConversation::getStoreId, storeId));
        return conversations.stream()
                .map(ChatConversation::getLeaderUnreadCount)
                .filter(count -> count != null && count > 0)
                .mapToLong(Integer::longValue)
                .sum();
    }

    private Map<String, Long> initializedCounts(List<String> keys) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String key : keys) {
            counts.put(key, 0L);
        }
        return counts;
    }
}
