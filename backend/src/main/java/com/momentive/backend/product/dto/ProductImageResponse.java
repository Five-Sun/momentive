package com.momentive.backend.product.dto;

import com.momentive.backend.product.domain.ProductImage;

public record ProductImageResponse(Long id, String url, Integer displayOrder) {

    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(image.getId(), image.getUrl(), image.getDisplayOrder());
    }
}
