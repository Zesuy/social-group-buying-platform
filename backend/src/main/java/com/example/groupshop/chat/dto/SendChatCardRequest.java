package com.example.groupshop.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendChatCardRequest {

    @NotBlank
    private String cardType;

    @NotNull
    private Long orderId;

    @Size(max = 128)
    private String clientMessageId;
}
