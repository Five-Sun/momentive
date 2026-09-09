package com.momentive.backend.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momentive.backend.address.domain.Address;
import com.momentive.backend.auth.domain.User;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

/**
 * plan Phase 1 마지막 step: markAsPaid의 shippingStatus 자동 초기화와
 * updateShipping의 불변식(PAID 상태에서만 변경 가능, SHIPPING/DELIVERED 전환 시
 * 택배사·송장번호 필수, 순서 제약 없음, PREPARING 복귀 시 기존 값 유지)을 검증한다.
 */
class OrderTest {

    private User user() {
        return User.createUser("test@momentive.com", "hash", "몽이");
    }

    private Address address() {
        return Address.create(user(), "몽이", "010-1111-2222", "12345", "서울시 강남구", "101호", true);
    }

    private Order pendingOrder() {
        return Order.createPending(user(), address(), 10_000);
    }

    private Order paidOrder() {
        Order order = pendingOrder();
        order.markAsPaid("payKey-1");
        return order;
    }

    @Test
    void markAsPaid_sets_shipping_status_to_preparing() {
        Order order = pendingOrder();

        order.markAsPaid("payKey-1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getShippingStatus()).isEqualTo(ShippingStatus.PREPARING);
    }

    @Test
    void updateShipping_on_non_paid_order_throws_order_shipping_not_applicable() {
        Order order = pendingOrder();

        assertThatThrownBy(() -> order.updateShipping(ShippingStatus.SHIPPING, "CJ대한통운", "123456789012"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_SHIPPING_NOT_APPLICABLE);
    }

    @Test
    void updateShipping_to_shipping_without_courier_and_tracking_number_throws_shipping_info_required() {
        Order order = paidOrder();

        assertThatThrownBy(() -> order.updateShipping(ShippingStatus.SHIPPING, null, null))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SHIPPING_INFO_REQUIRED);
        assertThat(order.getShippingStatus()).isEqualTo(ShippingStatus.PREPARING);
    }

    @Test
    void updateShipping_to_delivered_without_courier_and_tracking_number_throws_shipping_info_required() {
        Order order = paidOrder();

        assertThatThrownBy(() -> order.updateShipping(ShippingStatus.DELIVERED, "CJ대한통운", " "))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SHIPPING_INFO_REQUIRED);
        assertThat(order.getShippingStatus()).isEqualTo(ShippingStatus.PREPARING);
    }

    @Test
    void updateShipping_allows_reverting_from_delivered_to_shipping_without_order_constraint() {
        Order order = paidOrder();
        order.updateShipping(ShippingStatus.DELIVERED, "CJ대한통운", "123456789012");

        order.updateShipping(ShippingStatus.SHIPPING, "우체국택배", "987654321098");

        assertThat(order.getShippingStatus()).isEqualTo(ShippingStatus.SHIPPING);
        assertThat(order.getCourier()).isEqualTo("우체국택배");
        assertThat(order.getTrackingNumber()).isEqualTo("987654321098");
    }

    @Test
    void updateShipping_to_preparing_keeps_existing_courier_and_tracking_number() {
        Order order = paidOrder();
        order.updateShipping(ShippingStatus.SHIPPING, "CJ대한통운", "123456789012");

        order.updateShipping(ShippingStatus.PREPARING, null, null);

        assertThat(order.getShippingStatus()).isEqualTo(ShippingStatus.PREPARING);
        assertThat(order.getCourier()).isEqualTo("CJ대한통운");
        assertThat(order.getTrackingNumber()).isEqualTo("123456789012");
    }
}
