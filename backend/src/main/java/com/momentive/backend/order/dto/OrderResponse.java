package com.momentive.backend.order.dto;

import com.momentive.backend.address.dto.AddressResponse;
import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        OrderStatus status,
        Integer totalAmount,
        List<OrderItemResponse> items,
        AddressResponse address,
        LocalDateTime createdAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                AddressResponse.from(order.getAddress()),
                order.getCreatedAt()
        );
    }
}
