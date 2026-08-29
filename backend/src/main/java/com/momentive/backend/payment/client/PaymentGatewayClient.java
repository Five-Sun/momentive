package com.momentive.backend.payment.client;

/**
 * 결제 PG사(Toss 등) 연동을 추상화한다. {@code PaymentService}는 이 인터페이스에만 의존해
 * 테스트에서 fake/mock 구현체로 대체할 수 있게 한다.
 */
public interface PaymentGatewayClient {

    PaymentConfirmResult confirm(PaymentConfirmCommand command);
}
