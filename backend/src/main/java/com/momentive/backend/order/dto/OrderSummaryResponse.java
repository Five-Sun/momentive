package com.momentive.backend.order.dto;

import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderStatus;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long orderId,
        OrderStatus status,
        Integer totalAmount,
        LocalDateTime createdAt,
        String itemsSummary
) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                buildItemsSummary(order)
        );
    }

    private static String buildItemsSummary(Order order) {
        if (order.getItems().isEmpty()) {
            return "";
        }
        String firstItemName = order.getItems().get(0).getProduct().getName();
        int remaining = order.getItems().size() - 1;
        return remaining > 0 ? firstItemName + " 외 " + remaining + "건" : firstItemName;
    }
}
