package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notifications")
public class Notification {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long recipientUserId;
    private Long senderUserId;
    private String type;
    private String title;
    private String summary;
    private String body;
    private String targetType;
    private Long targetId;
    private String actionUrl;
    private String dedupeKey;
    private String readStatus;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
