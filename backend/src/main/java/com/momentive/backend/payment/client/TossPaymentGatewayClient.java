package com.momentive.backend.payment.client;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Toss Payments confirm API를 Spring {@code RestClient}로 호출한다.
 * 이중 승인 위험을 피하기 위해 재시도는 하지 않고, 타임아웃은 5초로 짧게 둔다.
 */
@Slf4j
@Component
public class TossPaymentGatewayClient implements PaymentGatewayClient {

    private static final int TIMEOUT_MILLIS = 5000;
    private static final String CONFIRM_PATH = "/v1/payments/confirm";

    private final RestClient restClient;
    private final String secretKey;

    public TossPaymentGatewayClient(
            @Value("${momentive.toss.base-url}") String baseUrl,
            @Value("${momentive.toss.secret-key}") String secretKey
    ) {
        this.secretKey = secretKey;
        ClientHttpRequestFactory requestFactory = createTimeoutBoundRequestFactory();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    private ClientHttpRequestFactory createTimeoutBoundRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(TIMEOUT_MILLIS));
        factory.setReadTimeout(Duration.ofMillis(TIMEOUT_MILLIS));
        return factory;
    }

    @Override
    public PaymentConfirmResult confirm(PaymentConfirmCommand command) {
        try {
            TossConfirmResponse response = restClient.post()
                    .uri(CONFIRM_PATH)
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "paymentKey", command.paymentKey(),
                            "orderId", command.pgOrderId(),
                            "amount", command.amount()
                    ))
                    .retrieve()
                    .body(TossConfirmResponse.class);
            return PaymentConfirmResult.success(response != null ? response.paymentKey() : command.paymentKey());
        } catch (RestClientResponseException e) {
            log.warn("Toss confirm 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return PaymentConfirmResult.failure(String.valueOf(e.getStatusCode().value()), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("Toss confirm 호출 중 예외(타임아웃 등)", e);
            return PaymentConfirmResult.failure("NETWORK_ERROR", e.getMessage());
        }
    }

    private String basicAuthHeader() {
        String credentials = secretKey + ":";
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private record TossConfirmResponse(String paymentKey) {
    }
}
