package com.momentive.backend.order.dto;

public record OutOfStockItem(
        Long productId,
        String productName,
        Integer requestedQuantity,
        Integer availableStock
) {
}
