package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 浏览历史表 — browsing_histories
 *
 * <p>This batch only records group buy detail views ({@code targetType = "group_buy"}).
 * Upsert logic: insert or update viewed_at on repeat visit.
 */
@Data
@TableName("browsing_histories")
public class BrowsingHistory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** 浏览对象类型：group_buy */
    private String targetType;

    /** 浏览对象ID（group_buy_id） */
    private Long targetId;

    private LocalDateTime viewedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
