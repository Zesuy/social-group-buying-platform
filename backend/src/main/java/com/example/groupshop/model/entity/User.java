package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User entity — maps to the {@code users} table.
 */
@Data
@TableName("users")
public class User {

    private Long id;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String wechatOpenid;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
