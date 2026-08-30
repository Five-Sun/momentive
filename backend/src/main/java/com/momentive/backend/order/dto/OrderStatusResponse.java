package com.momentive.backend.order.dto;

import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record OrderStatusResponse(
        @Schema(description = "주문 ID") Long orderId,
        @Schema(description = "주문 상태") OrderStatus status
) {

    public static OrderStatusResponse from(Order order) {
        return new OrderStatusResponse(order.getId(), order.getStatus());
    }
}
