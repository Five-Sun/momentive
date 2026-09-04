package com.momentive.backend.product.dto.admin;

import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.domain.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 상품 목록의 한 행. 목록 화면의 열(썸네일/이름/카테고리/가격/재고 합/상태)에 대응한다.
 */
public record AdminProductSummaryResponse(
        @Schema(description = "상품 ID") Long id,
        @Schema(description = "상품명") String name,
        @Schema(description = "카테고리") Category category,
        @Schema(description = "정가") Integer price,
        @Schema(description = "할인가(없으면 null)") Integer discountPrice,
        @Schema(description = "전체 재고 합") Integer totalStock,
        @Schema(description = "판매 상태") ProductStatus status,
        @Schema(description = "썸네일 이미지 URL(이미지가 없으면 null)") String thumbnailUrl
) {

    public static AdminProductSummaryResponse from(Product product) {
        return new AdminProductSummaryResponse(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.getTotalStock(),
                product.getStatus(),
                product.getThumbnailUrl()
        );
    }
}
