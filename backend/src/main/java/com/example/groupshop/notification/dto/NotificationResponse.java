package com.example.groupshop.notification.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {

    private Long id;
    private String type;
    private String title;
    private String summary;
    private String body;
    private String targetType;
    private Long targetId;
    private String actionUrl;
    private String readStatus;
    private String readAt;
    private String createdAt;
}
