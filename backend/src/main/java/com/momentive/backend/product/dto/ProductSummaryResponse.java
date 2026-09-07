package com.momentive.backend.product.dto;

import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductSummaryResponse(
        @Schema(description = "상품 ID") Long id,
        @Schema(description = "상품명") String name,
        @Schema(description = "정가") Integer price,
        @Schema(description = "할인가") Integer discountPrice,
        @Schema(description = "품절 여부(전체 사이즈 재고 합 0에서 파생)") Boolean soldOut,
        @Schema(description = "카테고리") Category category,
        @Schema(description = "썸네일 이미지 URL") String thumbnailUrl,
        @Schema(description = "평균 평점(리뷰 없으면 null)") Double averageRating,
        @Schema(description = "리뷰 개수") Integer reviewCount
) {

    public static ProductSummaryResponse from(Product product) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.isSoldOut(),
                product.getCategory(),
                product.getThumbnailUrl(),
                product.getAverageRating(),
                product.getReviewCount()
        );
    }
}
