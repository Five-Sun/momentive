package com.momentive.backend.payment.client;

/**
 * PG사에 결제 승인(confirm)을 요청하기 위한 최소 계약.
 */
public record PaymentConfirmCommand(
        String paymentKey,
        String pgOrderId,
        Integer amount
) {
}
