package com.example.groupshop.aftersale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for after-sale.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfterSaleResponse {

    private Long id;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long leaderId;
    private Long storeId;
    private String type;
    private String reason;
    private String status;
    private Long amount;
    private String rejectReason;
    private String approvedAt;
    private String rejectedAt;
    private String completedAt;
    private String createdAt;
    private String updatedAt;

    // ── Order summary for context ──────────────────────────────────────

    /** Original order status at the time of after-sale application (API format) */
    private String originalOrderStatus;

    /** Current order status */
    private String orderStatus;

    /** Current pay status */
    private String payStatus;
}
