package com.momentive.backend.product.dto;

import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ProductDetailResponse(
        @Schema(description = "상품 ID") Long id,
        @Schema(description = "상품명") String name,
        @Schema(description = "상품 설명") String description,
        @Schema(description = "정가") Integer price,
        @Schema(description = "할인가") Integer discountPrice,
        @Schema(description = "품절 여부(전체 사이즈 재고 합 0에서 파생)") Boolean soldOut,
        @Schema(description = "카테고리") Category category,
        @Schema(description = "상품 이미지 목록") List<ProductImageResponse> images,
        @Schema(description = "사이즈별 재고 목록. 사이즈가 없는 상품은 size가 null인 단일 항목")
        List<ProductVariantResponse> variants,
        @Schema(description = "평균 평점(리뷰 없으면 null)") Double averageRating,
        @Schema(description = "리뷰 개수") Integer reviewCount
) {

    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.isSoldOut(),
                product.getCategory(),
                product.getImages().stream().map(ProductImageResponse::from).toList(),
                product.getVariants().stream().map(ProductVariantResponse::from).toList(),
                product.getAverageRating(),
                product.getReviewCount()
        );
    }
}
