package com.momentive.backend.product.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 관리자 상품 목록 응답. 결과가 없어도 {@code content}가 빈 배열인 바디를 항상 내려보낸다
 * (빈 바디를 파싱하려다 터지는 계약 불일치를 만들지 않기 위함).
 */
public record AdminProductListResponse(
        @Schema(description = "상품 목록") List<AdminProductSummaryResponse> content,
        @Schema(description = "현재 페이지 번호(0부터 시작)") int page,
        @Schema(description = "페이지당 항목 수") int size,
        @Schema(description = "전체 항목 수") long totalElements,
        @Schema(description = "전체 페이지 수") int totalPages
) {

    public static AdminProductListResponse from(Page<AdminProductSummaryResponse> page) {
        return new AdminProductListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
