package com.momentive.backend.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record OutOfStockItem(
        @Schema(description = "상품 ID") Long productId,
        @Schema(description = "상품명") String productName,
        @Schema(description = "요청 수량") Integer requestedQuantity,
        @Schema(description = "재고 수량") Integer availableStock
) {
}
