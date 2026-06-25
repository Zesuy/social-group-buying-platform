package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Leader entity — maps to the {@code leaders} table.
 */
@Data
@TableName("leaders")
public class Leader {

    private Long id;
    private Long userId;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String serviceStatus;
    private Integer memberCount;
    private Integer followerCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
