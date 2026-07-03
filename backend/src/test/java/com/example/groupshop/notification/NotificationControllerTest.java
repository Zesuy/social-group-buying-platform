package com.example.groupshop.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.groupshop.base.MockMvcTestBase;
import com.example.groupshop.model.mapper.NotificationMapper;
import com.example.groupshop.notification.dto.CreateNotificationCommand;
import com.example.groupshop.notification.dto.NotificationResponse;
import com.example.groupshop.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String NOTIFICATION_URL = "/api/v1/my/notifications";

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationMapper notificationMapper;

    @BeforeEach
    void setUp() {
        notificationMapper.delete(new LambdaQueryWrapper<>());
    }

    @Test
    void listMyNotifications_shouldReturnCurrentUserNotifications() throws Exception {
        LoginUser user = login("13800009801");
        notificationService.createIfAbsent(command("batch08:controller:list", user.userId()));

        mockMvc.perform(get(NOTIFICATION_URL)
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.items[0].title").value("发货通知"))
                .andExpect(jsonPath("$.data.items[0].readStatus").value("unread"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void unreadCount_shouldReturnCurrentUserUnreadCount() throws Exception {
        LoginUser user = login("13800009802");
        notificationService.createIfAbsent(command("batch08:controller:count", user.userId()));

        mockMvc.perform(get(NOTIFICATION_URL + "/unread-count")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    @Test
    void getMyNotification_shouldReturnDetail() throws Exception {
        LoginUser user = login("13800009803");
        NotificationResponse notification = notificationService.createIfAbsent(command("batch08:controller:get", user.userId()));

        mockMvc.perform(get(NOTIFICATION_URL + "/" + notification.getId())
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.id").value(notification.getId()))
                .andExpect(jsonPath("$.data.summary").value("团长已填写物流：顺丰速运 SF1234567890。"));
    }

    @Test
    void markRead_shouldMarkSingleNotificationRead() throws Exception {
        LoginUser user = login("13800009804");
        NotificationResponse notification = notificationService.createIfAbsent(command("batch08:controller:read", user.userId()));

        mockMvc.perform(post(NOTIFICATION_URL + "/" + notification.getId() + "/read")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.readStatus").value("read"))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());
    }

    @Test
    void markAllRead_shouldMarkCurrentUserNotificationsRead() throws Exception {
        LoginUser user = login("13800009805");
        notificationService.createIfAbsent(command("batch08:controller:read-all", user.userId()));

        mockMvc.perform(post(NOTIFICATION_URL + "/read-all")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpectAll(successResult());

        mockMvc.perform(get(NOTIFICATION_URL + "/unread-count")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void listMyNotifications_shouldFailWhenNoToken() throws Exception {
        mockMvc.perform(get(NOTIFICATION_URL))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    private LoginUser login(String phone) throws Exception {
        String response = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                    "phone": "%s",
                                    "nickname": "消息测试用户"
                                }
                                """.formatted(phone)))
                .andReturn().getResponse().getContentAsString();
        String token = extractToken(response);
        Long userId = Long.parseLong(response.split("\"id\":\"")[1].split("\"")[0]);
        return new LoginUser(token, userId);
    }

    private CreateNotificationCommand command(String dedupeKey, Long recipientUserId) {
        return CreateNotificationCommand.builder()
                .recipientUserId(recipientUserId)
                .senderUserId(201L)
                .type("order_shipped")
                .title("发货通知")
                .summary("团长已填写物流：顺丰速运 SF1234567890。")
                .body("你的团购订单已发货。")
                .targetType("order")
                .targetId(3001L)
                .actionUrl("/orders/3001")
                .dedupeKey(dedupeKey)
                .build();
    }

    private record LoginUser(String token, Long userId) {
    }
}
