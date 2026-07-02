package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 团购分享 token 表 — group_buy_share_tokens
 *
 * <p>A share token provides access to a (potentially hidden) group buy.
 * Each group buy can have at most one active token at a time.
 * Tokens are long-lived and reusable.
 */
@Data
@TableName("group_buy_share_tokens")
public class GroupBuyShareToken {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long groupBuyId;

    /** 唯一 token 值，用于 URL 分享 */
    private String token;

    /** active / expired / revoked */
    private String status;

    /** 可选的过期时间，null 表示长期有效 */
    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
