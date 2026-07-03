package com.example.groupshop.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Notification;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.model.entity.Subscription;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.NotificationMapper;
import com.example.groupshop.model.mapper.SubscriptionMapper;
import com.example.groupshop.notification.dto.CreateNotificationCommand;
import com.example.groupshop.notification.dto.NotificationResponse;
import com.example.groupshop.notification.dto.UnreadCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final LeaderMapper leaderMapper;
    private final SubscriptionMapper subscriptionMapper;

    public PageResponse<NotificationResponse> listMyNotifications(Long userId, String type, boolean unreadOnly,
                                                                   int page, int pageSize) {
        Page<Notification> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getRecipientUserId, userId)
                .orderByDesc(Notification::getCreatedAt);
        if (type != null && !type.isBlank()) {
            wrapper.eq(Notification::getType, type);
        }
        if (unreadOnly) {
            wrapper.eq(Notification::getReadStatus, "unread");
        }
        Page<Notification> result = notificationMapper.selectPage(pageObj, wrapper);
        List<NotificationResponse> items = result.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(items, page, pageSize, result.getTotal());
    }

    public UnreadCountResponse unreadCount(Long userId) {
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getRecipientUserId, userId)
                .eq(Notification::getReadStatus, "unread"));
        return new UnreadCountResponse(count == null ? 0 : count);
    }

    public NotificationResponse getMyNotification(Long userId, Long notificationId) {
        return toResponse(findForUser(userId, notificationId));
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = findForUser(userId, notificationId);
        if (!"read".equals(notification.getReadStatus())) {
            LocalDateTime now = LocalDateTime.now();
            notification.setReadStatus("read");
            notification.setReadAt(now);
            notificationMapper.updateById(notification);
        }
        return toResponse(notification);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getRecipientUserId, userId)
                .eq(Notification::getReadStatus, "unread")
                .set(Notification::getReadStatus, "read")
                .set(Notification::getReadAt, LocalDateTime.now()));
    }

    @Transactional
    public void notifyOrderPaid(Order order, Long senderUserId) {
        createIfAbsent(CreateNotificationCommand.builder()
                .recipientUserId(order.getUserId())
                .senderUserId(senderUserId)
                .type("order_paid")
                .title("支付成功")
                .summary("你购买的团购订单已支付成功，等待团长发货。")
                .body("订单已完成模拟支付，团长会尽快处理发货。")
                .targetType("order")
                .targetId(order.getId())
                .actionUrl("/orders/" + order.getId())
                .dedupeKey("order_paid:" + order.getId() + ":" + order.getUserId())
                .build());

        Leader leader = leaderMapper.selectById(order.getLeaderId());
        if (leader != null && leader.getUserId() != null) {
            createIfAbsent(CreateNotificationCommand.builder()
                    .recipientUserId(leader.getUserId())
                    .senderUserId(senderUserId)
                    .type("order_paid")
                    .title("有新待发货订单")
                    .summary("买家已完成支付，请及时处理发货。")
                    .body("有订单完成模拟支付，已进入待发货状态。")
                    .targetType("order")
                    .targetId(order.getId())
                    .actionUrl("/leader/orders/" + order.getId())
                    .dedupeKey("order_paid:" + order.getId() + ":" + leader.getUserId())
                    .build());
        }
    }

    @Transactional
    public void notifyOrderShipped(Order order, String logisticsCompany, String trackingNo, Long senderUserId) {
        String logistics = logisticsCompany == null || logisticsCompany.isBlank()
                ? "团长已处理发货"
                : "团长已填写物流：" + logisticsCompany + (trackingNo == null || trackingNo.isBlank() ? "" : " " + trackingNo);
        createIfAbsent(CreateNotificationCommand.builder()
                .recipientUserId(order.getUserId())
                .senderUserId(senderUserId)
                .type("order_shipped")
                .title("发货通知")
                .summary(logistics + "。")
                .body("你的团购订单已发货，可在订单详情查看履约信息。")
                .targetType("order")
                .targetId(order.getId())
                .actionUrl("/orders/" + order.getId())
                .dedupeKey("order_shipped:" + order.getId() + ":" + order.getUserId())
                .build());
    }

    @Transactional
    public void notifyOrderCompleted(Order order, Long senderUserId) {
        Leader leader = leaderMapper.selectById(order.getLeaderId());
        if (leader == null || leader.getUserId() == null) {
            return;
        }
        createIfAbsent(CreateNotificationCommand.builder()
                .recipientUserId(leader.getUserId())
                .senderUserId(senderUserId)
                .type("order_completed")
                .title("订单已完成")
                .summary("买家已确认收货，本单交易完成。")
                .body("买家已确认收到商品，订单状态已更新为已完成。")
                .targetType("order")
                .targetId(order.getId())
                .actionUrl("/leader/orders/" + order.getId())
                .dedupeKey("order_completed:" + order.getId() + ":" + leader.getUserId())
                .build());
    }

    @Transactional
    public void notifySubscriptionCreated(Subscription subscription, Long senderUserId) {
        Leader leader = leaderMapper.selectById(subscription.getLeaderId());
        if (leader == null || leader.getUserId() == null) {
            return;
        }
        createIfAbsent(CreateNotificationCommand.builder()
                .recipientUserId(leader.getUserId())
                .senderUserId(senderUserId)
                .type("subscription_created")
                .title("新增订阅")
                .summary("有用户订阅了你的团长主页。")
                .body("新的订阅关系已建立，后续开团通知可触达该用户。")
                .targetType("leader")
                .targetId(subscription.getLeaderId())
                .actionUrl("/leader/subscribers?subscriptionId=" + subscription.getId())
                .dedupeKey("subscription_created:" + subscription.getId() + ":" + leader.getUserId())
                .build());
    }

    @Transactional
    public void notifyGroupBuyPublished(GroupBuy groupBuy, Long senderUserId) {
        List<Subscription> subscriptions = subscriptionMapper.selectList(new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getLeaderId, groupBuy.getLeaderId())
                .eq(Subscription::getStatus, "active"));
        for (Subscription subscription : subscriptions) {
            createIfAbsent(CreateNotificationCommand.builder()
                    .recipientUserId(subscription.getUserId())
                    .senderUserId(senderUserId)
                    .type("group_buy_published")
                    .title("你订阅的团长开新团了")
                    .summary(groupBuy.getTitle())
                    .body("你订阅的团长发布了新的团购活动。")
                    .targetType("group_buy")
                    .targetId(groupBuy.getId())
                    .actionUrl("/group-buys/" + groupBuy.getId())
                    .dedupeKey("group_buy_published:" + groupBuy.getId() + ":" + subscription.getUserId())
                    .build());
        }
    }

    @Transactional
    public NotificationResponse createIfAbsent(CreateNotificationCommand command) {
        Long existing = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getDedupeKey, command.getDedupeKey()));
        if (existing != null && existing > 0) {
            Notification notification = notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getDedupeKey, command.getDedupeKey()));
            return toResponse(notification);
        }

        Notification notification = new Notification();
        notification.setRecipientUserId(command.getRecipientUserId());
        notification.setSenderUserId(command.getSenderUserId());
        notification.setType(command.getType());
        notification.setTitle(command.getTitle());
        notification.setSummary(command.getSummary());
        notification.setBody(command.getBody());
        notification.setTargetType(command.getTargetType());
        notification.setTargetId(command.getTargetId());
        notification.setActionUrl(command.getActionUrl());
        notification.setDedupeKey(command.getDedupeKey());
        notification.setReadStatus("unread");
        try {
            notificationMapper.insert(notification);
        } catch (DuplicateKeyException ex) {
            notification = notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getDedupeKey, command.getDedupeKey()));
        }
        return toResponse(notification);
    }

    private Notification findForUser(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        if (!notification.getRecipientUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }
        return notification;
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .summary(notification.getSummary())
                .body(notification.getBody())
                .targetType(notification.getTargetType())
                .targetId(notification.getTargetId())
                .actionUrl(notification.getActionUrl())
                .readStatus(notification.getReadStatus())
                .readAt(notification.getReadAt() != null ? notification.getReadAt().toString() : null)
                .createdAt(notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : null)
                .build();
    }
}
