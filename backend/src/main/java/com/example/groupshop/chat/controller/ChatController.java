package com.example.groupshop.chat.controller;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.chat.dto.ChatConversationResponse;
import com.example.groupshop.chat.dto.ChatMessageResponse;
import com.example.groupshop.chat.dto.ChatUnreadCountResponse;
import com.example.groupshop.chat.dto.SendChatCardRequest;
import com.example.groupshop.chat.dto.SendChatMessageRequest;
import com.example.groupshop.chat.service.ChatService;
import com.example.groupshop.common.response.ApiResponse;
import com.example.groupshop.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/my/chat-conversations")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/orders/{orderId}")
    public ApiResponse<ChatConversationResponse> openByOrder(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long orderId) {
        return ApiResponse.success(chatService.openByOrder(userId, orderId));
    }

    @GetMapping
    public ApiResponse<PageResponse<ChatConversationResponse>> listMyConversations(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(chatService.listMyConversations(userId, role, page, pageSize));
    }

    @GetMapping("/unread-count")
    public ApiResponse<ChatUnreadCountResponse> unreadCount(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.success(chatService.unreadCount(userId));
    }

    @GetMapping("/{conversationId}/messages")
    public ApiResponse<PageResponse<ChatMessageResponse>> listMessages(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long afterMessageId,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(chatService.listMessages(userId, conversationId, afterMessageId, pageSize));
    }

    @PostMapping("/{conversationId}/messages")
    public ApiResponse<ChatMessageResponse> sendMessage(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long conversationId,
            @Valid @RequestBody SendChatMessageRequest request) {
        return ApiResponse.success(chatService.sendMessage(userId, conversationId, request));
    }

    @PostMapping("/{conversationId}/cards")
    public ApiResponse<ChatMessageResponse> sendCard(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long conversationId,
            @Valid @RequestBody SendChatCardRequest request) {
        return ApiResponse.success(chatService.sendCard(userId, conversationId, request));
    }

    @PostMapping("/{conversationId}/read")
    public ApiResponse<Void> markRead(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long conversationId) {
        chatService.markRead(userId, conversationId);
        return ApiResponse.success();
    }
}
