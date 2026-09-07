package com.momentive.backend.product.dto.admin;

import com.momentive.backend.product.domain.ProductVariant;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 응답의 variant 한 행. 수정 폼이 그대로 되돌려 보낼 수 있도록
 * 요청 DTO와 같은 필드명({@code id})을 쓴다.
 */
public record AdminProductVariantResponse(
        @Schema(description = "variant ID") Long id,
        @Schema(description = "사이즈 이름. 사이즈가 없는 상품은 null") String size,
        @Schema(description = "재고 수량") Integer stock
) {

    public static AdminProductVariantResponse from(ProductVariant variant) {
        return new AdminProductVariantResponse(variant.getId(), variant.getSize(), variant.getStock());
    }
}
