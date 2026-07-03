package com.example.groupshop.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.model.mapper.NotificationMapper;
import com.example.groupshop.notification.dto.CreateNotificationCommand;
import com.example.groupshop.notification.dto.NotificationResponse;
import com.example.groupshop.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationServiceTest extends ServiceTestBase {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationMapper notificationMapper;

    @BeforeEach
    void setUp() {
        notificationMapper.delete(new LambdaQueryWrapper<>());
    }

    @Test
    void createIfAbsent_shouldDedupeAndExposeUnreadList() {
        CreateNotificationCommand command = command("batch08:dedupe:1", 101L);

        NotificationResponse first = notificationService.createIfAbsent(command);
        NotificationResponse second = notificationService.createIfAbsent(command);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(notificationService.unreadCount(101L).getUnreadCount()).isEqualTo(1);
        assertThat(notificationService.listMyNotifications(101L, null, true, 1, 20).getItems())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getTitle()).isEqualTo("发货通知");
                    assertThat(item.getReadStatus()).isEqualTo("unread");
                    assertThat(item.getActionUrl()).isEqualTo("/orders/3001");
                });
    }

    @Test
    void markRead_shouldUpdateUnreadCount() {
        NotificationResponse created = notificationService.createIfAbsent(command("batch08:read:1", 102L));

        NotificationResponse read = notificationService.markRead(102L, created.getId());

        assertThat(read.getReadStatus()).isEqualTo("read");
        assertThat(read.getReadAt()).isNotBlank();
        assertThat(notificationService.unreadCount(102L).getUnreadCount()).isZero();
    }

    @Test
    void markAllRead_shouldOnlyAffectCurrentUser() {
        notificationService.createIfAbsent(command("batch08:all:current", 103L));
        notificationService.createIfAbsent(command("batch08:all:other", 104L));

        notificationService.markAllRead(103L);

        assertThat(notificationService.unreadCount(103L).getUnreadCount()).isZero();
        assertThat(notificationService.unreadCount(104L).getUnreadCount()).isEqualTo(1);
    }

    @Test
    void getMyNotification_shouldRejectOtherRecipient() {
        NotificationResponse created = notificationService.createIfAbsent(command("batch08:forbidden:1", 105L));

        assertThatThrownBy(() -> notificationService.getMyNotification(106L, created.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_FORBIDDEN);
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
}
