package com.momentive.backend.payment.service;

import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.coupon.domain.UserCoupon;
import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderItem;
import com.momentive.backend.order.repository.OrderRepository;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.repository.ProductRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link PaymentService}의 confirm 흐름은 "PENDING 검증"과 "PG 호출 결과 반영"을 서로 다른
 * DB 트랜잭션으로 분리해야 한다(락을 들고 있지 않은 상태로 Toss confirm을 호출하기 위함).
 * 같은 클래스 내 self-invocation은 Spring 프록시가 가로채지 못해 별도 트랜잭션이 시작되지 않으므로,
 * 트랜잭션 경계가 필요한 각 단계를 별도 빈으로 분리한다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
class OrderPaymentTransactionSupport {

    private static final int MAX_STOCK_RETRY = 2;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    Order assertPendingAndOwned(Long userId, Long orderId, Integer amount) {
        Order order = getOwnedOrder(userId, orderId);
        if (!order.isPending()) {
            throw new CustomException(ErrorCode.ORDER_NOT_PENDING);
        }
        // 요청 온 금액과 Order.totalAmount가 다르면 위변조로 간주해 confirm 자체를 시도하지 않는다.
        if (!order.getTotalAmount().equals(amount)) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        return order;
    }

    @Transactional
    Order markPaid(Long orderId, String paymentKey) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        order.markAsPaid(paymentKey);
        return order;
    }

    @Transactional
    void markFailedAndRestoreStock(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.isPending()) {
            return;
        }
        order.markAsFailed();
        restoreStockWithRetry(order);
        restoreCouponIfUsed(order);
    }

    @Transactional
    Order cancelOrder(Long userId, Long orderId) {
        Order order = getOwnedOrder(userId, orderId);
        if (!order.isCancellable()) {
            throw new CustomException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }
        order.markAsCancelled();
        restoreStockWithRetry(order);
        restoreCouponIfUsed(order);
        return order;
    }

    /**
     * 결제 실패/PENDING 만료/PAID 취소 3개 경로 공통으로, 주문에 적용된 쿠폰이 있으면 사용 가능 상태로 복원한다.
     * 복원 시점에 이미 유효기간이 지났다면 만료된 쿠폰으로 남을 뿐 별도 처리는 하지 않는다(spec 예외 케이스).
     */
    private void restoreCouponIfUsed(Order order) {
        UserCoupon userCoupon = order.getUserCoupon();
        if (userCoupon != null) {
            userCoupon.restore();
        }
    }

    private void restoreStockWithRetry(Order order) {
        for (OrderItem item : order.getItems()) {
            restoreStockWithRetry(item.getProduct().getId(), item.getQuantity());
        }
    }

    /**
     * 재고 원복도 @Version 낙관적 락 대상이므로 차감과 동일한 재시도 정책(최초 시도 + 최대 2회)을 적용한다.
     */
    private void restoreStockWithRetry(Long productId, int quantity) {
        int retryCount = 0;
        while (true) {
            try {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
                product.restoreStock(quantity);
                productRepository.saveAndFlush(product);
                return;
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                retryCount++;
                if (retryCount > MAX_STOCK_RETRY) {
                    throw new CustomException(ErrorCode.STOCK_CONFLICT);
                }
            }
        }
    }

    private Order getOwnedOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return order;
    }
}
