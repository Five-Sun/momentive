package com.momentive.backend.order.dto;

import com.momentive.backend.order.domain.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;

public record OrderItemResponse(
        @Schema(description = "상품 ID") Long productId,
        @Schema(description = "상품명") String productName,
        @Schema(description = "재고 단위(variant) ID. variant 도입 이전 주문은 null") Long variantId,
        @Schema(description = "주문 수량") Integer quantity,
        @Schema(description = "옵션(사이즈)") String size,
        @Schema(description = "주문 시점 단가") Integer unitPrice
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getVariant() == null ? null : item.getVariant().getId(),
                item.getQuantity(),
                item.getSize(),
                item.getUnitPrice()
        );
    }
}
