package com.momentive.backend.payment.client;

/**
 * PG사 결제 승인 결과. 성공 시 {@code success=true}와 함께 실제 승인된 결제 수단 식별자(paymentKey)를,
 * 실패 시 원본 에러코드/메시지를 담는다 — 원본 코드는 우리 {@code ErrorCode}로 1:1 매핑하지 않고
 * 로그/{@code ErrorResponse} 상세 필드에만 남기기 위해 별도로 보존한다.
 */
public record PaymentConfirmResult(
        boolean success,
        String paymentKey,
        String failureCode,
        String failureMessage
) {

    public static PaymentConfirmResult success(String paymentKey) {
        return new PaymentConfirmResult(true, paymentKey, null, null);
    }

    public static PaymentConfirmResult failure(String failureCode, String failureMessage) {
        return new PaymentConfirmResult(false, null, failureCode, failureMessage);
    }
}
