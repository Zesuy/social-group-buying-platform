package com.example.groupshop.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatUnreadCountResponse {

    private long unreadCount;
}
