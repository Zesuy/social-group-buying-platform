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
import com.example.groupshop.product.dto.CreateProductRequest;
import com.example.groupshop.product.dto.ProductResponse;
import com.example.groupshop.product.dto.UpdateProductRequest;
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
            productService.createProduct(userId, request);
        }

        PageResponse<ProductResponse> page1 = productService.getMyStoreProducts(userId, 1, 2);
        assertThat(page1.getItems()).hasSize(2);
        assertThat(page1.getTotal()).isEqualTo(3);
        assertThat(page1.isHasMore()).isTrue();

        PageResponse<ProductResponse> page2 = productService.getMyStoreProducts(userId, 2, 2);
        assertThat(page2.getItems()).hasSize(1);
        assertThat(page2.isHasMore()).isFalse();
    }

    @Test
    void getMyStoreProducts_shouldExcludeDeletedProducts() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("正常商品");
        request.setBasePriceAmount(1000L);
        request.setStock(10);
        ProductResponse response = productService.createProduct(userId, request);

        // Delete one
        productService.deleteProduct(userId, response.getId());

        // List should be empty since the only product was deleted
        PageResponse<ProductResponse> result = productService.getMyStoreProducts(userId, 1, 20);
        assertThat(result.getItems()).isEmpty();
    }

    // ── Get ───────────────────────────────────────────────────────────

    @Test
    void getProduct_shouldReturnProduct() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("测试商品");
        request.setBasePriceAmount(2990L);
        request.setStock(50);
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
}
