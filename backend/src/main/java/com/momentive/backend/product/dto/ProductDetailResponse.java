package com.momentive.backend.product.dto;

import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String name,
        String description,
        Integer price,
        Integer discountPrice,
        Boolean soldOut,
        Category category,
        List<ProductImageResponse> images
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
