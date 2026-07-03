package com.example.groupshop.notification.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateNotificationCommand {

    private Long recipientUserId;
    private Long senderUserId;
    private String type;
    private String title;
    private String summary;
    private String body;
    private String targetType;
    private Long targetId;
    private String actionUrl;
    private String dedupeKey;
}
