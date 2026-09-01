package com.momentive.backend.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momentive.backend.address.dto.AddressRequest;
import com.momentive.backend.address.repository.AddressRepository;
import com.momentive.backend.auth.domain.User;
import com.momentive.backend.auth.repository.RefreshTokenRepository;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderStatus;
import com.momentive.backend.order.dto.OrderConfirmRequest;
import com.momentive.backend.order.dto.OrderCreateRequest;
import com.momentive.backend.order.dto.OrderItemRequest;
import com.momentive.backend.order.dto.OrderResponse;
import com.momentive.backend.order.dto.OrderStatusResponse;
import com.momentive.backend.order.repository.OrderRepository;
import com.momentive.backend.order.service.OrderService;
import com.momentive.backend.payment.service.OrderExpirationService;
import com.momentive.backend.payment.service.PaymentService;
import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.repository.ProductRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * PaymentService(confirm/cancel)와 OrderExpirationService(만료 스케줄러가 위임하는 실제 상태 전이 로직)를
 * FakePaymentGatewayClient로 대체해 검증한다.
 * plan Phase 2 마지막 step: confirm 성공/실패, 재confirm 거부, 취소 성공/거부, 만료 전이를 커버한다.
 */
@SpringBootTest
@org.springframework.context.annotation.Import(FakePaymentGatewayClient.Config.class)
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderExpirationService orderExpirationService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private FakePaymentGatewayClient fakePaymentGatewayClient;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        cleanUp();
        fakePaymentGatewayClient.forceFailure(false);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        fakePaymentGatewayClient.forceFailure(false);
    }

    private void cleanUp() {
        orderRepository.deleteAll();
        addressRepository.deleteAll();
        productRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser(String email) {
        return userRepository.save(User.createUser(email, "hash", "몽이"));
    }

    private Product createProduct(String name, int price, int stock) {
        return productRepository.save(new Product(name, "desc", price, null, false, Category.ACCESSORY, stock));
    }

    private AddressRequest newAddressRequest() {
        return new AddressRequest("몽이", "010-1111-2222", "12345", "서울시 강남구", "101호", true);
    }

    private OrderResponse createPendingOrder(User user, Product product, int quantity) {
        return orderService.createOrder(user.getId(), new OrderCreateRequest(
                List.of(new OrderItemRequest(product.getId(), quantity, null)), null, newAddressRequest(), null));
    }

    @Test
    void confirmOrder_success_transitions_to_paid_and_records_payment_key() {
        User user = createUser("confirm-success@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        OrderResponse pending = createPendingOrder(user, product, 2);

        OrderStatusResponse response = paymentService.confirmOrder(user.getId(), pending.orderId(),
                new OrderConfirmRequest("payKey-1", "toss-order-1", pending.totalAmount()));

        assertThat(response.status()).isEqualTo(OrderStatus.PAID);

        Order reloaded = orderRepository.findById(pending.orderId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(reloaded.getTossPaymentKey()).isEqualTo("payKey-1");

        // 결제 성공 시 재고는 이미 주문 생성 시점에 선점됐으므로 추가로 복원되지 않아야 한다.
        Product reloadedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloadedProduct.getStock()).isEqualTo(3);
    }

    @Test
    void confirmOrder_failure_transitions_to_failed_and_restores_stock() {
        User user = createUser("confirm-fail@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        OrderResponse pending = createPendingOrder(user, product, 2);
        fakePaymentGatewayClient.forceFailure(true);

        assertThatThrownBy(() -> paymentService.confirmOrder(user.getId(), pending.orderId(),
                new OrderConfirmRequest("payKey-2", "toss-order-2", pending.totalAmount())))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_CONFIRM_FAILED);

        Order reloaded = orderRepository.findById(pending.orderId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.FAILED);

        Product reloadedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloadedProduct.getStock()).isEqualTo(5);
    }

    @Test
    void confirmOrder_on_already_processed_order_is_rejected_with_order_not_pending() {
        User user = createUser("confirm-reconfirm@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        OrderResponse pending = createPendingOrder(user, product, 1);

        paymentService.confirmOrder(user.getId(), pending.orderId(),
                new OrderConfirmRequest("payKey-3", "toss-order-3", pending.totalAmount()));

        assertThatThrownBy(() -> paymentService.confirmOrder(user.getId(), pending.orderId(),
                new OrderConfirmRequest("payKey-3-retry", "toss-order-3", pending.totalAmount())))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_PENDING);
    }

    @Test
    void cancelOrder_success_restores_stock() {
        User user = createUser("cancel-success@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        OrderResponse pending = createPendingOrder(user, product, 2);
        paymentService.confirmOrder(user.getId(), pending.orderId(),
                new OrderConfirmRequest("payKey-4", "toss-order-4", pending.totalAmount()));

        OrderStatusResponse response = paymentService.cancelOrder(user.getId(), pending.orderId());

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);

        Product reloadedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloadedProduct.getStock()).isEqualTo(5);
    }

    @Test
    void cancelOrder_on_non_paid_order_is_rejected_with_order_not_cancellable() {
        User user = createUser("cancel-invalid@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        OrderResponse pending = createPendingOrder(user, product, 1);

        assertThatThrownBy(() -> paymentService.cancelOrder(user.getId(), pending.orderId()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_CANCELLABLE);
    }

    @Test
    void expireOrder_transitions_pending_order_to_failed_and_restores_stock() {
        User user = createUser("expire@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        OrderResponse pending = createPendingOrder(user, product, 3);

        // 스케줄러가 만료 대상으로 판단하는 상황(생성 시각이 타임아웃을 지남)을 재현하기 위해 createdAt을 과거로 되돌린다.
        transactionTemplate.executeWithoutResult(status -> {
            Order order = orderRepository.findById(pending.orderId()).orElseThrow();
            ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now().minusHours(1));
        });

        orderExpirationService.expireOrder(pending.orderId());

        Order reloaded = orderRepository.findById(pending.orderId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.FAILED);

        Product reloadedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloadedProduct.getStock()).isEqualTo(5);
    }
}
