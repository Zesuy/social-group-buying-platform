package com.example.groupshop.chat;

import com.example.groupshop.base.MockMvcTestBase;
import com.example.groupshop.common.enums.DeliveryType;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.OrderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String CHAT_URL = "/api/v1/my/chat-conversations";

    @Autowired
    private LeaderMapper leaderMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Test
    void chatConversationFlow_shouldOpenSendReadAndCount() throws Exception {
        LoginUser buyer = login("13807001001", "聊天买家");
        LoginUser leaderUser = login("13807001002", "聊天团长");
        Long orderId = createOrderFixture(buyer.userId(), leaderUser.userId());

        String openResponse = mockMvc.perform(post(CHAT_URL + "/orders/" + orderId)
                        .header("Authorization", "Bearer " + buyer.token()))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.currentUserRole").value("buyer"))
                .andReturn().getResponse().getContentAsString();
        String conversationId = openResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(post(CHAT_URL + "/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + buyer.token())
                        .contentType("application/json")
                        .content("""
                                {
                                  "messageType": "text",
                                  "content": "请问什么时候发货",
                                  "clientMessageId": "mvc-text-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.messageType").value("text"));

        mockMvc.perform(get(CHAT_URL + "/unread-count")
                        .header("Authorization", "Bearer " + leaderUser.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        mockMvc.perform(post(CHAT_URL + "/" + conversationId + "/read")
                        .header("Authorization", "Bearer " + leaderUser.token()))
                .andExpect(status().isOk())
                .andExpectAll(successResult());

        mockMvc.perform(get(CHAT_URL + "/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + leaderUser.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].content").value("请问什么时候发货"));
    }

    @Test
    void listConversations_shouldFailWhenNoToken() throws Exception {
        mockMvc.perform(get(CHAT_URL))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    private LoginUser login(String phone, String nickname) throws Exception {
        String response = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("""
                                {
                                  "phone": "%s",
                                  "nickname": "%s"
                                }
                                """.formatted(phone, nickname)))
                .andReturn().getResponse().getContentAsString();
        String token = extractToken(response);
        Long userId = Long.parseLong(response.split("\"id\":\"")[1].split("\"")[0]);
        return new LoginUser(token, userId);
    }

    private Long createOrderFixture(Long buyerUserId, Long leaderUserId) {
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
        order.setOrderNo("CHATMVC" + System.nanoTime());
        order.setUserId(buyerUserId);
        order.setLeaderId(leader.getId());
        order.setStoreId(store.getId());
        order.setGroupBuyId(90001L);
        order.setAddressId(90002L);
        order.setReceiverName("买家");
        order.setReceiverPhone("13807001001");
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
        return order.getId();
    }

    private record LoginUser(String token, Long userId) {
    }
}
