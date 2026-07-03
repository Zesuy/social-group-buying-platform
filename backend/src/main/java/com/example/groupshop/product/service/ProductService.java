package com.example.groupshop.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.groupshop.category.service.CategoryService;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.common.util.ContentValidationUtil;
import com.example.groupshop.common.util.CurrentStoreHelper;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.entity.Product;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import com.example.groupshop.model.mapper.GroupBuyMapper;
import com.example.groupshop.model.mapper.ProductMapper;
import com.example.groupshop.product.dto.CreateProductRequest;
import com.example.groupshop.product.dto.ProductResponse;
import com.example.groupshop.product.dto.ProductUsageResponse;
import com.example.groupshop.product.dto.UpdateProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing products under the current user's store.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final GroupBuyItemMapper groupBuyItemMapper;
    private final GroupBuyMapper groupBuyMapper;
    private final CurrentStoreHelper currentStoreHelper;
    private final CategoryService categoryService;
    private final ContentValidationUtil contentValidationUtil;

    /**
     * Create a product for the current user's store.
     */
    @Transactional
    public ProductResponse createProduct(Long userId, CreateProductRequest request) {
        var leaderAndStore = currentStoreHelper.getLeaderAndStore(userId);
        Store store = leaderAndStore.getStore();

        // Validate categoryId is active
        if (!categoryService.isActiveCategory(request.getCategoryId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "商品分类不存在或已失效");
        }

        Product product = new Product();
        product.setStoreId(store.getId());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCoverImageUrl(request.getCoverImageUrl());
        contentValidationUtil.validateImageUrls(request.getDetailImageUrls(), 9, "detailImageUrls");
        product.setDetailImageUrls(contentValidationUtil.serializeImageUrls(request.getDetailImageUrls()));
        product.setBasePriceAmount(request.getBasePriceAmount());
        product.setStock(request.getStock());
        product.setCategoryId(request.getCategoryId());
        product.setStatus("active");
        productMapper.insert(product);

        return toProductResponse(product);
    }

    /**
     * List products for the current user's store with optional filtering.
     */
    public PageResponse<ProductResponse> getMyStoreProducts(Long userId, String keyword, Long categoryId,
                                                              String status, int page, int pageSize) {
        var leaderAndStore = currentStoreHelper.getLeaderAndStore(userId);
        Store store = leaderAndStore.getStore();

        Page<Product> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStoreId, store.getId())
                .orderByDesc(Product::getCreatedAt);

        // When no explicit status filter, exclude soft-deleted by default
        if (status == null || status.isEmpty()) {
            wrapper.ne(Product::getStatus, "deleted");
        } else {
            wrapper.eq(Product::getStatus, status);
        }

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Product::getName, keyword)
                    .or().like(Product::getDescription, keyword));
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }

        Page<Product> result = productMapper.selectPage(pageObj, wrapper);
        List<ProductResponse> items = result.getRecords().stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());

        return PageResponse.of(items, page, pageSize, result.getTotal());
    }

    /**
     * Get a product by id, verifying it belongs to the current user's store.
     */
    public ProductResponse getProduct(Long userId, Long productId) {
        Product product = findProductForStore(userId, productId);
        return toProductResponse(product);
    }

    /**
     * Partial-update a product. Only non-null fields are updated.
     */
    @Transactional
    public ProductResponse updateProduct(Long userId, Long productId, UpdateProductRequest request) {
        Product product = findProductForStore(userId, productId);

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getCoverImageUrl() != null) {
            product.setCoverImageUrl(request.getCoverImageUrl());
        }
        if (request.getBasePriceAmount() != null) {
            product.setBasePriceAmount(request.getBasePriceAmount());
        }
        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }
        if (request.getCategoryId() != null) {
            if (!categoryService.isActiveCategory(request.getCategoryId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "商品分类不存在或已失效");
            }
            product.setCategoryId(request.getCategoryId());
        }
        if (request.getDetailImageUrls() != null) {
            contentValidationUtil.validateImageUrls(request.getDetailImageUrls(), 9, "detailImageUrls");
            product.setDetailImageUrls(contentValidationUtil.serializeImageUrls(request.getDetailImageUrls()));
        }

        productMapper.updateById(product);
        return toProductResponse(product);
    }

    /**
     * Soft-delete a product by setting status to "deleted".
     */
    @Transactional
    public void deleteProduct(Long userId, Long productId) {
        Product product = findProductForStore(userId, productId);
        product.setStatus("deleted");
        productMapper.updateById(product);
    }

    /**
     * List group buy usages for a product in the current store.
     */
    public PageResponse<ProductUsageResponse> getProductUsages(Long userId, Long productId, int page, int pageSize) {
        var leaderAndStore = currentStoreHelper.getLeaderAndStore(userId);
        Store store = leaderAndStore.getStore();

        // Verify product exists and belongs to current store
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!product.getStoreId().equals(store.getId())) {
            throw new BusinessException(ErrorCode.STORE_FORBIDDEN);
        }

        // Find group buy items referencing this product
        Page<GroupBuyItem> itemPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<GroupBuyItem> itemWrapper = new LambdaQueryWrapper<GroupBuyItem>()
                .eq(GroupBuyItem::getProductId, productId)
                .orderByDesc(GroupBuyItem::getCreatedAt);

        Page<GroupBuyItem> itemResult = groupBuyItemMapper.selectPage(itemPage, itemWrapper);

        List<ProductUsageResponse> usages = itemResult.getRecords().stream()
                .map(item -> {
                    GroupBuy gb = groupBuyMapper.selectById(item.getGroupBuyId());
                    if (gb == null) return null;
                    return ProductUsageResponse.builder()
                            .groupBuyId(gb.getId())
                            .title(gb.getTitle())
                            .status(gb.getStatus())
                            .itemId(item.getId())
                            .displayName(item.getDisplayName())
                            .groupPriceAmount(item.getGroupPriceAmount())
                            .groupStock(item.getGroupStock())
                            .soldCount(item.getSoldCount())
                            .startAt(gb.getStartTime() != null ? gb.getStartTime().toString() : null)
                            .endAt(gb.getEndTime() != null ? gb.getEndTime().toString() : null)
                            .createdAt(item.getCreatedAt() != null ? item.getCreatedAt().toString() : null)
                            .build();
                })
                .filter(u -> u != null)
                .collect(Collectors.toList());

        return PageResponse.of(usages, page, pageSize, itemResult.getTotal());
    }

    /**
     * Find a product and verify it belongs to the current user's store.
     *
     * @throws BusinessException RESOURCE_NOT_FOUND if product doesn't exist
     * @throws BusinessException STORE_FORBIDDEN if product belongs to another store
     */
    public Product findProductForStore(Long userId, Long productId) {
        var leaderAndStore = currentStoreHelper.getLeaderAndStore(userId);
        Store store = leaderAndStore.getStore();

        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!product.getStoreId().equals(store.getId())) {
            throw new BusinessException(ErrorCode.STORE_FORBIDDEN);
        }
        return product;
    }

    private ProductResponse toProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .storeId(product.getStoreId())
                .name(product.getName())
                .description(product.getDescription())
                .coverImageUrl(product.getCoverImageUrl())
                .basePriceAmount(product.getBasePriceAmount())
                .stock(product.getStock())
                .categoryId(product.getCategoryId())
                .status(product.getStatus())
                .detailImageUrls(contentValidationUtil.deserializeImageUrls(product.getDetailImageUrls()))
                .build();
    }
}
