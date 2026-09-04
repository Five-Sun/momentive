package com.momentive.backend.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momentive.backend.address.dto.AddressRequest;
import com.momentive.backend.address.repository.AddressRepository;
import com.momentive.backend.auth.domain.User;
import com.momentive.backend.auth.repository.RefreshTokenRepository;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.coupon.domain.Coupon;
import com.momentive.backend.coupon.domain.DiscountType;
import com.momentive.backend.coupon.domain.UserCoupon;
import com.momentive.backend.coupon.domain.UserCouponStatus;
import com.momentive.backend.coupon.repository.CouponRepository;
import com.momentive.backend.coupon.repository.UserCouponRepository;
import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderStatus;
import com.momentive.backend.order.dto.OrderConfirmRequest;
import com.momentive.backend.order.dto.OrderCreateRequest;
import com.momentive.backend.order.dto.OrderItemRequest;
import com.momentive.backend.order.dto.OrderResponse;
import com.momentive.backend.order.repository.OrderRepository;
import com.momentive.backend.order.service.OrderService;
import com.momentive.backend.payment.FakePaymentGatewayClient;
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
 * plan Phase 2 마지막 step: 쿠폰이 적용된 주문의 금액 계산, 검증 실패 4종, 복원 3경로를 검증한다.
 */
@SpringBootTest
@org.springframework.context.annotation.Import(FakePaymentGatewayClient.Config.class)
class OrderCouponServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

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
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

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
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        addressRepository.deleteAll();
        productRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser(String email) {
        return userRepository.save(User.createUser(email, "hash", "몽이"));
    }

    /**
     * 사이즈가 없는 상품은 {@code size = null}인 단일 variant로 표현한다.
     */
    private Product createProduct(String name, int price, int stock) {
        Product product = new Product(name, "desc", price, null, Category.ACCESSORY);
        product.addVariant(null, stock);
        return productRepository.save(product);
    }

    private AddressRequest newAddressRequest() {
        return new AddressRequest("몽이", "010-1111-2222", "12345", "서울시 강남구", "101호", true);
    }

    private Coupon createCoupon(String code, DiscountType type, int value, Integer maxDiscount, int minOrderAmount) {
        return couponRepository.save(Coupon.create(
                code, code + " 쿠폰", type, value, maxDiscount, minOrderAmount, LocalDateTime.now().plusDays(30)));
    }

    private UserCoupon registerCoupon(User user, Coupon coupon) {
        return userCouponRepository.save(UserCoupon.register(user, coupon));
    }

    private OrderCreateRequest requestWithCoupon(Product product, int quantity, Long userCouponId) {
        return new OrderCreateRequest(
                List.of(new OrderItemRequest(product.getId(), product.getVariants().get(0).getId(), quantity)),
                null, newAddressRequest(), userCouponId);
    }

    @Test
    void createOrder_with_fixed_coupon_applies_discount_and_uses_coupon() {
        User user = createUser("coupon-order-fixed@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        Coupon coupon = createCoupon("FIXED3000", DiscountType.FIXED, 3000, null, 0);
        UserCoupon userCoupon = registerCoupon(user, coupon);

        OrderResponse response = orderService.createOrder(user.getId(),
                requestWithCoupon(product, 2, userCoupon.getId()));

        assertThat(response.itemsSubtotal()).isEqualTo(20000);
        assertThat(response.discountAmount()).isEqualTo(3000);
        assertThat(response.couponName()).isEqualTo(coupon.getName());
        assertThat(response.shippingFee()).isEqualTo(3400);
        assertThat(response.totalAmount()).isEqualTo(20000 - 3000 + 3400);

        UserCoupon reloaded = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserCouponStatus.USED);
        assertThat(reloaded.getUsedOrderId()).isEqualTo(response.orderId());
    }

    @Test
    void createOrder_with_percent_coupon_caps_discount_at_max_discount_amount() {
        User user = createUser("coupon-order-percent@momentive.com");
        Product product = createProduct("사료", 100000, 5);
        Coupon coupon = createCoupon("PERCENT20", DiscountType.PERCENT, 20, 10000, 0);
        UserCoupon userCoupon = registerCoupon(user, coupon);

        OrderResponse response = orderService.createOrder(user.getId(),
                requestWithCoupon(product, 1, userCoupon.getId()));

        assertThat(response.itemsSubtotal()).isEqualTo(100000);
        assertThat(response.discountAmount()).isEqualTo(10000);
    }

    @Test
    void createOrder_with_coupon_discount_exceeding_items_subtotal_floors_at_zero() {
        User user = createUser("coupon-order-floor@momentive.com");
        Product product = createProduct("간식", 2000, 5);
        Coupon coupon = createCoupon("FIXED5000", DiscountType.FIXED, 5000, null, 0);
        UserCoupon userCoupon = registerCoupon(user, coupon);

        OrderResponse response = orderService.createOrder(user.getId(),
                requestWithCoupon(product, 1, userCoupon.getId()));

        assertThat(response.itemsSubtotal()).isEqualTo(2000);
        assertThat(response.discountAmount()).isEqualTo(2000);
        assertThat(response.totalAmount()).isEqualTo(0 + response.shippingFee());
    }

    @Test
    void createOrder_shipping_fee_is_based_on_pre_discount_items_subtotal() {
        User user = createUser("coupon-order-shipping@momentive.com");
        Product product = createProduct("사료", 70000, 5);
        Coupon coupon = createCoupon("FIXED10000", DiscountType.FIXED, 10000, null, 0);
        UserCoupon userCoupon = registerCoupon(user, coupon);

        OrderResponse response = orderService.createOrder(user.getId(),
                requestWithCoupon(product, 1, userCoupon.getId()));

        // 상품금액이 70,000원(무료배송 임계값)이므로 쿠폰 적용 후 60,000원이 되어도 배송비는 0이어야 한다.
        assertThat(response.itemsSubtotal()).isEqualTo(70000);
        assertThat(response.discountAmount()).isEqualTo(10000);
        assertThat(response.shippingFee()).isEqualTo(0);
        assertThat(response.totalAmount()).isEqualTo(60000);
    }

    @Test
    void createOrder_fails_when_user_coupon_not_owned_by_requester() {
        User owner = createUser("coupon-order-owner@momentive.com");
        User other = createUser("coupon-order-other@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        Coupon coupon = createCoupon("OWNERCHECK", DiscountType.FIXED, 1000, null, 0);
        UserCoupon userCoupon = registerCoupon(owner, coupon);

        assertThatThrownBy(() -> orderService.createOrder(other.getId(),
                requestWithCoupon(product, 1, userCoupon.getId())))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_COUPON_NOT_FOUND);
    }

    @Test
    void createOrder_fails_when_user_coupon_already_used() {
        User user = createUser("coupon-order-used@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        Coupon coupon = createCoupon("ALREADYUSED", DiscountType.FIXED, 1000, null, 0);
        UserCoupon userCoupon = registerCoupon(user, coupon);
        orderService.createOrder(user.getId(), requestWithCoupon(product, 1, userCoupon.getId()));

        assertThatThrownBy(() -> orderService.createOrder(user.getId(),
                requestWithCoupon(product, 1, userCoupon.getId())))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_COUPON_NOT_AVAILABLE);
    }

    @Test
    void createOrder_fails_when_user_coupon_expired() {
        User user = createUser("coupon-order-expired@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        Coupon coupon = createCoupon("EXPIREDCOUPON", DiscountType.FIXED, 1000, null, 0);
        UserCoupon userCoupon = registerCoupon(user, coupon);
        transactionTemplate.executeWithoutResult(status -> {
            Coupon managed = couponRepository.findById(coupon.getId()).orElseThrow();
            ReflectionTestUtils.setField(managed, "expiresAt", LocalDateTime.now().minusDays(1));
            couponRepository.saveAndFlush(managed);
        });

        assertThatThrownBy(() -> orderService.createOrder(user.getId(),
                requestWithCoupon(product, 1, userCoupon.getId())))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_COUPON_NOT_AVAILABLE);
    }

    @Test
    void createOrder_fails_when_items_subtotal_below_min_order_amount() {
        User user = createUser("coupon-order-minamount@momentive.com");
        Product product = createProduct("간식", 5000, 5);
        Coupon coupon = createCoupon("MIN30000", DiscountType.FIXED, 3000, null, 30000);
        UserCoupon userCoupon = registerCoupon(user, coupon);

        assertThatThrownBy(() -> orderService.createOrder(user.getId(),
                requestWithCoupon(product, 1, userCoupon.getId())))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
    }

    @Test
    void payment_failure_restores_coupon_to_available() {
        User user = createUser("coupon-restore-fail@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        Coupon coupon = createCoupon("RESTOREFAIL", DiscountType.FIXED, 1000, null, 0);
        UserCoupon userCoupon = registerCoupon(user, coupon);
        OrderResponse pending = orderService.createOrder(user.getId(),
                requestWithCoupon(product, 1, userCoupon.getId()));
        fakePaymentGatewayClient.forceFailure(true);

        assertThatThrownBy(() -> paymentService.confirmOrder(user.getId(), pending.orderId(),
                new OrderConfirmRequest("payKey", "toss-order", pending.totalAmount())))
                .isInstanceOf(CustomException.class);

        UserCoupon reloaded = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
        assertThat(reloaded.getUsedOrderId()).isNull();
    }

    @Test
    void pending_expiration_restores_coupon_to_available() {
        User user = createUser("coupon-restore-expire@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        Coupon coupon = createCoupon("RESTOREEXPIRE", DiscountType.FIXED, 1000, null, 0);
        UserCoupon userCoupon = registerCoupon(user, coupon);
        OrderResponse pending = orderService.createOrder(user.getId(),
                requestWithCoupon(product, 1, userCoupon.getId()));

        transactionTemplate.executeWithoutResult(status -> {
            Order order = orderRepository.findById(pending.orderId()).orElseThrow();
            ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now().minusHours(1));
        });

        orderExpirationService.expireOrder(pending.orderId());

        Order reloadedOrder = orderRepository.findById(pending.orderId()).orElseThrow();
        assertThat(reloadedOrder.getStatus()).isEqualTo(OrderStatus.FAILED);

        UserCoupon reloaded = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
        assertThat(reloaded.getUsedOrderId()).isNull();
    }

    @Test
    void paid_order_cancellation_restores_coupon_to_available() {
        User user = createUser("coupon-restore-cancel@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        Coupon coupon = createCoupon("RESTORECANCEL", DiscountType.FIXED, 1000, null, 0);
        UserCoupon userCoupon = registerCoupon(user, coupon);
        OrderResponse pending = orderService.createOrder(user.getId(),
                requestWithCoupon(product, 1, userCoupon.getId()));
        paymentService.confirmOrder(user.getId(), pending.orderId(),
                new OrderConfirmRequest("payKey", "toss-order", pending.totalAmount()));

        paymentService.cancelOrder(user.getId(), pending.orderId());

        UserCoupon reloaded = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
        assertThat(reloaded.getUsedOrderId()).isNull();
    }

    @Test
    void existing_order_without_coupon_still_reads_correctly_after_migration_backfill() {
        User user = createUser("coupon-no-coupon@momentive.com");
        Product product = createProduct("사료", 10000, 5);

        OrderResponse response = orderService.createOrder(user.getId(), requestWithCoupon(product, 1, null));

        assertThat(response.discountAmount()).isEqualTo(0);
        assertThat(response.couponName()).isNull();
    }
}
