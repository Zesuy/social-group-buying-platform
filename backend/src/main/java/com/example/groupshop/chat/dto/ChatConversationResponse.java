package com.example.groupshop.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatConversationResponse {

    private Long id;
    private Long buyerUserId;
    private Long leaderUserId;
    private Long storeId;
    private String buyerName;
    private String buyerAvatarUrl;
    private String leaderName;
    private String leaderAvatarUrl;
    private String storeName;
    private String storeLogoUrl;
    private String currentUserRole;
    private Integer unreadCount;
    private Long lastMessageId;
    private String lastMessageText;
    private String lastMessageType;
    private String lastMessageAt;
    private String createdAt;
}
