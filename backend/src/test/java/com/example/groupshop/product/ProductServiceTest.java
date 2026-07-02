package com.example.groupshop.product;

import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.enums.DeliveryType;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Product;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.ProductMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import com.example.groupshop.model.mapper.GroupBuyMapper;
import com.example.groupshop.model.mapper.ProductCategoryMapper;
import com.example.groupshop.product.dto.CreateProductRequest;
import com.example.groupshop.product.dto.ProductResponse;
import com.example.groupshop.product.dto.ProductUsageResponse;
import com.example.groupshop.product.dto.UpdateProductRequest;
import java.util.List;
import com.example.groupshop.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ProductService}.
 */
@Transactional
class ProductServiceTest extends ServiceTestBase {

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
    private ProductCategoryMapper productCategoryMapper;

    @Autowired
    private GroupBuyService groupBuyService;

    @Autowired
    private GroupBuyMapper groupBuyMapper;

    @Autowired
    private GroupBuyItemMapper groupBuyItemMapper;

    private Long userId;
    private Long storeId;

    @BeforeEach
    void setUp() {
        // Create a test user with leader and store
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

        Store store = new Store();
        store.setLeaderId(leader.getId());
        store.setName("测试店铺");
        store.setDefaultDeliveryType(DeliveryType.EXPRESS.getValue());
        store.setDistributionEnabled(false);
        store.setStatus("active");
        storeMapper.insert(store);
        storeId = store.getId();
    }

    // ── Create ────────────────────────────────────────────────────────

    @Test
    void createProduct_shouldCreateProduct() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("白玉蜜桃");
        request.setDescription("山东蒙阴产地直发");
        request.setCoverImageUrl("https://example.com/product.png");
        request.setBasePriceAmount(2990L);
        request.setStock(100);
        request.setCategoryId(1L);

        ProductResponse response = productService.createProduct(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isPositive();
        assertThat(response.getStoreId()).isEqualTo(storeId);
        assertThat(response.getName()).isEqualTo("白玉蜜桃");
        assertThat(response.getDescription()).isEqualTo("山东蒙阴产地直发");
        assertThat(response.getCoverImageUrl()).isEqualTo("https://example.com/product.png");
        assertThat(response.getBasePriceAmount()).isEqualTo(2990L);
        assertThat(response.getStock()).isEqualTo(100);
        assertThat(response.getStatus()).isEqualTo("active");

        // Verify database state
        Product product = productMapper.selectById(response.getId());
        assertThat(product).isNotNull();
        assertThat(product.getStoreId()).isEqualTo(storeId);
        assertThat(product.getName()).isEqualTo("白玉蜜桃");
        assertThat(product.getStatus()).isEqualTo("active");
    }

    @Test
    void createProduct_shouldThrowWhenNotLeader() {
        // User without leader/store
        User regularUser = new User();
        regularUser.setNickname("普通用户");
        regularUser.setPhone("13800009902");
        regularUser.setStatus("normal");
        userMapper.insert(regularUser);

        CreateProductRequest request = new CreateProductRequest();
        request.setName("商品");
        request.setBasePriceAmount(1000L);
        request.setStock(10);
        request.setCategoryId(1L);

        assertThatThrownBy(() -> productService.createProduct(regularUser.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.LEADER_REQUIRED.getDefaultMessage());
    }

    // ── List ──────────────────────────────────────────────────────────

    @Test
    void getMyStoreProducts_shouldReturnPagedProducts() {
        // Create 3 products
        for (int i = 0; i < 3; i++) {
            CreateProductRequest request = new CreateProductRequest();
            request.setName("商品" + i);
            request.setBasePriceAmount(1000L);
            request.setStock(10);
            request.setCategoryId(1L);
            productService.createProduct(userId, request);
        }

        PageResponse<ProductResponse> page1 = productService.getMyStoreProducts(userId, null, null, null, 1, 2);
        assertThat(page1.getItems()).hasSize(2);
        assertThat(page1.getTotal()).isEqualTo(3);
        assertThat(page1.isHasMore()).isTrue();

        PageResponse<ProductResponse> page2 = productService.getMyStoreProducts(userId, null, null, null, 2, 2);
        assertThat(page2.getItems()).hasSize(1);
        assertThat(page2.isHasMore()).isFalse();
    }

    @Test
    void getMyStoreProducts_shouldExcludeDeletedProducts() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("正常商品");
        request.setBasePriceAmount(1000L);
        request.setStock(10);
        request.setCategoryId(1L);
        ProductResponse response = productService.createProduct(userId, request);

        // Delete one
        productService.deleteProduct(userId, response.getId());

        // List should be empty since the only product was deleted
        PageResponse<ProductResponse> result = productService.getMyStoreProducts(userId, null, null, null, 1, 20);
        assertThat(result.getItems()).isEmpty();
    }

    // ── Get ───────────────────────────────────────────────────────────

    @Test
    void getProduct_shouldReturnProduct() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("测试商品");
        request.setBasePriceAmount(2990L);
        request.setStock(50);
        request.setCategoryId(1L);
        ProductResponse created = productService.createProduct(userId, request);

        ProductResponse response = productService.getProduct(userId, created.getId());
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(created.getId());
        assertThat(response.getName()).isEqualTo("测试商品");
    }

    @Test
    void getProduct_shouldThrowWhenNotFound() {
        assertThatThrownBy(() -> productService.getProduct(userId, 99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage());
    }

    @Test
    void getProduct_shouldThrowWhenNotOwnStore() {
        // Create a product for current store
        CreateProductRequest request = new CreateProductRequest();
        request.setName("我的商品");
        request.setBasePriceAmount(1000L);
        request.setStock(10);
        request.setCategoryId(1L);
        ProductResponse myProduct = productService.createProduct(userId, request);

        // Another user with different store
        User otherUser = new User();
        otherUser.setNickname("其他团长");
        otherUser.setPhone("13800009910");
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

        assertThatThrownBy(() -> productService.getProduct(otherUser.getId(), myProduct.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.STORE_FORBIDDEN.getDefaultMessage());
    }

    // ── Update ────────────────────────────────────────────────────────

    @Test
    void updateProduct_shouldPartialUpdate() {
        CreateProductRequest createRequest = new CreateProductRequest();
        createRequest.setName("原始名称");
        createRequest.setDescription("原始简介");
        createRequest.setCoverImageUrl("https://example.com/old.png");
        createRequest.setBasePriceAmount(1000L);
        createRequest.setStock(10);
        createRequest.setCategoryId(1L);
        ProductResponse created = productService.createProduct(userId, createRequest);

        // Partial update: only name and price
        UpdateProductRequest updateRequest = new UpdateProductRequest();
        updateRequest.setName("新名称");
        updateRequest.setBasePriceAmount(2000L);

        ProductResponse updated = productService.updateProduct(userId, created.getId(), updateRequest);

        assertThat(updated.getName()).isEqualTo("新名称");
        assertThat(updated.getBasePriceAmount()).isEqualTo(2000L);
        assertThat(updated.getDescription()).isEqualTo("原始简介"); // unchanged
        assertThat(updated.getCoverImageUrl()).isEqualTo("https://example.com/old.png"); // unchanged
        assertThat(updated.getStock()).isEqualTo(10); // unchanged
        assertThat(updated.getStatus()).isEqualTo("active");
    }

    @Test
    void updateProduct_shouldThrowWhenNotFound() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("新名称");

        assertThatThrownBy(() -> productService.updateProduct(userId, 99999L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage());
    }

    // ── Delete ────────────────────────────────────────────────────────

    @Test
    void deleteProduct_shouldSoftDelete() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("待删除商品");
        request.setBasePriceAmount(1000L);
        request.setStock(10);
        request.setCategoryId(1L);
        ProductResponse created = productService.createProduct(userId, request);

        productService.deleteProduct(userId, created.getId());

        // Verify soft-deleted
        Product product = productMapper.selectById(created.getId());
        assertThat(product.getStatus()).isEqualTo("deleted");
    }

    @Test
    void deleteProduct_shouldThrowWhenNotFound() {
        assertThatThrownBy(() -> productService.deleteProduct(userId, 99999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage());
    }

    // ── Category ────────────────────────────────────────────────────────

    @Test
    void createProduct_shouldFailWhenCategoryIdInvalid() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("无效分类商品");
        request.setBasePriceAmount(1000L);
        request.setStock(10);
        request.setCategoryId(99999L);

        assertThatThrownBy(() -> productService.createProduct(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品分类不存在或已失效");
    }

    @Test
    void createProduct_shouldSucceedWithValidCategoryId() {
        Long validCategoryId = 1L; // First seeded category (生鲜水果)

        CreateProductRequest request = new CreateProductRequest();
        request.setName("分类商品");
        request.setBasePriceAmount(1000L);
        request.setStock(10);
        request.setCategoryId(validCategoryId);

        ProductResponse response = productService.createProduct(userId, request);

        assertThat(response.getCategoryId()).isEqualTo(validCategoryId);

        // Verify database state
        Product product = productMapper.selectById(response.getId());
        assertThat(product.getCategoryId()).isEqualTo(validCategoryId);
    }

    // ── List with filters ───────────────────────────────────────────────

    @Test
    void getMyStoreProducts_shouldFilterByKeyword() {
        CreateProductRequest request1 = new CreateProductRequest();
        request1.setName("白玉蜜桃");
        request1.setBasePriceAmount(1000L);
        request1.setStock(10);
        request1.setCategoryId(1L);
        productService.createProduct(userId, request1);

        CreateProductRequest request2 = new CreateProductRequest();
        request2.setName("红富士苹果");
        request2.setBasePriceAmount(1000L);
        request2.setStock(10);
        request2.setCategoryId(1L);
        productService.createProduct(userId, request2);

        // Search by keyword matching first product
        PageResponse<ProductResponse> result = productService.getMyStoreProducts(userId, "蜜桃", null, null, 1, 20);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getName()).isEqualTo("白玉蜜桃");

        // Search by keyword matching second product
        PageResponse<ProductResponse> result2 = productService.getMyStoreProducts(userId, "红富士", null, null, 1, 20);
        assertThat(result2.getItems()).hasSize(1);
        assertThat(result2.getItems().get(0).getName()).isEqualTo("红富士苹果");
    }

    @Test
    void getMyStoreProducts_shouldFilterByCategoryId() {
        Long categoryId = 1L;

        CreateProductRequest request1 = new CreateProductRequest();
        request1.setName("分类A商品");
        request1.setBasePriceAmount(1000L);
        request1.setStock(10);
        request1.setCategoryId(categoryId);
        productService.createProduct(userId, request1);

        CreateProductRequest request2 = new CreateProductRequest();
        request2.setName("无分类商品");
        request2.setBasePriceAmount(1000L);
        request2.setStock(10);
        request2.setCategoryId(2L);
        productService.createProduct(userId, request2);

        PageResponse<ProductResponse> result = productService.getMyStoreProducts(userId, null, categoryId, null, 1, 20);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getName()).isEqualTo("分类A商品");
    }

    @Test
    void getMyStoreProducts_shouldFilterByStatus() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("正常商品");
        request.setBasePriceAmount(1000L);
        request.setStock(10);
        request.setCategoryId(1L);
        ProductResponse created = productService.createProduct(userId, request);

        // List active products
        PageResponse<ProductResponse> active = productService.getMyStoreProducts(userId, null, null, "active", 1, 20);
        assertThat(active.getItems()).isNotEmpty();

        // Delete product so status becomes "deleted"
        productService.deleteProduct(userId, created.getId());

        // List active should exclude deleted
        PageResponse<ProductResponse> afterDelete = productService.getMyStoreProducts(userId, null, null, "active", 1, 20);
        assertThat(afterDelete.getItems()).extracting(ProductResponse::getId).doesNotContain(created.getId());
    }

    // ── Product Usages ──────────────────────────────────────────────────

    @Test
    void getProductUsages_shouldReturnUsages() {
        // Create a product first
        CreateProductRequest productRequest = new CreateProductRequest();
        productRequest.setName("团购用商品");
        productRequest.setBasePriceAmount(1000L);
        productRequest.setStock(100);
        productRequest.setCategoryId(1L);
        ProductResponse product = productService.createProduct(userId, productRequest);

        // Create a group buy that uses this product
        CreateGroupBuyRequest gbRequest = new CreateGroupBuyRequest();
        gbRequest.setTitle("团购测试");
        gbRequest.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        item.setProductId(product.getId());
        item.setDisplayName("团购商品");
        item.setGroupPriceAmount(1990L);
        item.setGroupStock(50);
        gbRequest.setItems(List.of(item));

        groupBuyService.createGroupBuy(userId, gbRequest);

        // Check usages
        PageResponse<ProductUsageResponse> usages = productService.getProductUsages(userId, product.getId(), 1, 20);
        assertThat(usages.getItems()).hasSize(1);
        assertThat(usages.getItems().get(0).getTitle()).isEqualTo("团购测试");
        assertThat(usages.getItems().get(0).getGroupBuyId()).isPositive();
    }

    @Test
    void getProductUsages_shouldRejectCrossStoreAccess() {
        // Create a product for current store
        CreateProductRequest productRequest = new CreateProductRequest();
        productRequest.setName("跨店商品");
        productRequest.setBasePriceAmount(1000L);
        productRequest.setStock(10);
        productRequest.setCategoryId(1L);
        ProductResponse product = productService.createProduct(userId, productRequest);

        // Create another user with different store
        User otherUser = new User();
        otherUser.setNickname("其他团长");
        otherUser.setPhone("13800009920");
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

        assertThatThrownBy(() -> productService.getProductUsages(otherUser.getId(), product.getId(), 1, 20))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.STORE_FORBIDDEN.getDefaultMessage());
    }
}
