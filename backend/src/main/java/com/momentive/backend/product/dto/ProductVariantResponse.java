package com.momentive.backend.product.dto;

import com.momentive.backend.product.domain.ProductVariant;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductVariantResponse(
        @Schema(description = "재고 단위(사이즈) ID") Long variantId,
        @Schema(description = "사이즈 이름. 사이즈가 없는 상품은 null") String size,
        @Schema(description = "해당 사이즈의 재고 수량") Integer stock,
        @Schema(description = "해당 사이즈 품절 여부(재고 0에서 파생)") Boolean soldOut
) {

    public static ProductVariantResponse from(ProductVariant variant) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getSize(),
                variant.getStock(),
                variant.isSoldOut()
        );
    }
}
