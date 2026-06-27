package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订阅关系表 — subscriptions
 *
 * <p>A user can subscribe to a leader to receive updates.
 * Unique constraint on (user_id, leader_id).
 */
@Data
@TableName("subscriptions")
public class Subscription {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Long leaderId;
    private Long storeId;

    /** active / canceled */
    private String status;

    /** homepage / product_detail / invitation */
    private String source;

    private LocalDateTime subscribedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
