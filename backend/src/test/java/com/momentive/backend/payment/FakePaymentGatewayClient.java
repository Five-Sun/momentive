package com.momentive.backend.payment;

import com.momentive.backend.payment.client.PaymentConfirmCommand;
import com.momentive.backend.payment.client.PaymentConfirmResult;
import com.momentive.backend.payment.client.PaymentGatewayClient;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 테스트에서 실제 Toss API를 호출하지 않도록 {@link PaymentGatewayClient}를 대체하는 fake 구현체.
 * {@link #forceFailure}로 confirm 실패 시나리오를 강제할 수 있다.
 */
public class FakePaymentGatewayClient implements PaymentGatewayClient {

    private final AtomicBoolean forceFailure = new AtomicBoolean(false);

    public void forceFailure(boolean value) {
        forceFailure.set(value);
    }

    @Override
    public PaymentConfirmResult confirm(PaymentConfirmCommand command) {
        if (forceFailure.get()) {
            return PaymentConfirmResult.failure("FAKE_FAILURE", "테스트 강제 실패");
        }
        return PaymentConfirmResult.success(command.paymentKey());
    }

    @TestConfiguration
    public static class Config {
        @Bean
        @Primary
        public PaymentGatewayClient fakePaymentGatewayClient() {
            return new FakePaymentGatewayClient();
        }
    }
}
