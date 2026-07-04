package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_conversations")
public class ChatConversation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long buyerUserId;
    private Long leaderUserId;
    private Long storeId;
    private Long lastMessageId;
    private LocalDateTime lastMessageAt;
    private Integer buyerUnreadCount;
    private Integer leaderUnreadCount;
    private LocalDateTime buyerLastReadAt;
    private LocalDateTime leaderLastReadAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
