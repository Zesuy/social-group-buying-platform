package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏表 — favorite_items
 *
 * <p>This batch only supports {@code targetType = "group_buy"}.
 * Unique constraint on (user_id, target_type, target_id).
 */
@Data
@TableName("favorite_items")
public class FavoriteItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** 收藏对象类型：group_buy */
    private String targetType;

    /** 收藏对象ID（group_buy_id） */
    private Long targetId;

    /** active / canceled */
    private String status;

    private LocalDateTime favoritedAt;
    private LocalDateTime canceledAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
