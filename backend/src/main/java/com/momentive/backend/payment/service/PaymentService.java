package com.momentive.backend.payment.service;

import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.dto.OrderConfirmRequest;
import com.momentive.backend.order.dto.OrderStatusResponse;
import com.momentive.backend.payment.client.PaymentConfirmCommand;
import com.momentive.backend.payment.client.PaymentConfirmResult;
import com.momentive.backend.payment.client.PaymentGatewayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Toss 결제 승인(confirm)/사용자 취소에 따른 {@code Order} 상태 전이를 오케스트레이션한다.
 * 외부 PG 호출은 {@link PaymentGatewayClient} 인터페이스에만 의존해 테스트에서 대체 가능하게 하고,
 * DB 트랜잭션 경계는 {@link OrderPaymentTransactionSupport}로 위임한다 —
 * Toss confirm 호출을 "락을 들고 있지 않은 상태"로 수행하기 위해 PENDING 검증 트랜잭션을 먼저 커밋한 뒤 호출한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderPaymentTransactionSupport transactionSupport;
    private final PaymentGatewayClient paymentGatewayClient;

    /**
     * confirm 재시도 자체를 허용하지 않는다 — PENDING이 아닌 주문에 대한 재호출은 ORDER_NOT_PENDING으로 거부한다.
     */
    public OrderStatusResponse confirmOrder(Long userId, Long orderId, OrderConfirmRequest request) {
        transactionSupport.assertPendingAndOwned(userId, orderId, request.amount());

        PaymentConfirmResult result = paymentGatewayClient.confirm(
                new PaymentConfirmCommand(request.paymentKey(), request.orderId(), request.amount()));

        if (result.success()) {
            Order paid = transactionSupport.markPaid(orderId, result.paymentKey());
            return OrderStatusResponse.from(paid);
        }

        log.warn("Toss confirm 실패: orderId={}, failureCode={}, failureMessage={}",
                orderId, result.failureCode(), result.failureMessage());
        transactionSupport.markFailedAndRestoreStock(orderId);
        throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
    }

    public OrderStatusResponse cancelOrder(Long userId, Long orderId) {
        Order cancelled = transactionSupport.cancelOrder(userId, orderId);
        return OrderStatusResponse.from(cancelled);
    }
}
