package com.example.groupshop.order.dto;

/**
 * Common interface for order items (used by both preview and create request).
 */
public interface OrderItemEntry {
    Long getGroupBuyItemId();
    Integer getQuantity();
}
