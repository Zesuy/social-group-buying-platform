package com.example.groupshop.category.controller;

import com.example.groupshop.category.dto.CategoryResponse;
import com.example.groupshop.category.service.CategoryService;
import com.example.groupshop.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for product categories (public, no auth required).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * List all active categories.
     */
    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> listCategories() {
        return ApiResponse.success(categoryService.listCategories());
    }
}
