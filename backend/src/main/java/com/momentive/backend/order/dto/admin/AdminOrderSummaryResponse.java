package com.momentive.backend.order.dto.admin;

import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderStatus;
import com.momentive.backend.order.domain.ShippingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 관리자 주문 목록의 한 행. 목록 화면의 열(주문ID/주문자/총액/주문상태/배송상태/생성일시)에 대응한다.
 */
public record AdminOrderSummaryResponse(
        @Schema(description = "주문 ID") Long orderId,
        @Schema(description = "주문자 이메일") String userEmail,
        @Schema(description = "주문자 닉네임") String userNickname,
        @Schema(description = "총 결제 금액") Integer totalAmount,
        @Schema(description = "주문 상태") OrderStatus status,
        @Schema(description = "배송상태(결제 완료 이전이거나 취소·실패한 주문은 null)") ShippingStatus shippingStatus,
        @Schema(description = "주문 생성 일시") LocalDateTime createdAt
) {

    public static AdminOrderSummaryResponse from(Order order) {
        return new AdminOrderSummaryResponse(
                order.getId(),
                order.getUser().getEmail(),
                order.getUser().getNickname(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getShippingStatus(),
                order.getCreatedAt()
        );
    }
}
