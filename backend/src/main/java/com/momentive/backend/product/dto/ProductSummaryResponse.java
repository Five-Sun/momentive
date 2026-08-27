package com.momentive.backend.product.dto;

import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;

public record ProductSummaryResponse(
        Long id,
        String name,
        Integer price,
        Integer discountPrice,
        Boolean soldOut,
        Category category,
        String thumbnailUrl
) {

    public static ProductSummaryResponse from(Product product) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.getSoldOut(),
                product.getCategory(),
                product.getThumbnailUrl()
        );
    }
}
