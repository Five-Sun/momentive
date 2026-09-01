package com.momentive.backend.order.dto;

import com.momentive.backend.address.dto.AddressResponse;
import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        @Schema(description = "주문 ID") Long orderId,
        @Schema(description = "주문 상태") OrderStatus status,
        @Schema(description = "상품 금액 합계(배송비 제외)") Integer itemsSubtotal,
        @Schema(description = "배송비") Integer shippingFee,
        @Schema(description = "총 결제 금액") Integer totalAmount,
        @Schema(description = "주문 상품 목록") List<OrderItemResponse> items,
        @Schema(description = "배송지") AddressResponse address,
        @Schema(description = "주문 생성 일시") LocalDateTime createdAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getItemsSubtotal(),
                order.getShippingFee(),
                order.getTotalAmount(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                AddressResponse.from(order.getAddress()),
                order.getCreatedAt()
        );
    }
}
