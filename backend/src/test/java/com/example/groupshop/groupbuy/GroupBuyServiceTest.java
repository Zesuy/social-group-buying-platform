package com.example.groupshop.groupbuy;

import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.enums.DeliveryType;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse;
import com.example.groupshop.groupbuy.dto.UpdateGroupBuyRequest;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.OrderItem;
import com.example.groupshop.model.entity.Product;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import com.example.groupshop.model.mapper.GroupBuyMapper;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.OrderItemMapper;
import com.example.groupshop.model.mapper.ProductMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import com.example.groupshop.favorite.service.FavoriteService;
import com.example.groupshop.product.dto.CreateProductRequest;
import com.example.groupshop.product.service.ProductService;
import com.example.groupshop.publicbrowsing.dto.GroupBuyDetailResponse;
import com.example.groupshop.publicbrowsing.dto.PublicGroupBuyItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link GroupBuyService}.
 */
@Transactional
class GroupBuyServiceTest extends ServiceTestBase {

    @Autowired
    private GroupBuyService groupBuyService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaderMapper leaderMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private GroupBuyMapper groupBuyMapper;

    @Autowired
    private GroupBuyItemMapper groupBuyItemMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private FavoriteService favoriteService;

    private Long userId;
    private Long storeId;
    private Long leaderId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setNickname("测试团长");
        user.setPhone("13800009901");
        user.setStatus("normal");
        userMapper.insert(user);
        userId = user.getId();

        Leader leader = new Leader();
        leader.setUserId(userId);
        leader.setDisplayName("测试团长");
        leader.setServiceStatus("normal");
        leader.setMemberCount(0);
        leader.setFollowerCount(0);
        leaderMapper.insert(leader);
        leaderId = leader.getId();

        Store store = new Store();
        store.setLeaderId(leader.getId());
        store.setName("测试店铺");
        store.setDefaultDeliveryType(DeliveryType.EXPRESS.getValue());
        store.setDistributionEnabled(false);
        store.setStatus("active");
        storeMapper.insert(store);
        storeId = store.getId();
    }

    // ── Create with inline product ────────────────────────────────────

    @Test
    void createGroupBuy_shouldCreateWithInlineProduct() {
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("山东蜜桃团购");
        request.setIntroduction("产地直发，香甜多汁");
        request.setCoverImageUrl("https://example.com/cover.png");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("白玉蜜桃");
        inlineProduct.setDescription("山东蒙阴产地直发");
        inlineProduct.setCoverImageUrl("https://example.com/product.png");
        inlineProduct.setBasePriceAmount(2990L);
        inlineProduct.setStock(100);
        item.setProduct(inlineProduct);
        item.setDisplayName("白玉蜜桃 5 斤装");
        item.setGroupPriceAmount(2990L);
        item.setGroupStock(100);
        item.setSortOrder(1);
        request.setItems(List.of(item));

        GroupBuyResponse response = groupBuyService.createGroupBuy(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getGroupBuy()).isNotNull();
        assertThat(response.getGroupBuy().getId()).isPositive();
        assertThat(response.getGroupBuy().getStoreId()).isEqualTo(storeId);
        assertThat(response.getGroupBuy().getLeaderId()).isEqualTo(leaderId);
        assertThat(response.getGroupBuy().getTitle()).isEqualTo("山东蜜桃团购");
        assertThat(response.getGroupBuy().getGroupType()).isEqualTo("normal");
        assertThat(response.getGroupBuy().getStatus()).isEqualTo("published");
        assertThat(response.getGroupBuy().getVisibility()).isEqualTo("public");
        assertThat(response.getGroupBuy().getDeliveryType()).isEqualTo("express");

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getDisplayName()).isEqualTo("白玉蜜桃 5 斤装");
        assertThat(response.getItems().get(0).getGroupPriceAmount()).isEqualTo(2990L);
        assertThat(response.getItems().get(0).getGroupStock()).isEqualTo(100);
        assertThat(response.getItems().get(0).getSoldCount()).isZero();
        assertThat(response.getItems().get(0).getSortOrder()).isEqualTo(1);

        // Verify the inline product was created
        Product product = productMapper.selectById(response.getItems().get(0).getProductId());
        assertThat(product).isNotNull();
        assertThat(product.getStoreId()).isEqualTo(storeId);
        assertThat(product.getName()).isEqualTo("白玉蜜桃");
    }

    // ── Create with existing product ──────────────────────────────────

    @Test
    void createGroupBuy_shouldCreateWithExistingProduct() {
        // Create a product first
        CreateProductRequest productReq = new CreateProductRequest();
        productReq.setName("蜜桃");
        productReq.setBasePriceAmount(2990L);
        productReq.setStock(100);
        productReq.setCategoryId(1L);
        var productResponse = productService.createProduct(userId, productReq);

        // Create group buy reusing that product
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("蜜桃团购");
        request.setDeliveryType(DeliveryType.PICKUP);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        item.setProductId(productResponse.getId());
        item.setDisplayName("蜜桃 5 斤装");
        item.setGroupPriceAmount(2990L);
        item.setGroupStock(100);
        request.setItems(List.of(item));

        GroupBuyResponse response = groupBuyService.createGroupBuy(userId, request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(productResponse.getId());
        assertThat(response.getItems().get(0).getDisplayName()).isEqualTo("蜜桃 5 斤装");
    }

    // ── Create validation ─────────────────────────────────────────────

    @Test
    void createGroupBuy_shouldThrowWhenProductNotOwnStore() {
        // Another user's product
        User otherUser = new User();
        otherUser.setNickname("其他店铺");
        otherUser.setPhone("13800009902");
        otherUser.setStatus("normal");
        userMapper.insert(otherUser);

        Leader otherLeader = new Leader();
        otherLeader.setUserId(otherUser.getId());
        otherLeader.setDisplayName("其他团长");
        otherLeader.setServiceStatus("normal");
        otherLeader.setMemberCount(0);
        otherLeader.setFollowerCount(0);
        leaderMapper.insert(otherLeader);

        Store otherStore = new Store();
        otherStore.setLeaderId(otherLeader.getId());
        otherStore.setName("其他店铺");
        otherStore.setDefaultDeliveryType(DeliveryType.EXPRESS.getValue());
        otherStore.setDistributionEnabled(false);
        otherStore.setStatus("active");
        storeMapper.insert(otherStore);

        Product otherProduct = new Product();
        otherProduct.setStoreId(otherStore.getId());
        otherProduct.setName("别人的商品");
        otherProduct.setBasePriceAmount(1000L);
        otherProduct.setStock(10);
        otherProduct.setStatus("active");
        productMapper.insert(otherProduct);

        // Try to use it in group buy
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("跨店团购");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        item.setProductId(otherProduct.getId());
        item.setDisplayName("别人的商品");
        item.setGroupPriceAmount(1000L);
        item.setGroupStock(10);
        request.setItems(List.of(item));

        assertThatThrownBy(() -> groupBuyService.createGroupBuy(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不属于当前店铺");
    }

    @Test
    void createGroupBuy_shouldThrowWhenProductNotFound() {
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("测试团购");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        item.setProductId(99999L);
        item.setDisplayName("测试商品");
        item.setGroupPriceAmount(1000L);
        item.setGroupStock(10);
        request.setItems(List.of(item));

        assertThatThrownBy(() -> groupBuyService.createGroupBuy(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品不存在");
    }

    @Test
    void createGroupBuy_shouldThrowWhenEmptyItems() {
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("测试团购");
        request.setDeliveryType(DeliveryType.EXPRESS);
        request.setItems(List.of());

        assertThatThrownBy(() -> groupBuyService.createGroupBuy(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("团购至少包含一个商品");
    }

    @Test
    void createGroupBuy_shouldThrowWhenGroupStockNegative() {
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("测试团购");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("商品");
        inlineProduct.setBasePriceAmount(1000L);
        inlineProduct.setStock(10);
        item.setProduct(inlineProduct);
        item.setDisplayName("测试商品");
        item.setGroupPriceAmount(1000L);
        item.setGroupStock(-1);
        request.setItems(List.of(item));

        assertThatThrownBy(() -> groupBuyService.createGroupBuy(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("团购库存不能为负数");
    }

    // ── List ──────────────────────────────────────────────────────────

    @Test
    void getMyStoreGroupBuys_shouldReturnPaged() {
        // Create 2 group buys
        for (int i = 0; i < 2; i++) {
            CreateGroupBuyRequest request = new CreateGroupBuyRequest();
            request.setTitle("团购" + i);
            request.setDeliveryType(DeliveryType.EXPRESS);

            CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
            CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
            inlineProduct.setName("商品" + i);
            inlineProduct.setBasePriceAmount(1000L);
            inlineProduct.setStock(10);
            item.setProduct(inlineProduct);
            item.setDisplayName("商品" + i);
            item.setGroupPriceAmount(1000L);
            item.setGroupStock(10);
            request.setItems(List.of(item));

            groupBuyService.createGroupBuy(userId, request);
        }

        PageResponse<GroupBuyResponse.GroupBuyData> result = groupBuyService.getMyStoreGroupBuys(userId, null, 1, 20);
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotal()).isEqualTo(2);
    }

    // ── Detail ────────────────────────────────────────────────────────

    @Test
    void getGroupBuy_shouldReturnDetail() {
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("详情团购");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("商品");
        inlineProduct.setBasePriceAmount(1000L);
        inlineProduct.setStock(10);
        item.setProduct(inlineProduct);
        item.setDisplayName("测试商品");
        item.setGroupPriceAmount(1000L);
        item.setGroupStock(10);
        request.setItems(List.of(item));

        GroupBuyResponse created = groupBuyService.createGroupBuy(userId, request);
        GroupBuyResponse detail = groupBuyService.getGroupBuy(userId, created.getGroupBuy().getId());

        assertThat(detail.getGroupBuy().getId()).isEqualTo(created.getGroupBuy().getId());
        assertThat(detail.getItems()).hasSize(1);
    }

    @Test
    void getGroupBuy_shouldThrowWhenNotOwnStore() {
        // Create a group buy
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("我的团购");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("商品");
        inlineProduct.setBasePriceAmount(1000L);
        inlineProduct.setStock(10);
        item.setProduct(inlineProduct);
        item.setDisplayName("商品");
        item.setGroupPriceAmount(1000L);
        item.setGroupStock(10);
        request.setItems(List.of(item));

        GroupBuyResponse created = groupBuyService.createGroupBuy(userId, request);

        // Another user
        User otherUser = new User();
        otherUser.setNickname("其他人");
        otherUser.setPhone("13800009999");
        otherUser.setStatus("normal");
        userMapper.insert(otherUser);

        assertThatThrownBy(() -> groupBuyService.getGroupBuy(otherUser.getId(), created.getGroupBuy().getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.LEADER_REQUIRED.getDefaultMessage());
    }

    // ── Update ────────────────────────────────────────────────────────

    @Test
    void updateGroupBuy_shouldUpdateFields() {
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("原始标题");
        request.setIntroduction("原始介绍");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("商品");
        inlineProduct.setBasePriceAmount(1000L);
        inlineProduct.setStock(10);
        item.setProduct(inlineProduct);
        item.setDisplayName("商品");
        item.setGroupPriceAmount(1000L);
        item.setGroupStock(10);
        request.setItems(List.of(item));

        GroupBuyResponse created = groupBuyService.createGroupBuy(userId, request);

        // Update
        UpdateGroupBuyRequest updateRequest = new UpdateGroupBuyRequest();
        updateRequest.setTitle("新标题");
        updateRequest.setIntroduction("新介绍");

        GroupBuyResponse updated = groupBuyService.updateGroupBuy(userId, created.getGroupBuy().getId(), updateRequest);

        assertThat(updated.getGroupBuy().getTitle()).isEqualTo("新标题");
        assertThat(updated.getGroupBuy().getIntroduction()).isEqualTo("新介绍");
    }

    @Test
    void updateGroupBuy_shouldNotUpdatePriceWhenOrderExists() {
        // Create group buy
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("团购");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("商品");
        inlineProduct.setBasePriceAmount(1000L);
        inlineProduct.setStock(10);
        item.setProduct(inlineProduct);
        item.setDisplayName("商品");
        item.setGroupPriceAmount(1000L);
        item.setGroupStock(10);
        request.setItems(List.of(item));

        GroupBuyResponse created = groupBuyService.createGroupBuy(userId, request);
        Long itemId = created.getItems().get(0).getId();

        // Create an order referencing this item (minimal order to trigger protection)
        long orderId = created.getGroupBuy().getId() * 10000 + 1;
        String ts = "2026-06-26 12:00:00";
        jdbcTemplate.update("INSERT INTO orders (id, order_no, user_id, leader_id, store_id, group_buy_id, address_id, " +
                        "receiver_name, receiver_phone, province, city, district, detail, full_address, " +
                        "total_amount, discount_amount, pay_amount, pay_status, order_status, created_at, updated_at) " +
                        "VALUES (?, ?, 1, 1, 1, ?, 1, '张三', '13800000000', '浙江省', '杭州市', '西湖区', " +
                        "'某某路', '浙江省杭州市西湖区某某路', 1000, 0, 1000, 'unpaid', 'pending_pay', ?, ?)",
                orderId, "TEST_ORDER", created.getGroupBuy().getId(), ts, ts);

        jdbcTemplate.update("INSERT INTO order_items (id, order_id, product_id, group_buy_item_id, product_name, " +
                        "sku_name, unit_price_amount, quantity, total_amount, created_at) " +
                        "VALUES (?, ?, 1, ?, '测试商品', '', 1000, 1, 1000, ?)",
                orderId + 1, orderId, itemId, ts);

        // Try to update price
        UpdateGroupBuyRequest updateRequest = new UpdateGroupBuyRequest();
        UpdateGroupBuyRequest.UpdateItemEntry itemUpdate = new UpdateGroupBuyRequest.UpdateItemEntry();
        itemUpdate.setId(itemId);
        itemUpdate.setGroupPriceAmount(2000L);
        updateRequest.setItems(List.of(itemUpdate));

        assertThatThrownBy(() -> groupBuyService.updateGroupBuy(userId, created.getGroupBuy().getId(), updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许修改价格");
    }

    // ── End ───────────────────────────────────────────────────────────

    @Test
    void endGroupBuy_shouldEndPublished() {
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("可结束团购");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("商品");
        inlineProduct.setBasePriceAmount(1000L);
        inlineProduct.setStock(10);
        item.setProduct(inlineProduct);
        item.setDisplayName("商品");
        item.setGroupPriceAmount(1000L);
        item.setGroupStock(10);
        request.setItems(List.of(item));

        GroupBuyResponse created = groupBuyService.createGroupBuy(userId, request);

        GroupBuyResponse ended = groupBuyService.endGroupBuy(userId, created.getGroupBuy().getId());
        assertThat(ended.getGroupBuy().getStatus()).isEqualTo("ended");

        // Verify database
        GroupBuy gb = groupBuyMapper.selectById(created.getGroupBuy().getId());
        assertThat(gb.getStatus()).isEqualTo("ended");
    }

    @Test
    void endGroupBuy_shouldThrowWhenAlreadyEnded() {
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("已结束团购");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("商品");
        inlineProduct.setBasePriceAmount(1000L);
        inlineProduct.setStock(10);
        item.setProduct(inlineProduct);
        item.setDisplayName("商品");
        item.setGroupPriceAmount(1000L);
        item.setGroupStock(10);
        request.setItems(List.of(item));

        GroupBuyResponse created = groupBuyService.createGroupBuy(userId, request);
        groupBuyService.endGroupBuy(userId, created.getGroupBuy().getId());

        // Second end should fail
        assertThatThrownBy(() -> groupBuyService.endGroupBuy(userId, created.getGroupBuy().getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有发布状态的团购可以结束");
    }

    // ── Public browsing ─────────────────────────────────────────────────

    @Test
    void getPublicGroupBuys_shouldFilterByKeyword() {
        // Create a group buy with distinctive title
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("山东蜜桃团购");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("蜜桃");
        inlineProduct.setBasePriceAmount(1000L);
        inlineProduct.setStock(10);
        item.setProduct(inlineProduct);
        item.setDisplayName("蜜桃 5 斤装");
        item.setGroupPriceAmount(2990L);
        item.setGroupStock(10);
        request.setItems(List.of(item));

        groupBuyService.createGroupBuy(userId, request);

        // Keyword search should find by title
        PageResponse<PublicGroupBuyItem> result = groupBuyService.getPublicGroupBuys(1, 20, "蜜桃", null);
        assertThat(result.getItems()).isNotEmpty();
        assertThat(result.getItems().get(0).getTitle()).contains("蜜桃");
    }

    @Test
    void getPublicGroupBuys_shouldFilterByCategoryId() {
        Long categoryId = 1L;

        // Create a product with category
        Product catProduct = new Product();
        catProduct.setStoreId(storeId);
        catProduct.setName("分类商品");
        catProduct.setBasePriceAmount(1000L);
        catProduct.setStock(10);
        catProduct.setCategoryId(categoryId);
        catProduct.setStatus("active");
        productMapper.insert(catProduct);

        // Create group buy using this categorized product
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("分类团购");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        item.setProductId(catProduct.getId());
        item.setDisplayName("分类商品");
        item.setGroupPriceAmount(1990L);
        item.setGroupStock(10);
        request.setItems(List.of(item));

        groupBuyService.createGroupBuy(userId, request);

        // Filter by categoryId
        PageResponse<PublicGroupBuyItem> result = groupBuyService.getPublicGroupBuys(1, 20, null, categoryId);
        assertThat(result.getItems()).isNotEmpty();
        assertThat(result.getItems()).extracting(PublicGroupBuyItem::getTitle)
                .contains("分类团购");
    }

    @Test
    void getPublicGroupBuyDetail_shouldReturnFavoritedTrueWhenFavorited() {
        // Create a group buy
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("收藏详情测试");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("商品");
        inlineProduct.setBasePriceAmount(1000L);
        inlineProduct.setStock(10);
        item.setProduct(inlineProduct);
        item.setDisplayName("商品");
        item.setGroupPriceAmount(1000L);
        item.setGroupStock(10);
        request.setItems(List.of(item));

        GroupBuyResponse created = groupBuyService.createGroupBuy(userId, request);
        Long gbId = created.getGroupBuy().getId();

        // Create a viewer user
        User viewer = new User();
        viewer.setNickname("浏览者");
        viewer.setPhone("13800009999");
        viewer.setStatus("normal");
        userMapper.insert(viewer);

        // Favorite the group buy
        favoriteService.favorite(viewer.getId(), gbId);

        // Get detail as the viewer — should see favorited=true
        GroupBuyDetailResponse detail = groupBuyService.getPublicGroupBuyDetail(gbId, viewer.getId());
        assertThat(detail).isNotNull();
        assertThat(detail.getViewer().isFavorited()).isTrue();
    }

    @Test
    void getPublicGroupBuyDetail_shouldReturnFavoritedFalseWhenNotFavorited() {
        // Create a group buy
        CreateGroupBuyRequest request = new CreateGroupBuyRequest();
        request.setTitle("未收藏详情测试");
        request.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("商品");
        inlineProduct.setBasePriceAmount(1000L);
        inlineProduct.setStock(10);
        item.setProduct(inlineProduct);
        item.setDisplayName("商品");
        item.setGroupPriceAmount(1000L);
        item.setGroupStock(10);
        request.setItems(List.of(item));

        GroupBuyResponse created = groupBuyService.createGroupBuy(userId, request);
        Long gbId = created.getGroupBuy().getId();

        // Create a viewer user (no favorite)
        User viewer = new User();
        viewer.setNickname("浏览者2");
        viewer.setPhone("13800009998");
        viewer.setStatus("normal");
        userMapper.insert(viewer);

        // Get detail — should see favorited=false
        GroupBuyDetailResponse detail = groupBuyService.getPublicGroupBuyDetail(gbId, viewer.getId());
        assertThat(detail).isNotNull();
        assertThat(detail.getViewer().isFavorited()).isFalse();
    }
}
