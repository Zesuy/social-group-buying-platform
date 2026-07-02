package com.example.groupshop.category;

import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.category.dto.CategoryResponse;
import com.example.groupshop.category.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CategoryService}.
 */
@Transactional
class CategoryServiceTest extends ServiceTestBase {

    @Autowired
    private CategoryService categoryService;

    @Test
    void listCategories_shouldReturnAllActiveCategoriesSortedBySortOrder() {
        List<CategoryResponse> categories = categoryService.listCategories();

        assertThat(categories).hasSize(6);
        assertThat(categories).extracting(CategoryResponse::getName)
                .containsExactly("生鲜水果", "蔬菜食品", "肉禽蛋奶", "熟食烘焙", "日用百货", "其他");
    }

    @Test
    void listCategories_shouldIncludeAllExpectedFields() {
        List<CategoryResponse> categories = categoryService.listCategories();

        CategoryResponse first = categories.get(0);
        assertThat(first.getId()).isPositive();
        assertThat(first.getName()).isEqualTo("生鲜水果");
        assertThat(first.getCode()).isEqualTo("fresh_fruit");
        assertThat(first.getParentId()).isNull();
        assertThat(first.getLevel()).isEqualTo(1);
        assertThat(first.getSortOrder()).isEqualTo(1);
        assertThat(first.getStatus()).isEqualTo("active");
    }

    @Test
    void listCategories_shouldHaveNullParentIdForLevelOne() {
        List<CategoryResponse> categories = categoryService.listCategories();

        assertThat(categories).allSatisfy(cat -> {
            assertThat(cat.getLevel()).isEqualTo(1);
            assertThat(cat.getParentId()).isNull();
        });
    }

    @Test
    void listCategories_shouldReturnCategoriesOrderedBySortOrder() {
        List<CategoryResponse> categories = categoryService.listCategories();

        assertThat(categories).extracting(CategoryResponse::getSortOrder)
                .containsExactly(1, 2, 3, 4, 5, 6);
    }
}
