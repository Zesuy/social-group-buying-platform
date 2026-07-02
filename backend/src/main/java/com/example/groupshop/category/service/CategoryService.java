package com.example.groupshop.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.groupshop.category.dto.CategoryResponse;
import com.example.groupshop.model.entity.ProductCategory;
import com.example.groupshop.model.mapper.ProductCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for product categories.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final ProductCategoryMapper productCategoryMapper;

    /**
     * List all active categories, ordered by sort_order ascending.
     */
    public List<CategoryResponse> listCategories() {
        List<ProductCategory> categories = productCategoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getStatus, "active")
                        .orderByAsc(ProductCategory::getSortOrder));

        return categories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Validate that a category exists and is active.
     *
     * @return true if the category is active, false if null (null is allowed for inline-created products)
     */
    public boolean isActiveCategory(Long categoryId) {
        if (categoryId == null) {
            return false;
        }
        ProductCategory category = productCategoryMapper.selectById(categoryId);
        return category != null && "active".equals(category.getStatus());
    }

    private CategoryResponse toResponse(ProductCategory category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .code(category.getCode())
                .parentId(category.getParentId())
                .level(category.getLevel())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .build();
    }
}
