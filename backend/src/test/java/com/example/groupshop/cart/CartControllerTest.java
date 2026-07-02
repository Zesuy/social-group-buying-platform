package com.example.groupshop.cart;

import com.example.groupshop.base.MockMvcTestBase;
import com.example.groupshop.cart.dto.AddCartItemRequest;
import com.example.groupshop.cart.dto.UpdateCartItemRequest;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * MockMvc tests for {@link com.example.groupshop.cart.controller.CartController}.
 */
@Transactional
class CartControllerTest extends MockMvcTestBase {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaderMapper leaderMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private GroupBuyService groupBuyService;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private Long groupBuyItemId;
    private Long secondItemId;
    private Long groupBuyId;

    @BeforeEach
    void setUp() throws Exception {
        // Register and login
        User user = new User();
        user.setNickname("测试用户");
        user.setPhone("13800000001");
        user.setStatus("normal");
        userMapper.insert(user);

        String loginBody = "{\"phone\":\"13800000001\"}";
        String loginResp = mockMvc.perform(post("/api/v1/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andReturn().getResponse().getContentAsString();
        token = extractToken(loginResp);

        // Set up leader and store
        // Register a phone for leader
        User leaderUser = new User();
        leaderUser.setNickname("团长用户");
        leaderUser.setPhone("13800000002");
        leaderUser.setStatus("normal");
        userMapper.insert(leaderUser);

        Leader leader = new Leader();
        leader.setUserId(leaderUser.getId());
        leader.setDisplayName("团长");
        leader.setServiceStatus("normal");
        leader.setMemberCount(0);
        leader.setFollowerCount(0);
        leaderMapper.insert(leader);

        Store store = new Store();
        store.setLeaderId(leader.getId());
        store.setName("店铺");
        store.setDefaultDeliveryType("express");
        store.setDistributionEnabled(false);
        store.setStatus("active");
        storeMapper.insert(store);

        // Also login as leader to ensure we can create group buy
        String leaderLoginBody = "{\"phone\":\"13800000002\"}";
        String leaderLoginResp = mockMvc.perform(post("/api/v1/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(leaderLoginBody))
                .andReturn().getResponse().getContentAsString();
        String leaderToken = extractToken(leaderLoginResp);

        // Create a group buy as leader
        CreateGroupBuyRequest gbRequest = new CreateGroupBuyRequest();
        gbRequest.setTitle("测试团购");
        gbRequest.setDeliveryType(com.example.groupshop.common.enums.DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item1 = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct p1 = new CreateGroupBuyRequest.InlineProduct();
        p1.setName("商品A");
        p1.setBasePriceAmount(2000L);
        p1.setStock(100);
        item1.setProduct(p1);
        item1.setDisplayName("商品A");
        item1.setGroupPriceAmount(1990L);
        item1.setGroupStock(50);
        item1.setSortOrder(1);

        CreateGroupBuyRequest.ItemEntry item2 = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct p2 = new CreateGroupBuyRequest.InlineProduct();
        p2.setName("商品B");
        p2.setBasePriceAmount(3000L);
        p2.setStock(100);
        item2.setProduct(p2);
        item2.setDisplayName("商品B");
        item2.setGroupPriceAmount(2990L);
        item2.setGroupStock(30);
        item2.setSortOrder(2);

        gbRequest.setItems(List.of(item1, item2));

        // Use leader token to create group buy via mockMvc
        String gbJson = objectMapper.writeValueAsString(gbRequest);
        String gbResp = mockMvc.perform(post("/api/v1/my/store/group-buys")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gbJson))
                .andExpect(successResult())
                .andReturn().getResponse().getContentAsString();

        // Parse response to get IDs
        com.fasterxml.jackson.databind.JsonNode gbNode = objectMapper.readTree(gbResp);
        groupBuyId = gbNode.get("data").get("groupBuy").get("id").asLong();
        groupBuyItemId = gbNode.get("data").get("items").get(0).get("id").asLong();
        secondItemId = gbNode.get("data").get("items").get(1).get("id").asLong();
    }

    // ── Cart CRUD ───────────────────────────────────────────────────────

    @Test
    void listCartItems_shouldReturnEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(successResult())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void addCartItem_shouldSucceed() throws Exception {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .groupBuyItemId(groupBuyItemId)
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(successResult())
                .andExpect(jsonPath("$.data.cartItemId").exists())
                .andExpect(jsonPath("$.data.quantity").value(2));
    }

    @Test
    void addCartItem_shouldMergeForDuplicate() throws Exception {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .groupBuyItemId(groupBuyItemId)
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/v1/cart/items")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(successResult())
                .andExpect(jsonPath("$.data.quantity").value(4));
    }

    @Test
    void addCartItem_shouldFailWhenNotLoggedIn() throws Exception {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .groupBuyItemId(groupBuyItemId)
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateCartItem_shouldSucceed() throws Exception {
        // Add first
        AddCartItemRequest addReq = AddCartItemRequest.builder()
                .groupBuyItemId(groupBuyItemId)
                .quantity(2)
                .build();
        String addResp = mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andReturn().getResponse().getContentAsString();
        Long cartItemId = objectMapper.readTree(addResp).get("data").get("cartItemId").asLong();

        UpdateCartItemRequest updateReq = UpdateCartItemRequest.builder()
                .quantity(5)
                .build();
        mockMvc.perform(patch("/api/v1/cart/items/{cartItemId}", cartItemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(successResult())
                .andExpect(jsonPath("$.data.quantity").value(5));
    }

    @Test
    void deleteCartItem_shouldSucceed() throws Exception {
        AddCartItemRequest addReq = AddCartItemRequest.builder()
                .groupBuyItemId(groupBuyItemId)
                .quantity(2)
                .build();
        String addResp = mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(successResult())
                .andReturn().getResponse().getContentAsString();
        Long cartItemId = objectMapper.readTree(addResp).get("data").get("cartItemId").asLong();

        mockMvc.perform(delete("/api/v1/cart/items/{cartItemId}", cartItemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(successResult());
    }

    @Test
    void clearCart_shouldSucceed() throws Exception {
        // Add two items
        mockMvc.perform(post("/api/v1/cart/items")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(1).build())));

        mockMvc.perform(delete("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(successResult());
    }

    // ── Checkout Preview ────────────────────────────────────────────────

    @Test
    void checkoutPreview_shouldSucceed() throws Exception {
        // Add item to cart first
        AddCartItemRequest addReq = AddCartItemRequest.builder()
                .groupBuyItemId(groupBuyItemId)
                .quantity(2)
                .build();
        String addResp = mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(successResult())
                .andReturn().getResponse().getContentAsString();
        Long cartItemId = objectMapper.readTree(addResp).get("data").get("cartItemId").asLong();

        // Create address for the buyer
        String addrBody = "{\"receiverName\":\"张三\",\"receiverPhone\":\"13800000001\","
                + "\"province\":\"浙江省\",\"city\":\"杭州市\",\"district\":\"西湖区\",\"detail\":\"某某路1号\"}";
        String addrResp = mockMvc.perform(post("/api/v1/my/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addrBody))
                .andExpect(successResult())
                .andReturn().getResponse().getContentAsString();
        Long addressId = objectMapper.readTree(addrResp).get("data").get("id").asLong();

        // Checkout preview
        mockMvc.perform(post("/api/v1/cart/checkout-preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + ",\"cartItemIds\":[" + cartItemId + "]}"))
                .andExpect(successResult())
                .andExpect(jsonPath("$.data.groupBuyId").exists())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalAmount").value(3980));
    }

    @Test
    void checkoutPreview_shouldFailWhenMissingCartItemIds() throws Exception {
        String addrBody = "{\"receiverName\":\"张三\",\"receiverPhone\":\"13800000001\","
                + "\"province\":\"浙江省\",\"city\":\"杭州市\",\"district\":\"西湖区\",\"detail\":\"某某路1号\"}";
        String addrResp = mockMvc.perform(post("/api/v1/my/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addrBody))
                .andExpect(successResult())
                .andReturn().getResponse().getContentAsString();
        Long addressId = objectMapper.readTree(addrResp).get("data").get("id").asLong();

        // Missing cartItemIds should fail validation
        mockMvc.perform(post("/api/v1/cart/checkout-preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + ",\"cartItemIds\":[]}"))
                .andExpect(errorResult("VALIDATION_ERROR"));
    }

    // ── Negative quantity ───────────────────────────────────────────────

    @Test
    void addCartItem_shouldFailWhenQuantityZero() throws Exception {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .groupBuyItemId(groupBuyItemId)
                .quantity(0)
                .build();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(errorResult("VALIDATION_ERROR"));
    }
}
