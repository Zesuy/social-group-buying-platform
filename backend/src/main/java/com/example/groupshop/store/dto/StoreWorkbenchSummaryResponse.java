package com.example.groupshop.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreWorkbenchSummaryResponse {

    private StoreInfo store;
    private LeaderInfo leader;
    private Todos todos;
    private StatusCounts statusCounts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreInfo {
        private Long id;
        private String name;
        private String logoUrl;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderInfo {
        private Long id;
        private String displayName;
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Todos {
        private Long paidOrders;
        private Long pendingAfterSales;
        private Long unreadLeaderChats;
        private Long publishedGroupBuys;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCounts {
        private Map<String, Long> orders;
        private Map<String, Long> afterSales;
        private Map<String, Long> groupBuys;
    }
}
