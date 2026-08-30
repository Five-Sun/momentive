package com.momentive.backend.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

public record ProductListResponse(
        @Schema(description = "상품 목록") List<ProductSummaryResponse> content,
        @Schema(description = "현재 페이지 번호(0부터 시작)") int page,
        @Schema(description = "페이지당 항목 수") int size,
        @Schema(description = "전체 항목 수") long totalElements,
        @Schema(description = "전체 페이지 수") int totalPages
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
