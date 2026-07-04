package com.example.groupshop.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendChatMessageRequest {

    @NotBlank
    private String messageType;

    @Size(max = 1000)
    private String content;

    private Long imageAssetId;

    @Size(max = 512)
    private String imageUrl;

    @NotBlank
    @Size(max = 128)
    private String clientMessageId;
}
