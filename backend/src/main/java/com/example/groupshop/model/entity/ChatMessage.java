package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_messages")
public class ChatMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long conversationId;
    private Long senderUserId;
    private String senderRole;
    private String messageType;
    private String content;
    private Long imageAssetId;
    private String imageUrl;
    private String cardType;
    private String cardPayload;
    private Long relatedOrderId;
    private String clientMessageId;
    private LocalDateTime createdAt;
}
