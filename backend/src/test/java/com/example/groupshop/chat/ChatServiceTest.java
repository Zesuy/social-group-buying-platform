package com.example.groupshop.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.chat.dto.ChatConversationResponse;
import com.example.groupshop.chat.dto.ChatMessageResponse;
import com.example.groupshop.chat.dto.SendChatCardRequest;
import com.example.groupshop.chat.dto.SendChatMessageRequest;
import com.example.groupshop.chat.service.ChatService;
import com.example.groupshop.common.enums.DeliveryType;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.model.entity.ChatMessage;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.UploadAsset;
import com.example.groupshop.model.entity.UploadAssetReference;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.ChatMessageMapper;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UploadAssetReferenceMapper;
import com.example.groupshop.model.mapper.UserMapper;
import com.example.groupshop.model.mapper.OrderMapper;
import com.example.groupshop.upload.service.UploadAssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ChatServiceTest extends ServiceTestBase {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaderMapper leaderMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ChatMessageMapper messageMapper;

    @Autowired
    private UploadAssetService uploadAssetService;

    @Autowired
    private UploadAssetReferenceMapper referenceMapper;

    private Long buyerUserId;
    private Long leaderUserId;
    private Long otherUserId;
    private Long orderId;

    @BeforeEach
    void setUp() {
        User buyer = user("聊天买家", "13807000001");
        userMapper.insert(buyer);
        buyerUserId = buyer.getId();

        User leaderUser = user("聊天团长用户", "13807000002");
        userMapper.insert(leaderUser);
        leaderUserId = leaderUser.getId();

        User otherUser = user("无关用户", "13807000003");
        userMapper.insert(otherUser);
        otherUserId = otherUser.getId();

        Leader leader = new Leader();
        leader.setUserId(leaderUserId);
        leader.setDisplayName("聊天团长");
        leader.setServiceStatus("normal");
        leader.setMemberCount(0);
        leader.setFollowerCount(0);
        leaderMapper.insert(leader);

        Store store = new Store();
        store.setLeaderId(leader.getId());
        store.setName("聊天小店");
        store.setDefaultDeliveryType(DeliveryType.EXPRESS.getValue());
        store.setDistributionEnabled(false);
        store.setStatus("active");
        storeMapper.insert(store);

        Order order = new Order();
        order.setOrderNo("CHAT" + System.nanoTime());
        order.setUserId(buyerUserId);
        order.setLeaderId(leader.getId());
        order.setStoreId(store.getId());
        order.setGroupBuyId(10001L);
        order.setAddressId(20001L);
        order.setReceiverName("买家");
        order.setReceiverPhone("13807000001");
        order.setProvince("浙江省");
        order.setCity("杭州市");
        order.setDistrict("西湖区");
        order.setDetail("测试地址");
        order.setFullAddress("浙江省杭州市西湖区测试地址");
        order.setTotalAmount(1990L);
        order.setDiscountAmount(0L);
        order.setPayAmount(1990L);
        order.setPayStatus("unpaid");
        order.setOrderStatus("pending_pay");
        orderMapper.insert(order);
        orderId = order.getId();
    }

    @Test
    void openByOrder_shouldDedupeAndRejectUnrelatedUser() {
        ChatConversationResponse buyerView = chatService.openByOrder(buyerUserId, orderId);
        ChatConversationResponse leaderView = chatService.openByOrder(leaderUserId, orderId);

        assertThat(leaderView.getId()).isEqualTo(buyerView.getId());
        assertThat(buyerView.getCurrentUserRole()).isEqualTo("buyer");
        assertThat(leaderView.getCurrentUserRole()).isEqualTo("leader");

        assertThatThrownBy(() -> chatService.openByOrder(otherUserId, orderId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CHAT_FORBIDDEN);
    }

    @Test
    void sendText_shouldDedupeAndUpdateUnreadCount() {
        ChatConversationResponse conversation = chatService.openByOrder(buyerUserId, orderId);
        SendChatMessageRequest request = new SendChatMessageRequest();
        request.setMessageType("text");
        request.setContent("什么时候发货");
        request.setClientMessageId("client-text-1");

        ChatMessageResponse first = chatService.sendMessage(buyerUserId, conversation.getId(), request);
        ChatMessageResponse second = chatService.sendMessage(buyerUserId, conversation.getId(), request);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(first.getCreatedAt()).endsWith("+08:00");
        assertThat(chatService.listMyConversations(leaderUserId, "leader", 1, 10)
                .getItems()
                .get(0)
                .getLastMessageAt()).endsWith("+08:00");
        assertThat(chatService.unreadCount(leaderUserId).getUnreadCount()).isEqualTo(1);

        chatService.markRead(leaderUserId, conversation.getId());

        assertThat(chatService.unreadCount(leaderUserId).getUnreadCount()).isZero();
    }

    @Test
    void sendImage_shouldRequireOwnUploadAssetAndRegisterReference() {
        ChatConversationResponse conversation = chatService.openByOrder(buyerUserId, orderId);
        UploadAsset asset = uploadAssetService.recordUpload(
                buyerUserId,
                "images/chat-test.png",
                "/uploads/images/chat-test.png",
                "chat-test.png",
                "image/png",
                128,
                new byte[] {1, 2, 3});
        SendChatMessageRequest request = new SendChatMessageRequest();
        request.setMessageType("image");
        request.setImageAssetId(asset.getId());
        request.setImageUrl(asset.getUrl());
        request.setClientMessageId("client-image-1");

        ChatMessageResponse sent = chatService.sendMessage(buyerUserId, conversation.getId(), request);

        assertThat(sent.getMessageType()).isEqualTo("image");
        assertThat(referenceMapper.selectCount(new LambdaQueryWrapper<UploadAssetReference>()
                .eq(UploadAssetReference::getRefType, "chat_message")
                .eq(UploadAssetReference::getRefId, sent.getId())
                .eq(UploadAssetReference::getFieldName, "imageUrl"))).isEqualTo(1);

        request.setClientMessageId("client-image-2");
        assertThatThrownBy(() -> chatService.sendMessage(otherUserId, conversation.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CHAT_FORBIDDEN);
    }

    @Test
    void prepareDoneCard_shouldOnlyAllowLeader() {
        ChatConversationResponse conversation = chatService.openByOrder(buyerUserId, orderId);
        SendChatCardRequest request = new SendChatCardRequest();
        request.setCardType("prepare_done");
        request.setOrderId(orderId);

        assertThatThrownBy(() -> chatService.sendCard(buyerUserId, conversation.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CHAT_CARD_NOT_ALLOWED);

        ChatMessageResponse sent = chatService.sendCard(leaderUserId, conversation.getId(), request);

        assertThat(sent.getCardType()).isEqualTo("prepare_done");
        assertThat(chatService.unreadCount(buyerUserId).getUnreadCount()).isEqualTo(1);
    }

    @Test
    void recordOrderCreated_shouldCreateDedupeSystemCard() {
        Order order = orderMapper.selectById(orderId);

        chatService.recordOrderCreated(order);
        chatService.recordOrderCreated(order);

        ChatConversationResponse conversation = chatService.openByOrder(buyerUserId, orderId);
        assertThat(messageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversation.getId())
                .eq(ChatMessage::getCardType, "order_created"))).isEqualTo(1);
        assertThat(chatService.unreadCount(leaderUserId).getUnreadCount()).isEqualTo(1);
    }

    private User user(String nickname, String phone) {
        User user = new User();
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setStatus("normal");
        return user;
    }
}
