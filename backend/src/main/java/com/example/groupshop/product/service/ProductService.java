package com.example.groupshop.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.common.util.CurrentStoreHelper;
import com.example.groupshop.model.entity.Product;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.mapper.ProductMapper;
import com.example.groupshop.product.dto.CreateProductRequest;
import com.example.groupshop.product.dto.ProductResponse;
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
    private final CurrentStoreHelper currentStoreHelper;

    /**
     * Create a product for the current user's store.
     */
    @Transactional
    public ProductResponse createProduct(Long userId, CreateProductRequest request) {
        var leaderAndStore = currentStoreHelper.getLeaderAndStore(userId);
        Store store = leaderAndStore.getStore();

        Product product = new Product();
        product.setStoreId(store.getId());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCoverImageUrl(request.getCoverImageUrl());
        product.setBasePriceAmount(request.getBasePriceAmount());
        product.setStock(request.getStock());
        product.setStatus("active");
        productMapper.insert(product);

        return toProductResponse(product);
    }

    /**
     * List products for the current user's store (excluding soft-deleted).
     */
    public PageResponse<ProductResponse> getMyStoreProducts(Long userId, int page, int pageSize) {
        var leaderAndStore = currentStoreHelper.getLeaderAndStore(userId);
        Store store = leaderAndStore.getStore();

        Page<Product> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStoreId, store.getId())
                .ne(Product::getStatus, "deleted")
                .orderByDesc(Product::getCreatedAt);

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
                .status(product.getStatus())
                .build();
    }
}
