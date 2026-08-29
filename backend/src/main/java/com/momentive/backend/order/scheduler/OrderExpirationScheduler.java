package com.momentive.backend.order.scheduler;

import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderStatus;
import com.momentive.backend.order.repository.OrderRepository;
import com.momentive.backend.payment.service.OrderExpirationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 방치된 PENDING 주문을 주기적으로 FAILED 전환 + 재고 복원한다.
 * 배치 주기 사이의 공백은 {@code OrderService.getOrder}의 조회 시점 lazy 체크로 보완한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    /**
     * PENDING 주문의 만료 기준 시간(분). Toss 결제위젯 세션의 통상 유효 시간(약 15분)을 고려해
     * 여유를 두고 30분으로 설정한다 — 너무 짧으면 결제 진행 중인 정상 사용자의 주문이 조기 만료될 수 있다.
     */
    public static final long PENDING_TIMEOUT_MINUTES = 30;

    /** 5분마다 만료 대상 PENDING 주문을 훑는다. */
    private static final long FIXED_DELAY_MILLIS = 5 * 60 * 1000L;

    private final OrderRepository orderRepository;
    private final OrderExpirationService orderExpirationService;

    @Scheduled(fixedDelay = FIXED_DELAY_MILLIS)
    public void expireStalePendingOrders() {
        List<Order> candidates = orderRepository.findAllByStatus(OrderStatus.PENDING);
        for (Order order : candidates) {
            if (order.isExpiredPending(PENDING_TIMEOUT_MINUTES)) {
                orderExpirationService.expireOrder(order.getId());
                log.info("PENDING 주문 만료 처리: orderId={}", order.getId());
            }
        }
    }
}
