package com.example.groupshop.notification.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.notification.dto.NotificationResponse;
import com.example.groupshop.notification.dto.UnreadCountResponse;
import com.example.groupshop.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/my/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> listMyNotifications(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(notificationService.listMyNotifications(userId, type, unreadOnly, page, pageSize));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.success(notificationService.unreadCount(userId));
    }

    @GetMapping("/{notificationId}")
    public ApiResponse<NotificationResponse> getMyNotification(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long notificationId) {
        return ApiResponse.success(notificationService.getMyNotification(userId, notificationId));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long notificationId) {
        return ApiResponse.success(notificationService.markRead(userId, notificationId));
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        notificationService.markAllRead(userId);
        return ApiResponse.success();
    }
}
