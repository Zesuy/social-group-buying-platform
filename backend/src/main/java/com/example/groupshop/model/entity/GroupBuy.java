package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 团购活动表 — group_buys
 *
 * <p>A group buy is created by a leader under a store.
 * MVP only supports {@code groupType = normal} and creates directly
 * with {@code status = published}.
 */
@Data
@TableName("group_buys")
public class GroupBuy {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long storeId;
    private Long leaderId;
    private String title;
    private String introduction;
    private String coverImageUrl;

    /** 团购活动多图，JSON 字符串数组；实体层用 String 存 JSON */
    private String galleryImageUrls;

    /** 团购活动结构化正文块，JSON 字符串数组；实体层用 String 存 JSON */
    private String contentBlocks;

    /** normal / presale / coupon / signup — MVP only normal */
    private String groupType;

    /** express / pickup / local_delivery */
    private String deliveryType;

    /** 承诺发货时间 */
    private LocalDateTime shippingTime;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** public / hidden */
    private String visibility;

    /** draft / published / ended / removed */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
