package com.momentive.backend.product.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 상품 등록·수정 요청의 사이즈별 재고 한 행.
 */
public record AdminProductVariantRequest(
        @Schema(description = "기존 variant ID. null이면 신규 추가, 값이 있으면 해당 variant 갱신")
        Long id,

        @Schema(description = "사이즈 이름. 사이즈가 없는 상품은 null(또는 빈 문자열)로 보내며 상품당 한 행만 허용된다")
        @Size(max = 50)
        String size,

        @Schema(description = "해당 사이즈의 재고 수량")
        @NotNull @Min(0)
        Integer stock
) {
}
