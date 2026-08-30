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
        @Schema(description = "품절 여부") Boolean soldOut,
        @Schema(description = "카테고리") Category category,
        @Schema(description = "상품 이미지 목록") List<ProductImageResponse> images
) {

    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.getSoldOut(),
                product.getCategory(),
                product.getImages().stream().map(ProductImageResponse::from).toList()
        );
    }
}
