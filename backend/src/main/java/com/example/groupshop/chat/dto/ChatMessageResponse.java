package com.example.groupshop.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ChatMessageResponse {

    private Long id;
    private Long conversationId;
    private Long senderUserId;
    private String senderRole;
    private String messageType;
    private String content;
    private Long imageAssetId;
    private String imageUrl;
    private String cardType;
    private Map<String, Object> cardPayload;
    private Long relatedOrderId;
    private Boolean mine;
    private String createdAt;
}
