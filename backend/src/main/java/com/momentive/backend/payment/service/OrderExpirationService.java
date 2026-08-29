package com.momentive.backend.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 방치된 PENDING 주문을 FAILED로 전환하고 재고를 복원한다.
 * {@code OrderExpirationScheduler}(배치)와 {@code OrderService.getOrder}(조회 시점 lazy 체크) 양쪽에서 공용으로 사용한다.
 */
@Service
@RequiredArgsConstructor
public class OrderExpirationService {

    private final OrderPaymentTransactionSupport transactionSupport;

    public void expireOrder(Long orderId) {
        transactionSupport.markFailedAndRestoreStock(orderId);
    }
}
