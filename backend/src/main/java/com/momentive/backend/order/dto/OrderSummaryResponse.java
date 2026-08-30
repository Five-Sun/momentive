package com.momentive.backend.order.dto;

import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        @Schema(description = "주문 ID") Long orderId,
        @Schema(description = "주문 상태") OrderStatus status,
        @Schema(description = "총 결제 금액") Integer totalAmount,
        @Schema(description = "주문 생성 일시") LocalDateTime createdAt,
        @Schema(description = "주문 상품 요약 텍스트") String itemsSummary
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
