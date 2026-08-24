package com.momentive.backend.product.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record ProductListResponse(
        List<ProductSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ProductListResponse from(Page<ProductSummaryResponse> page) {
        return new ProductListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
