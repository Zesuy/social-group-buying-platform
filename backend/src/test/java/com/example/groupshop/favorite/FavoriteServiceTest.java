package com.example.groupshop.favorite;

import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.enums.DeliveryType;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.favorite.dto.FavoriteResponse;
import com.example.groupshop.favorite.service.FavoriteService;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.GroupBuyMapper;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link FavoriteService}.
 */
@Transactional
class FavoriteServiceTest extends ServiceTestBase {

    @Autowired
    private FavoriteService favoriteService;

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

    private Long leaderUserId;
    private Long regularUserId;
    private Long groupBuyId;

    @BeforeEach
    void setUp() {
        // Create leader user
        User leaderUser = new User();
        leaderUser.setNickname("测试团长");
        leaderUser.setPhone("13800009901");
        leaderUser.setStatus("normal");
        userMapper.insert(leaderUser);
        leaderUserId = leaderUser.getId();

        Leader leader = new Leader();
        leader.setUserId(leaderUserId);
        leader.setDisplayName("测试团长");
        leader.setServiceStatus("normal");
        leader.setMemberCount(0);
        leader.setFollowerCount(0);
        leaderMapper.insert(leader);

        Store store = new Store();
        store.setLeaderId(leader.getId());
        store.setName("测试店铺");
        store.setDefaultDeliveryType(DeliveryType.EXPRESS.getValue());
        store.setDistributionEnabled(false);
        store.setStatus("active");
        storeMapper.insert(store);

        // Create regular user
        User regularUser = new User();
        regularUser.setNickname("普通用户");
        regularUser.setPhone("13800009902");
        regularUser.setStatus("normal");
        userMapper.insert(regularUser);
        regularUserId = regularUser.getId();

        // Create a published public group buy
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("测试团购");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("测试商品");
        inlineProduct.setBasePriceAmount(1000L);
        inlineProduct.setStock(100);
        item.setProduct(inlineProduct);
        item.setDisplayName("测试商品");
        item.setGroupPriceAmount(1990L);
        item.setGroupStock(100);
        request.setItems(List.of(item));

        GroupBuyResponse response = groupBuyService.createGroupBuy(leaderUserId, request);
        groupBuyId = response.getGroupBuy().getId();
    }

    // ── Favorite ────────────────────────────────────────────────────────

    @Test
    void favorite_shouldSucceed() {
        FavoriteResponse response = favoriteService.favorite(regularUserId, groupBuyId);

        assertThat(response).isNotNull();
        assertThat(response.getGroupBuyId()).isEqualTo(groupBuyId);
        assertThat(response.getTitle()).isEqualTo("测试团购");
        assertThat(response.getFavoritedAt()).isNotNull();
    }

    @Test
    void favorite_shouldBeIdempotent() {
        FavoriteResponse first = favoriteService.favorite(regularUserId, groupBuyId);
        FavoriteResponse second = favoriteService.favorite(regularUserId, groupBuyId);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getGroupBuyId()).isEqualTo(groupBuyId);
    }

    @Test
    void favorite_shouldFailWhenGroupBuyNotFound() {
        assertThatThrownBy(() -> favoriteService.favorite(regularUserId, 99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("团购不存在或不可收藏");
    }

    @Test
    void favorite_shouldFailWhenGroupBuyNotPublished() {
        // End the group buy so it's no longer published
        groupBuyService.endGroupBuy(leaderUserId, groupBuyId);

        assertThatThrownBy(() -> favoriteService.favorite(regularUserId, groupBuyId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("团购不存在或不可收藏");
    }

    // ── Cancel Favorite ─────────────────────────────────────────────────

    @Test
    void cancelFavorite_shouldSucceed() {
        favoriteService.favorite(regularUserId, groupBuyId);

        // Should not throw
        favoriteService.cancelFavorite(regularUserId, groupBuyId);
    }

    @Test
    void cancelFavorite_shouldBeIdempotent() {
        // Cancel when no favorite exists — should not throw
        favoriteService.cancelFavorite(regularUserId, groupBuyId);

        // Cancel again — should not throw
        favoriteService.cancelFavorite(regularUserId, groupBuyId);
    }

    @Test
    void cancelFavorite_thenReFavorite_shouldReactivate() {
        FavoriteResponse first = favoriteService.favorite(regularUserId, groupBuyId);
        Long favoriteId = first.getId();

        favoriteService.cancelFavorite(regularUserId, groupBuyId);

        // Re-favorite should reactivate, not create new
        FavoriteResponse reFavored = favoriteService.favorite(regularUserId, groupBuyId);
        assertThat(reFavored.getId()).isEqualTo(favoriteId);
    }

    // ── List My Favorites ───────────────────────────────────────────────

    @Test
    void listMyFavorites_shouldReturnActiveFavoritesOnly() {
        favoriteService.favorite(regularUserId, groupBuyId);

        PageResponse<FavoriteResponse> result = favoriteService.listMyFavorites(regularUserId, 1, 20);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getGroupBuyId()).isEqualTo(groupBuyId);

        // Cancel and verify excluded
        favoriteService.cancelFavorite(regularUserId, groupBuyId);

        PageResponse<FavoriteResponse> afterCancel = favoriteService.listMyFavorites(regularUserId, 1, 20);
        assertThat(afterCancel.getItems()).isEmpty();
    }

    // ── Is Favorited ────────────────────────────────────────────────────

    @Test
    void isFavorited_shouldReturnTrueWhenActive() {
        favoriteService.favorite(regularUserId, groupBuyId);

        assertThat(favoriteService.isFavorited(regularUserId, groupBuyId)).isTrue();
    }

    @Test
    void isFavorited_shouldReturnFalseWhenNotFavorited() {
        assertThat(favoriteService.isFavorited(regularUserId, groupBuyId)).isFalse();
    }

    @Test
    void isFavorited_shouldReturnFalseWhenCanceled() {
        favoriteService.favorite(regularUserId, groupBuyId);
        favoriteService.cancelFavorite(regularUserId, groupBuyId);

        assertThat(favoriteService.isFavorited(regularUserId, groupBuyId)).isFalse();
    }

    @Test
    void isFavorited_shouldReturnFalseForNullUserId() {
        assertThat(favoriteService.isFavorited(null, groupBuyId)).isFalse();
    }
}
