package com.example.groupshop.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.groupshop.chat.dto.ChatConversationResponse;
import com.example.groupshop.chat.dto.ChatMessageResponse;
import com.example.groupshop.chat.dto.ChatUnreadCountResponse;
import com.example.groupshop.chat.dto.SendChatCardRequest;
import com.example.groupshop.chat.dto.SendChatMessageRequest;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.model.entity.ChatConversation;
import com.example.groupshop.model.entity.ChatMessage;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.UploadAsset;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.ChatConversationMapper;
import com.example.groupshop.model.mapper.ChatMessageMapper;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.OrderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import com.example.groupshop.upload.service.UploadAssetService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Long SYSTEM_USER_ID = 0L;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter RESPONSE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final OrderMapper orderMapper;
    private final LeaderMapper leaderMapper;
    private final StoreMapper storeMapper;
    private final UserMapper userMapper;
    private final UploadAssetService uploadAssetService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ChatConversationResponse openByOrder(Long userId, Long orderId) {
        Order order = findOrder(orderId);
        ChatConversation conversation = ensureConversationForOrder(order);
        ensureParticipant(conversation, userId);
        return toConversationResponse(conversation, userId);
    }

    public PageResponse<ChatConversationResponse> listMyConversations(Long userId, String role, int page, int pageSize) {
        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<ChatConversation>()
                .orderByDesc(ChatConversation::getLastMessageAt)
                .orderByDesc(ChatConversation::getUpdatedAt);
        if ("buyer".equals(role)) {
            wrapper.eq(ChatConversation::getBuyerUserId, userId);
        } else if ("leader".equals(role)) {
            wrapper.eq(ChatConversation::getLeaderUserId, userId);
        } else {
            wrapper.and(w -> w.eq(ChatConversation::getBuyerUserId, userId)
                    .or()
                    .eq(ChatConversation::getLeaderUserId, userId));
        }
        Page<ChatConversation> result = conversationMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<ChatConversationResponse> items = result.getRecords().stream()
                .map(conversation -> toConversationResponse(conversation, userId))
                .collect(Collectors.toList());
        return PageResponse.of(items, page, pageSize, result.getTotal());
    }

    public ChatUnreadCountResponse unreadCount(Long userId) {
        List<ChatConversation> conversations = conversationMapper.selectList(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getBuyerUserId, userId)
                .or()
                .eq(ChatConversation::getLeaderUserId, userId));
        long count = 0;
        for (ChatConversation conversation : conversations) {
            if (userId.equals(conversation.getBuyerUserId())) {
                count += safeCount(conversation.getBuyerUnreadCount());
            }
            if (userId.equals(conversation.getLeaderUserId())) {
                count += safeCount(conversation.getLeaderUnreadCount());
            }
        }
        return new ChatUnreadCountResponse(count);
    }

    public PageResponse<ChatMessageResponse> listMessages(Long userId, Long conversationId, Long afterMessageId,
                                                          int pageSize) {
        ChatConversation conversation = findConversationForUser(userId, conversationId);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId);
        if (afterMessageId != null) {
            wrapper.gt(ChatMessage::getId, afterMessageId)
                    .orderByAsc(ChatMessage::getId)
                    .last("LIMIT " + safePageSize(pageSize));
        } else {
            wrapper.orderByDesc(ChatMessage::getId)
                    .last("LIMIT " + safePageSize(pageSize));
        }
        List<ChatMessage> messages = messageMapper.selectList(wrapper);
        if (afterMessageId == null) {
            Collections.reverse(messages);
        }
        List<ChatMessageResponse> items = messages.stream()
                .map(message -> toMessageResponse(message, userId))
                .collect(Collectors.toList());
        return PageResponse.of(items, 1, safePageSize(pageSize), items.size());
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long userId, Long conversationId, SendChatMessageRequest request) {
        ChatConversation conversation = findConversationForUser(userId, conversationId);
        String role = participantRole(conversation, userId);
        String clientMessageId = normalizedClientMessageId(request.getClientMessageId());
        ChatMessage existing = findByClientMessage(conversationId, userId, clientMessageId);
        if (existing != null) {
            return toMessageResponse(existing, userId);
        }

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderUserId(userId);
        message.setSenderRole(role);
        message.setClientMessageId(clientMessageId);
        if ("text".equals(request.getMessageType())) {
            String content = request.getContent() == null ? "" : request.getContent().trim();
            if (content.isEmpty()) {
                throw new BusinessException(ErrorCode.CHAT_MESSAGE_INVALID, "文本消息不能为空");
            }
            message.setMessageType("text");
            message.setContent(content);
        } else if ("image".equals(request.getMessageType())) {
            UploadAsset asset = uploadAssetService.findUsableImageAssetForUploader(
                    request.getImageAssetId(), userId, request.getImageUrl());
            message.setMessageType("image");
            message.setImageAssetId(asset.getId());
            message.setImageUrl(asset.getUrl());
        } else {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_INVALID, "不支持的消息类型");
        }

        insertMessage(message);
        if ("image".equals(message.getMessageType())) {
            uploadAssetService.registerReferences("chat_message", message.getId(), "imageUrl",
                    List.of(message.getImageUrl()));
        }
        updateConversationAfterMessage(conversation.getId(), message, oppositeRole(role));
        return toMessageResponse(message, userId);
    }

    @Transactional
    public ChatMessageResponse sendCard(Long userId, Long conversationId, SendChatCardRequest request) {
        ChatConversation conversation = findConversationForUser(userId, conversationId);
        String role = participantRole(conversation, userId);
        Order order = findOrder(request.getOrderId());
        ensureOrderBelongsToConversation(conversation, order);
        if ("prepare_done".equals(request.getCardType()) && !"leader".equals(role)) {
            throw new BusinessException(ErrorCode.CHAT_CARD_NOT_ALLOWED, "只有团长可以发送备货完成卡片");
        }
        if (!"prepare_done".equals(request.getCardType()) && !"order_summary".equals(request.getCardType())) {
            throw new BusinessException(ErrorCode.CHAT_CARD_NOT_ALLOWED, "不支持的卡片类型");
        }

        String clientMessageId = request.getClientMessageId();
        if (clientMessageId == null || clientMessageId.isBlank()) {
            clientMessageId = "card:" + request.getCardType() + ":" + order.getId() + ":" + userId;
        }
        clientMessageId = normalizedClientMessageId(clientMessageId);
        ChatMessage existing = findByClientMessage(conversationId, userId, clientMessageId);
        if (existing != null) {
            return toMessageResponse(existing, userId);
        }

        ChatMessage message = buildCardMessage(conversation.getId(), userId, role, request.getCardType(),
                order, clientMessageId);
        insertMessage(message);
        updateConversationAfterMessage(conversation.getId(), message, oppositeRole(role));
        return toMessageResponse(message, userId);
    }

    @Transactional
    public void markRead(Long userId, Long conversationId) {
        ChatConversation conversation = findConversationForUser(userId, conversationId);
        LocalDateTime now = nowInShanghai();
        LambdaUpdateWrapper<ChatConversation> wrapper = new LambdaUpdateWrapper<ChatConversation>()
                .eq(ChatConversation::getId, conversationId);
        if (userId.equals(conversation.getBuyerUserId())) {
            wrapper.set(ChatConversation::getBuyerUnreadCount, 0)
                    .set(ChatConversation::getBuyerLastReadAt, now);
        } else {
            wrapper.set(ChatConversation::getLeaderUnreadCount, 0)
                    .set(ChatConversation::getLeaderLastReadAt, now);
        }
        conversationMapper.update(null, wrapper);
    }

    @Transactional
    public void recordOrderCreated(Order order) {
        createSystemOrderCard(order, "order_created", "leader");
    }

    @Transactional
    public void recordOrderPaid(Order order) {
        createSystemOrderCard(order, "order_paid", "leader");
    }

    @Transactional
    public void recordOrderShipped(Order order) {
        createSystemOrderCard(order, "order_shipped", "buyer");
    }

    @Transactional
    public void recordOrderCompleted(Order order) {
        createSystemOrderCard(order, "order_completed", "leader");
    }

    private void createSystemOrderCard(Order order, String cardType, String recipientRole) {
        ChatConversation conversation = ensureConversationForOrder(order);
        String clientMessageId = "system:" + cardType + ":" + order.getId();
        ChatMessage existing = findByClientMessage(conversation.getId(), SYSTEM_USER_ID, clientMessageId);
        if (existing != null) {
            return;
        }
        ChatMessage message = buildCardMessage(conversation.getId(), SYSTEM_USER_ID, "system",
                cardType, order, clientMessageId);
        insertMessage(message);
        updateConversationAfterMessage(conversation.getId(), message, recipientRole);
    }

    private ChatConversation ensureConversationForOrder(Order order) {
        Leader leader = leaderMapper.selectById(order.getLeaderId());
        if (leader == null || leader.getUserId() == null) {
            throw new BusinessException(ErrorCode.CHAT_FORBIDDEN, "订单团长不存在");
        }
        ChatConversation existing = conversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getBuyerUserId, order.getUserId())
                .eq(ChatConversation::getStoreId, order.getStoreId()));
        if (existing != null) {
            return existing;
        }
        ChatConversation conversation = new ChatConversation();
        conversation.setBuyerUserId(order.getUserId());
        conversation.setLeaderUserId(leader.getUserId());
        conversation.setStoreId(order.getStoreId());
        conversation.setBuyerUnreadCount(0);
        conversation.setLeaderUnreadCount(0);
        try {
            conversationMapper.insert(conversation);
            return conversation;
        } catch (DuplicateKeyException ex) {
            return conversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>()
                    .eq(ChatConversation::getBuyerUserId, order.getUserId())
                    .eq(ChatConversation::getStoreId, order.getStoreId()));
        }
    }

    private ChatMessage buildCardMessage(Long conversationId, Long senderUserId, String senderRole,
                                         String cardType, Order order, String clientMessageId) {
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderUserId(senderUserId);
        message.setSenderRole(senderRole);
        message.setMessageType("card");
        message.setCardType(cardType);
        message.setRelatedOrderId(order.getId());
        message.setClientMessageId(clientMessageId);
        message.setContent(cardTitle(cardType));
        message.setCardPayload(toJson(orderCardPayload(cardType, order)));
        return message;
    }

    private Map<String, Object> orderCardPayload(String cardType, Order order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("cardType", cardType);
        payload.put("title", cardTitle(cardType));
        payload.put("summary", cardSummary(cardType, order));
        payload.put("orderId", String.valueOf(order.getId()));
        payload.put("orderNo", order.getOrderNo());
        payload.put("orderStatus", toApiOrderStatus(order.getOrderStatus()));
        payload.put("payAmount", order.getPayAmount());
        payload.put("buyerActionUrl", "/orders/" + order.getId());
        payload.put("leaderActionUrl", "/leader/orders/" + order.getId());
        return payload;
    }

    private String cardTitle(String cardType) {
        return switch (cardType) {
            case "order_created" -> "已创建订单";
            case "order_paid" -> "订单已支付";
            case "order_shipped" -> "团长已发货";
            case "order_completed" -> "订单已完成";
            case "prepare_done" -> "备货完成";
            case "order_summary" -> "订单卡片";
            default -> "订单消息";
        };
    }

    private String cardSummary(String cardType, Order order) {
        return switch (cardType) {
            case "order_created" -> "订单已提交，等待买家完成支付。";
            case "order_paid" -> "买家已完成支付，请团长及时备货履约。";
            case "order_shipped" -> "团长已处理发货，可在订单详情查看履约信息。";
            case "order_completed" -> "买家已确认收货，本单交易完成。";
            case "prepare_done" -> "团长已完成备货，后续会按约定方式发货。";
            case "order_summary" -> "订单号：" + order.getOrderNo();
            default -> "订单状态：" + toApiOrderStatus(order.getOrderStatus());
        };
    }

    private void insertMessage(ChatMessage message) {
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(nowInShanghai());
        }
        try {
            messageMapper.insert(message);
        } catch (DuplicateKeyException ex) {
            ChatMessage existing = findByClientMessage(
                    message.getConversationId(), message.getSenderUserId(), message.getClientMessageId());
            if (existing == null) {
                throw ex;
            }
            message.setId(existing.getId());
            message.setCreatedAt(existing.getCreatedAt());
        }
    }

    private void updateConversationAfterMessage(Long conversationId, ChatMessage message, String roleOrRecipientRole) {
        LocalDateTime now = message.getCreatedAt() != null ? message.getCreatedAt() : nowInShanghai();
        LambdaUpdateWrapper<ChatConversation> wrapper = new LambdaUpdateWrapper<ChatConversation>()
                .eq(ChatConversation::getId, conversationId)
                .set(ChatConversation::getLastMessageId, message.getId())
                .set(ChatConversation::getLastMessageAt, now);
        if ("buyer".equals(roleOrRecipientRole)) {
            wrapper.setSql("buyer_unread_count = buyer_unread_count + 1");
        } else if ("leader".equals(roleOrRecipientRole)) {
            wrapper.setSql("leader_unread_count = leader_unread_count + 1");
        }
        conversationMapper.update(null, wrapper);
    }

    private ChatConversation findConversationForUser(Long userId, Long conversationId) {
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.CHAT_CONVERSATION_NOT_FOUND);
        }
        ensureParticipant(conversation, userId);
        return conversation;
    }

    private void ensureParticipant(ChatConversation conversation, Long userId) {
        if (!userId.equals(conversation.getBuyerUserId()) && !userId.equals(conversation.getLeaderUserId())) {
            throw new BusinessException(ErrorCode.CHAT_FORBIDDEN);
        }
    }

    private String participantRole(ChatConversation conversation, Long userId) {
        if (userId.equals(conversation.getBuyerUserId())) {
            return "buyer";
        }
        if (userId.equals(conversation.getLeaderUserId())) {
            return "leader";
        }
        throw new BusinessException(ErrorCode.CHAT_FORBIDDEN);
    }

    private Order findOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private void ensureOrderBelongsToConversation(ChatConversation conversation, Order order) {
        if (!conversation.getBuyerUserId().equals(order.getUserId())
                || !conversation.getStoreId().equals(order.getStoreId())) {
            throw new BusinessException(ErrorCode.CHAT_CARD_NOT_ALLOWED, "订单不属于当前会话");
        }
    }

    private ChatMessage findByClientMessage(Long conversationId, Long senderUserId, String clientMessageId) {
        if (clientMessageId == null || senderUserId == null) {
            return null;
        }
        return messageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getSenderUserId, senderUserId)
                .eq(ChatMessage::getClientMessageId, clientMessageId));
    }

    private ChatConversationResponse toConversationResponse(ChatConversation conversation, Long currentUserId) {
        User buyer = userMapper.selectById(conversation.getBuyerUserId());
        User leaderUser = userMapper.selectById(conversation.getLeaderUserId());
        Store store = storeMapper.selectById(conversation.getStoreId());
        Leader leader = store == null ? null : leaderMapper.selectById(store.getLeaderId());
        ChatMessage lastMessage = conversation.getLastMessageId() == null
                ? null
                : messageMapper.selectById(conversation.getLastMessageId());
        String role = participantRole(conversation, currentUserId);
        return ChatConversationResponse.builder()
                .id(conversation.getId())
                .buyerUserId(conversation.getBuyerUserId())
                .leaderUserId(conversation.getLeaderUserId())
                .storeId(conversation.getStoreId())
                .buyerName(buyer == null ? "买家" : buyer.getNickname())
                .buyerAvatarUrl(buyer == null ? null : buyer.getAvatarUrl())
                .leaderName(leader == null ? (leaderUser == null ? "团长" : leaderUser.getNickname()) : leader.getDisplayName())
                .leaderAvatarUrl(leader == null ? (leaderUser == null ? null : leaderUser.getAvatarUrl()) : leader.getAvatarUrl())
                .storeName(store == null ? "团长小店" : store.getName())
                .storeLogoUrl(store == null ? null : store.getLogoUrl())
                .currentUserRole(role)
                .unreadCount("buyer".equals(role) ? safeCount(conversation.getBuyerUnreadCount())
                        : safeCount(conversation.getLeaderUnreadCount()))
                .lastMessageId(conversation.getLastMessageId())
                .lastMessageText(lastMessageText(lastMessage))
                .lastMessageType(lastMessage == null ? null : lastMessage.getMessageType())
                .lastMessageAt(formatShanghaiTime(conversation.getLastMessageAt()))
                .createdAt(formatShanghaiTime(conversation.getCreatedAt()))
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message, Long currentUserId) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderUserId(message.getSenderUserId())
                .senderRole(message.getSenderRole())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .imageAssetId(message.getImageAssetId())
                .imageUrl(message.getImageUrl())
                .cardType(message.getCardType())
                .cardPayload(parseCardPayload(message.getCardPayload()))
                .relatedOrderId(message.getRelatedOrderId())
                .mine(currentUserId.equals(message.getSenderUserId()))
                .createdAt(formatShanghaiTime(message.getCreatedAt()))
                .build();
    }

    private LocalDateTime nowInShanghai() {
        return LocalDateTime.now(SHANGHAI_ZONE);
    }

    private String formatShanghaiTime(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(SHANGHAI_ZONE).format(RESPONSE_TIME_FORMATTER);
    }

    private String lastMessageText(ChatMessage message) {
        if (message == null) {
            return null;
        }
        if ("image".equals(message.getMessageType())) {
            return "[图片]";
        }
        if ("card".equals(message.getMessageType())) {
            return cardTitle(message.getCardType());
        }
        return message.getContent();
    }

    private Map<String, Object> parseCardPayload(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "消息卡片序列化失败");
        }
    }

    private String normalizedClientMessageId(String clientMessageId) {
        if (clientMessageId == null || clientMessageId.isBlank()) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_INVALID, "clientMessageId 不能为空");
        }
        return clientMessageId.trim();
    }

    private int safePageSize(int pageSize) {
        return Math.max(1, Math.min(pageSize, 50));
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : count;
    }

    private String oppositeRole(String role) {
        return "buyer".equals(role) ? "leader" : "buyer";
    }

    private String toApiOrderStatus(String dbStatus) {
        if ("pending_pay".equals(dbStatus)) {
            return "pendingPay";
        }
        return dbStatus;
    }
}
