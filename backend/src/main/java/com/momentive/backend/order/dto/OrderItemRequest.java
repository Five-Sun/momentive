package com.momentive.backend.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @Schema(description = "상품 ID") @NotNull Long productId,
        @Schema(description = "주문 수량") @NotNull @Positive Integer quantity,
        @Schema(description = "옵션(사이즈)") String size
) {
}
