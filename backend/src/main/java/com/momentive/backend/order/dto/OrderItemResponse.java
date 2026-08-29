package com.momentive.backend.order.dto;

import com.momentive.backend.order.domain.OrderItem;

public record OrderItemResponse(
        Long productId,
        String productName,
        Integer quantity,
        String size,
        Integer unitPrice
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getSize(),
                item.getUnitPrice()
        );
    }
}
