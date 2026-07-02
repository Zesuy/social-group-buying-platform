package com.example.groupshop.cart;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.cart.dto.AddCartItemRequest;
import com.example.groupshop.cart.dto.CartCheckoutPreviewRequest;
import com.example.groupshop.cart.dto.CartItemResponse;
import com.example.groupshop.cart.dto.UpdateCartItemRequest;
import com.example.groupshop.cart.service.CartCheckoutPreviewResult;
import com.example.groupshop.cart.service.CartService;
import com.example.groupshop.common.enums.DeliveryType;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.GroupBuyShareToken;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.GroupBuyMapper;
import com.example.groupshop.model.mapper.GroupBuyShareTokenMapper;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CartService}.
 */
@Transactional
class CartServiceTest extends ServiceTestBase {

    @Autowired
    private CartService cartService;

    @Autowired
    private GroupBuyService groupBuyService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaderMapper leaderMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private GroupBuyMapper groupBuyMapper;

    @Autowired
    private GroupBuyShareTokenMapper shareTokenMapper;

    private Long userId;
    private Long otherUserId;
    private Long leaderUserId;
    private Long groupBuyId;
    private Long groupBuyItemId;
    private Long secondItemId;

    @BeforeEach
    void setUp() {
        // Set up leader and store
        User leaderUser = new User();
        leaderUser.setNickname("团长");
        leaderUser.setPhone("13800009901");
        leaderUser.setStatus("normal");
        userMapper.insert(leaderUser);
        leaderUserId = leaderUser.getId();

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

        // Create a group buy with two items
        CreateGroupBuyRequest gbRequest = new CreateGroupBuyRequest();
        gbRequest.setTitle("测试团购");
        gbRequest.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item1 = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inline1 = new CreateGroupBuyRequest.InlineProduct();
        inline1.setName("商品A");
        inline1.setBasePriceAmount(2000L);
        inline1.setStock(100);
        item1.setProduct(inline1);
        item1.setDisplayName("商品A");
        item1.setGroupPriceAmount(1990L);
        item1.setGroupStock(50);
        item1.setSortOrder(1);

        CreateGroupBuyRequest.ItemEntry item2 = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inline2 = new CreateGroupBuyRequest.InlineProduct();
        inline2.setName("商品B");
        inline2.setBasePriceAmount(3000L);
        inline2.setStock(100);
        item2.setProduct(inline2);
        item2.setDisplayName("商品B");
        item2.setGroupPriceAmount(2990L);
        item2.setGroupStock(30);
        item2.setSortOrder(2);

        gbRequest.setItems(List.of(item1, item2));

        GroupBuyResponse gbResponse = groupBuyService.createGroupBuy(leaderUser.getId(), gbRequest);
        groupBuyId = gbResponse.getGroupBuy().getId();
        groupBuyItemId = gbResponse.getItems().get(0).getId();
        secondItemId = gbResponse.getItems().get(1).getId();

        // Set up buyer users
        User buyer = new User();
        buyer.setNickname("买家");
        buyer.setPhone("13800009902");
        buyer.setStatus("normal");
        userMapper.insert(buyer);
        userId = buyer.getId();

        User buyer2 = new User();
        buyer2.setNickname("买家2");
        buyer2.setPhone("13800009903");
        buyer2.setStatus("normal");
        userMapper.insert(buyer2);
        otherUserId = buyer2.getId();
    }

    // ── Add to Cart ─────────────────────────────────────────────────────

    @Test
    void addCartItem_shouldSucceed() {
        CartItemResponse response = cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(2).build());

        assertThat(response.getCartItemId()).isPositive();
        assertThat(response.getGroupBuyItemId()).isEqualTo(groupBuyItemId);
        assertThat(response.getQuantity()).isEqualTo(2);
        assertThat(response.getTitle()).isEqualTo("商品A");
        assertThat(response.getGroupPriceAmount()).isEqualTo(1990L);
    }

    @Test
    void addCartItem_shouldMergeQuantitiesForDuplicate() {
        cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(3).build());
        CartItemResponse merged = cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(2).build());

        assertThat(merged.getQuantity()).isEqualTo(5);
    }

    @Test
    void addCartItem_shouldFailWhenInsufficientStock() {
        assertThatThrownBy(() -> cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(999).build()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    void addCartItem_shouldFailWhenItemNotExists() {
        assertThatThrownBy(() -> cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(99999L).quantity(1).build()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ITEM_NOT_IN_GROUP_BUY);
    }

    @Test
    void addCartItem_shouldFailWhenGroupBuyEnded() {
        // End the group buy by setting endTime to past
        groupBuyMapper.update(null,
                new LambdaUpdateWrapper<GroupBuy>()
                        .eq(GroupBuy::getId, groupBuyId)
                        .set(GroupBuy::getEndTime, LocalDateTime.now().minusHours(1)));

        assertThatThrownBy(() -> cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(1).build()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_BUY_ENDED);
    }

    // ── List Cart ───────────────────────────────────────────────────────

    @Test
    void getCartItems_shouldReturnUserItemsOnly() {
        cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(2).build());
        cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(secondItemId).quantity(1).build());
        // Other user's cart
        cartService.addCartItem(otherUserId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(3).build());

        List<CartItemResponse> items = cartService.getCartItems(userId);

        assertThat(items).hasSize(2);
        assertThat(cartService.getCartItems(otherUserId)).hasSize(1);
    }

    // ── Update Cart ─────────────────────────────────────────────────────

    @Test
    void updateCartItem_shouldUpdateQuantity() {
        CartItemResponse added = cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(2).build());

        CartItemResponse updated = cartService.updateCartItem(userId, added.getCartItemId(),
                UpdateCartItemRequest.builder().quantity(5).build());

        assertThat(updated.getQuantity()).isEqualTo(5);
    }

    @Test
    void updateCartItem_shouldFailWhenQuantityZero() {
        CartItemResponse added = cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(2).build());

        assertThatThrownBy(() -> cartService.updateCartItem(userId, added.getCartItemId(),
                UpdateCartItemRequest.builder().quantity(0).build()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void updateCartItem_shouldFailWhenCrossUser() {
        CartItemResponse added = cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(2).build());

        assertThatThrownBy(() -> cartService.updateCartItem(otherUserId, added.getCartItemId(),
                UpdateCartItemRequest.builder().quantity(3).build()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CART_FORBIDDEN);
    }

    // ── Delete Cart ─────────────────────────────────────────────────────

    @Test
    void deleteCartItem_shouldSucceed() {
        CartItemResponse added = cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(2).build());

        cartService.deleteCartItem(userId, added.getCartItemId());

        assertThat(cartService.getCartItems(userId)).isEmpty();
    }

    @Test
    void deleteCartItem_shouldFailWhenCrossUser() {
        CartItemResponse added = cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(2).build());

        assertThatThrownBy(() -> cartService.deleteCartItem(otherUserId, added.getCartItemId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CART_FORBIDDEN);
    }

    @Test
    void deleteAllCartItems_shouldClearAll() {
        cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(2).build());
        cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(secondItemId).quantity(1).build());

        cartService.deleteAllCartItems(userId);

        assertThat(cartService.getCartItems(userId)).isEmpty();
    }

    // ── Checkout Preview ────────────────────────────────────────────────

    @Test
    void checkoutPreview_shouldResolveGroupBuyId() {
        CartItemResponse added = cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(2).build());
        CartItemResponse added2 = cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(secondItemId).quantity(1).build());

        CartCheckoutPreviewResult result = cartService.loadCartItemsForOrder(
                userId, List.of(added.getCartItemId(), added2.getCartItemId()), null);

        assertThat(result.getGroupBuyId()).isEqualTo(groupBuyId);
        assertThat(result.getCartItems()).hasSize(2);
    }

    @Test
    void checkoutPreview_shouldFailWhenCrossGroupBuy() {
        // Create a second group buy
        CreateGroupBuyRequest gb2Request = new CreateGroupBuyRequest();
        gb2Request.setTitle("另一个团购");
        gb2Request.setDeliveryType(DeliveryType.EXPRESS);
        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inline = new CreateGroupBuyRequest.InlineProduct();
        inline.setName("商品C");
        inline.setBasePriceAmount(1000L);
        inline.setStock(100);
        item.setProduct(inline);
        item.setDisplayName("商品C");
        item.setGroupPriceAmount(990L);
        item.setGroupStock(100);
        item.setSortOrder(1);
        gb2Request.setItems(List.of(item));
        GroupBuyResponse gb2 = groupBuyService.createGroupBuy(leaderUserId, gb2Request);
        Long gb2ItemId = gb2.getItems().get(0).getId();

        CartItemResponse cart1 = cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(groupBuyItemId).quantity(2).build());
        CartItemResponse cart2 = cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(gb2ItemId).quantity(1).build());

        List<Long> crossGroupBuyIds = List.of(cart1.getCartItemId(), cart2.getCartItemId());

        assertThatThrownBy(() -> cartService.loadCartItemsForOrder(userId, crossGroupBuyIds, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CART_CROSS_GROUP_BUY);
    }

    // ── Hidden Group Buy ────────────────────────────────────────────────

    @Test
    void addCartItem_shouldFailWhenHiddenWithoutToken() {
        // Create a hidden group buy
        CreateGroupBuyRequest hiddenGbRequest = new CreateGroupBuyRequest();
        hiddenGbRequest.setTitle("隐藏团购");
        hiddenGbRequest.setDeliveryType(DeliveryType.EXPRESS);
        hiddenGbRequest.setVisibility("hidden");
        CreateGroupBuyRequest.ItemEntry hiddenItem = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inline = new CreateGroupBuyRequest.InlineProduct();
        inline.setName("隐藏商品");
        inline.setBasePriceAmount(1000L);
        inline.setStock(100);
        hiddenItem.setProduct(inline);
        hiddenItem.setDisplayName("隐藏商品");
        hiddenItem.setGroupPriceAmount(990L);
        hiddenItem.setGroupStock(100);
        hiddenItem.setSortOrder(1);
        hiddenGbRequest.setItems(List.of(hiddenItem));
        GroupBuyResponse hiddenGb = groupBuyService.createGroupBuy(leaderUserId, hiddenGbRequest);
        Long hiddenItemId = hiddenGb.getItems().get(0).getId();

        // Make it hidden in DB
        groupBuyMapper.update(null,
                new LambdaUpdateWrapper<GroupBuy>()
                        .eq(GroupBuy::getId, hiddenGb.getGroupBuy().getId())
                        .set(GroupBuy::getVisibility, "hidden"));

        assertThatThrownBy(() -> cartService.addCartItem(userId,
                AddCartItemRequest.builder().groupBuyItemId(hiddenItemId).quantity(1).build()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.HIDDEN_GROUP_BUY_REQUIRES_TOKEN);
    }

    @Test
    void addCartItem_shouldSucceedForHiddenWithValidToken() {
        // Create a hidden group buy
        CreateGroupBuyRequest hiddenGbRequest = new CreateGroupBuyRequest();
        hiddenGbRequest.setTitle("隐藏团购");
        hiddenGbRequest.setDeliveryType(DeliveryType.EXPRESS);
        hiddenGbRequest.setVisibility("hidden");
        CreateGroupBuyRequest.ItemEntry hiddenItem = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inline = new CreateGroupBuyRequest.InlineProduct();
        inline.setName("隐藏商品");
        inline.setBasePriceAmount(1000L);
        inline.setStock(100);
        hiddenItem.setProduct(inline);
        hiddenItem.setDisplayName("隐藏商品");
        hiddenItem.setGroupPriceAmount(990L);
        hiddenItem.setGroupStock(100);
        hiddenItem.setSortOrder(1);
        hiddenGbRequest.setItems(List.of(hiddenItem));
        GroupBuyResponse hiddenGb = groupBuyService.createGroupBuy(leaderUserId, hiddenGbRequest);
        Long hiddenGbId = hiddenGb.getGroupBuy().getId();
        Long hiddenItemId = hiddenGb.getItems().get(0).getId();

        // Make it hidden
        groupBuyMapper.update(null,
                new LambdaUpdateWrapper<GroupBuy>()
                        .eq(GroupBuy::getId, hiddenGbId)
                        .set(GroupBuy::getVisibility, "hidden"));

        // Create a share token
        GroupBuyShareToken token = new GroupBuyShareToken();
        token.setGroupBuyId(hiddenGbId);
        token.setToken("test-share-token-123");
        token.setStatus("active");
        shareTokenMapper.insert(token);

        CartItemResponse response = cartService.addCartItem(userId,
                AddCartItemRequest.builder()
                        .groupBuyItemId(hiddenItemId)
                        .quantity(2)
                        .shareToken("test-share-token-123")
                        .build());

        assertThat(response.getCartItemId()).isPositive();
        assertThat(response.getQuantity()).isEqualTo(2);
    }
}
