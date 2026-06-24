package com.example.groupshop.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated list response wrapper.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> items;
    private int page;
    private int pageSize;
    private long total;
    private boolean hasMore;

    /**
     * Create a PageResponse from a MyBatis-Plus Page object (or any page-like result).
     *
     * @param items    the list of items on the current page
     * @param page     current page number (1-based)
     * @param pageSize number of items per page
     * @param total    total number of items across all pages
     */
    public static <T> PageResponse<T> of(List<T> items, int page, int pageSize, long total) {
        return PageResponse.<T>builder()
                .items(items)
                .page(page)
                .pageSize(pageSize)
                .total(total)
                .hasMore((long) page * pageSize < total)
                .build();
    }
}
