package com.clothingretail.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Generic paginated response envelope matching the frontend API contract:
 * {content, page, size, totalElements, totalPages}.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    public static <T, R> PageResponse<R> of(Page<T> page, List<R> mappedContent) {
        return new PageResponse<>(
                mappedContent,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
