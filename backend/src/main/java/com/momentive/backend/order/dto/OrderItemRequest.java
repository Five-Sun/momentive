package com.momentive.backend.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @Schema(description = "상품 ID") @NotNull Long productId,
        @Schema(description = "재고 단위(사이즈) ID. 사이즈가 없는 상품도 단일 variant의 ID를 보낸다")
        @NotNull Long variantId,
        @Schema(description = "주문 수량") @NotNull @Positive Integer quantity
) {
}
