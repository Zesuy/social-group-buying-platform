package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员等级规则表 — member_level_rules
 *
 * <p>Store-level member tier rules. Ordered by min_growth_value ascending.
 * PUT replaces all rules for a store (delete + insert in a transaction).
 */
@Data
@TableName("member_level_rules")
public class MemberLevelRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long storeId;
    private String levelName;
    private Integer minGrowthValue;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
