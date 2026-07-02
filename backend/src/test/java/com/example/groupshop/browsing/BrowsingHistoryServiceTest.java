package com.example.groupshop.browsing;

import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.browsing.dto.BrowsingHistoryResponse;
import com.example.groupshop.browsing.service.BrowsingHistoryService;
import com.example.groupshop.common.enums.DeliveryType;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.BrowsingHistoryMapper;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link BrowsingHistoryService}.
 */
@Transactional
class BrowsingHistoryServiceTest extends ServiceTestBase {

    @Autowired
    private BrowsingHistoryService browsingHistoryService;

    @Autowired
    private GroupBuyService groupBuyService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaderMapper leaderMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private BrowsingHistoryMapper browsingHistoryMapper;

    private Long leaderUserId;
    private Long regularUserId;
    private Long otherUserId;
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

        // Create another user
        User otherUser = new User();
        otherUser.setNickname("其他用户");
        otherUser.setPhone("13800009903");
        otherUser.setStatus("normal");
        userMapper.insert(otherUser);
        otherUserId = otherUser.getId();

        // Create a published public group buy
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("浏览历史测试团购");
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

        var response = groupBuyService.createGroupBuy(leaderUserId, request);
        groupBuyId = response.getGroupBuy().getId();
    }

    // ── Record View ─────────────────────────────────────────────────────

    @Test
    void recordView_shouldCreateNewRecord() {
        browsingHistoryService.recordView(regularUserId, groupBuyId);

        PageResponse<BrowsingHistoryResponse> result = browsingHistoryService.listMyHistories(regularUserId, 1, 20);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getGroupBuyId()).isEqualTo(groupBuyId);
        assertThat(result.getItems().get(0).getViewedAt()).isNotNull();
    }

    @Test
    void recordView_shouldUpdateViewedAtOnRepeat() throws InterruptedException {
        browsingHistoryService.recordView(regularUserId, groupBuyId);

        // Read first viewedAt
        PageResponse<BrowsingHistoryResponse> first = browsingHistoryService.listMyHistories(regularUserId, 1, 20);
        String firstViewedAt = first.getItems().get(0).getViewedAt();

        // Small delay to ensure different timestamp
        Thread.sleep(10);

        // Record view again
        browsingHistoryService.recordView(regularUserId, groupBuyId);

        PageResponse<BrowsingHistoryResponse> second = browsingHistoryService.listMyHistories(regularUserId, 1, 20);
        assertThat(second.getItems()).hasSize(1);
        // viewedAt should have been updated (not the same)
        assertThat(second.getItems().get(0).getViewedAt()).isNotEqualTo(firstViewedAt);
    }

    @Test
    void recordView_shouldNotThrowWhenGroupBuyDoesNotExist() {
        // Should not throw — method logs and swallows exceptions
        assertThatCode(() -> browsingHistoryService.recordView(regularUserId, 99999L))
                .doesNotThrowAnyException();
    }

    // ── List My Histories ───────────────────────────────────────────────

    @Test
    void listMyHistories_shouldReturnRecordsOrderedByViewedAtDesc() throws InterruptedException {
        browsingHistoryService.recordView(regularUserId, groupBuyId);

        // Create another group buy and view it
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("第二个团购");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("商品2");
        inlineProduct.setBasePriceAmount(1000L);
        inlineProduct.setStock(10);
        item.setProduct(inlineProduct);
        item.setDisplayName("商品2");
        item.setGroupPriceAmount(1000L);
        item.setGroupStock(10);
        request.setItems(List.of(item));

        var response = groupBuyService.createGroupBuy(leaderUserId, request);
        Long secondGroupBuyId = response.getGroupBuy().getId();

        Thread.sleep(10);

        browsingHistoryService.recordView(regularUserId, secondGroupBuyId);

        PageResponse<BrowsingHistoryResponse> result = browsingHistoryService.listMyHistories(regularUserId, 1, 20);
        assertThat(result.getItems()).hasSize(2);
        // Most recent first
        assertThat(result.getItems().get(0).getGroupBuyId()).isEqualTo(secondGroupBuyId);
        assertThat(result.getItems().get(1).getGroupBuyId()).isEqualTo(groupBuyId);
    }

    // ── Delete History ──────────────────────────────────────────────────

    @Test
    void deleteHistory_shouldSucceedForOwnRecord() {
        browsingHistoryService.recordView(regularUserId, groupBuyId);

        PageResponse<BrowsingHistoryResponse> result = browsingHistoryService.listMyHistories(regularUserId, 1, 20);
        Long historyId = result.getItems().get(0).getId();

        browsingHistoryService.deleteHistory(regularUserId, historyId);

        // Verify deleted
        PageResponse<BrowsingHistoryResponse> afterDelete = browsingHistoryService.listMyHistories(regularUserId, 1, 20);
        assertThat(afterDelete.getItems()).isEmpty();
    }

    @Test
    void deleteHistory_shouldThrowForbiddenForOtherUserRecord() {
        browsingHistoryService.recordView(regularUserId, groupBuyId);

        PageResponse<BrowsingHistoryResponse> result = browsingHistoryService.listMyHistories(regularUserId, 1, 20);
        Long historyId = result.getItems().get(0).getId();

        assertThatThrownBy(() -> browsingHistoryService.deleteHistory(otherUserId, historyId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能删除他人的浏览记录");
    }

    @Test
    void deleteHistory_shouldThrowResourceNotFoundForNonExistent() {
        assertThatThrownBy(() -> browsingHistoryService.deleteHistory(regularUserId, 99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage());
    }
}
