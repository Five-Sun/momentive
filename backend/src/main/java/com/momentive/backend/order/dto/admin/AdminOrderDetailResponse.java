package com.momentive.backend.order.dto.admin;

import com.momentive.backend.address.dto.AddressResponse;
import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderStatus;
import com.momentive.backend.order.domain.ShippingStatus;
import com.momentive.backend.order.dto.OrderItemResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 주문 상세. 고객용 {@code OrderResponse}에 상당하는 정보에 주문자 이메일/닉네임을 더한다.
 */
public record AdminOrderDetailResponse(
        @Schema(description = "주문 ID") Long orderId,
        @Schema(description = "주문자 이메일") String userEmail,
        @Schema(description = "주문자 닉네임") String userNickname,
        @Schema(description = "주문 상태") OrderStatus status,
        @Schema(description = "상품 금액 합계(배송비 제외)") Integer itemsSubtotal,
        @Schema(description = "배송비") Integer shippingFee,
        @Schema(description = "쿠폰 할인 금액") Integer discountAmount,
        @Schema(description = "사용한 쿠폰 이름 (미사용 시 null)") String couponName,
        @Schema(description = "총 결제 금액") Integer totalAmount,
        @Schema(description = "주문 상품 목록") List<OrderItemResponse> items,
        @Schema(description = "배송지") AddressResponse address,
        @Schema(description = "배송상태(결제 완료 이전이거나 취소·실패한 주문은 null)") ShippingStatus shippingStatus,
        @Schema(description = "택배사") String courier,
        @Schema(description = "송장번호") String trackingNumber,
        @Schema(description = "주문 생성 일시") LocalDateTime createdAt
) {

    public static AdminOrderDetailResponse from(Order order) {
        return new AdminOrderDetailResponse(
                order.getId(),
                order.getUser().getEmail(),
                order.getUser().getNickname(),
                order.getStatus(),
                order.getItemsSubtotal(),
                order.getShippingFee(),
                order.getDiscountAmount(),
                order.getUserCoupon() != null ? order.getUserCoupon().getCoupon().getName() : null,
                order.getTotalAmount(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                AddressResponse.from(order.getAddress()),
                order.getShippingStatus(),
                order.getCourier(),
                order.getTrackingNumber(),
                order.getCreatedAt()
        );
    }
}
