package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员关系表 — member_relations
 *
 * <p>Tracks membership between a user (buyer) and a leader/store.
 * Created on first paid order. MVP only supports V0 level.
 */
@Data
@TableName("member_relations")
public class MemberRelation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Long leaderId;
    private Long storeId;

    /** V0 / V1 / V2 / V3 — MVP only V0 */
    private String levelName;

    /** MVP: growthValue = totalOrderAmount (1 分 = 1 点) */
    private Integer growthValue;

    /** 累计支付金额，单位分 */
    private Long totalOrderAmount;

    /** 累计订单数 */
    private Integer totalOrders;

    private LocalDateTime lastOrderAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
