package com.momentive.backend.product.dto;

import com.momentive.backend.product.domain.ProductImage;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductImageResponse(
        @Schema(description = "이미지 ID") Long id,
        @Schema(description = "이미지 URL") String url,
        @Schema(description = "노출 순서") Integer displayOrder
) {

    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(image.getId(), image.getUrl(), image.getDisplayOrder());
    }
}
