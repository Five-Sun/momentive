package com.momentive.backend.order.dto;

import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderStatus;

public record OrderStatusResponse(
        Long orderId,
        OrderStatus status
) {

    public static OrderStatusResponse from(Order order) {
        return new OrderStatusResponse(order.getId(), order.getStatus());
    }
}
