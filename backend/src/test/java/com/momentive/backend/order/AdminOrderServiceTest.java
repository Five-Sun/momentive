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
import com.momentive.backend.coupon.repository.UserCouponRepository;
import com.momentive.backend.order.domain.OrderStatus;
import com.momentive.backend.order.domain.ShippingStatus;
import com.momentive.backend.order.dto.OrderConfirmRequest;
import com.momentive.backend.order.dto.OrderCreateRequest;
import com.momentive.backend.order.dto.OrderItemRequest;
import com.momentive.backend.order.dto.OrderResponse;
import com.momentive.backend.order.dto.OrderStatusResponse;
import com.momentive.backend.order.dto.admin.AdminOrderDetailResponse;
import com.momentive.backend.order.dto.admin.AdminOrderListResponse;
import com.momentive.backend.order.dto.admin.AdminShippingUpdateRequest;
import com.momentive.backend.order.repository.OrderRepository;
import com.momentive.backend.order.service.AdminOrderService;
import com.momentive.backend.order.service.OrderService;
import com.momentive.backend.payment.FakePaymentGatewayClient;
import com.momentive.backend.payment.service.PaymentService;
import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.repository.ProductRepository;
import com.momentive.backend.product.repository.ProductVariantRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * plan Phase 2 마지막 step: 관리자 주문 목록/상세 조회, 배송상태 변경(성공/실패), 관리자 대신 취소를 검증한다.
 * 취소는 {@link AdminOrderService}가 아니라 {@link PaymentService#cancelOrderAsAdmin}이 처리하므로
 * 함께 주입해 검증한다.
 */
@SpringBootTest
@Import(FakePaymentGatewayClient.Config.class)
class AdminOrderServiceTest {

    @Autowired
    private AdminOrderService adminOrderService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    // user_coupon이 users를 참조하므로, 남아 있으면 userRepository.deleteAll()이 FK 제약에 걸린다.
    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private FakePaymentGatewayClient fakePaymentGatewayClient;

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
        userCouponRepository.deleteAll();
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

    private Long variantIdOf(Product product) {
        return product.getVariants().get(0).getId();
    }

    private int stockOf(Product product) {
        return productVariantRepository.findById(variantIdOf(product)).orElseThrow().getStock();
    }

    private AddressRequest newAddressRequest() {
        return new AddressRequest("몽이", "010-1111-2222", "12345", "서울시 강남구", "101호", true);
    }

    private OrderResponse createPendingOrder(User user, Product product, int quantity) {
        return orderService.createOrder(user.getId(), new OrderCreateRequest(
                List.of(new OrderItemRequest(product.getId(), variantIdOf(product), quantity)),
                null, newAddressRequest(), null));
    }

    private OrderResponse createPaidOrder(User user, Product product, int quantity) {
        OrderResponse pending = createPendingOrder(user, product, quantity);
        paymentService.confirmOrder(user.getId(), pending.orderId(), new OrderConfirmRequest(
                "payKey-" + pending.orderId(), "toss-order-" + pending.orderId(), pending.totalAmount()));
        return pending;
    }

    @Test
    void getOrders_returns_only_paid_orders_by_default() {
        User user = createUser("admin-list-default@momentive.com");
        Product product = createProduct("사료", 10000, 10);
        OrderResponse paid = createPaidOrder(user, product, 1);
        createPendingOrder(user, product, 1); // PENDING인 채로 남겨 기본 필터에서 제외되는지 확인한다.

        AdminOrderListResponse response = adminOrderService.getOrders(0, 20, null);

        assertThat(response.content()).extracting("orderId").containsExactly(paid.orderId());
        assertThat(response.content().get(0).status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void getOrders_returns_all_statuses_when_specified_and_paginates() {
        User user = createUser("admin-list-all@momentive.com");
        Product product = createProduct("사료", 10000, 10);
        createPaidOrder(user, product, 1);
        createPendingOrder(user, product, 1);

        AdminOrderListResponse firstPage = adminOrderService.getOrders(
                0, 1, List.of(OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.FAILED, OrderStatus.CANCELLED));

        assertThat(firstPage.totalElements()).isEqualTo(2);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.content()).hasSize(1);
    }

    @Test
    void getOrder_returns_buyer_info_and_shipping_status_and_throws_not_found_for_unknown_id() {
        User user = createUser("admin-detail@momentive.com");
        Product product = createProduct("사료", 10000, 10);
        OrderResponse paid = createPaidOrder(user, product, 1);

        AdminOrderDetailResponse detail = adminOrderService.getOrder(paid.orderId());

        assertThat(detail.userEmail()).isEqualTo("admin-detail@momentive.com");
        assertThat(detail.userNickname()).isEqualTo("몽이");
        assertThat(detail.shippingStatus()).isEqualTo(ShippingStatus.PREPARING);

        assertThatThrownBy(() -> adminOrderService.getOrder(999999L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void updateShipping_to_shipping_without_courier_and_tracking_number_fails_and_keeps_status() {
        User user = createUser("admin-shipping-fail-1@momentive.com");
        Product product = createProduct("사료", 10000, 10);
        OrderResponse paid = createPaidOrder(user, product, 1);

        assertThatThrownBy(() -> adminOrderService.updateShipping(paid.orderId(),
                new AdminShippingUpdateRequest(ShippingStatus.SHIPPING, null, null)))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SHIPPING_INFO_REQUIRED);

        assertThat(adminOrderService.getOrder(paid.orderId()).shippingStatus()).isEqualTo(ShippingStatus.PREPARING);
    }

    @Test
    void updateShipping_to_delivered_without_courier_and_tracking_number_fails() {
        User user = createUser("admin-shipping-fail-2@momentive.com");
        Product product = createProduct("사료", 10000, 10);
        OrderResponse paid = createPaidOrder(user, product, 1);

        assertThatThrownBy(() -> adminOrderService.updateShipping(paid.orderId(),
                new AdminShippingUpdateRequest(ShippingStatus.DELIVERED, "CJ대한통운", " ")))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SHIPPING_INFO_REQUIRED);
    }

    @Test
    void updateShipping_on_non_paid_order_fails_with_not_applicable() {
        User user = createUser("admin-shipping-not-applicable@momentive.com");
        Product product = createProduct("사료", 10000, 10);
        OrderResponse pending = createPendingOrder(user, product, 1);

        assertThatThrownBy(() -> adminOrderService.updateShipping(pending.orderId(),
                new AdminShippingUpdateRequest(ShippingStatus.SHIPPING, "CJ대한통운", "123456789012")))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_SHIPPING_NOT_APPLICABLE);
    }

    @Test
    void updateShipping_allows_reverting_from_delivered_to_shipping_without_order_constraint() {
        User user = createUser("admin-shipping-revert@momentive.com");
        Product product = createProduct("사료", 10000, 10);
        OrderResponse paid = createPaidOrder(user, product, 1);
        adminOrderService.updateShipping(paid.orderId(),
                new AdminShippingUpdateRequest(ShippingStatus.DELIVERED, "CJ대한통운", "123456789012"));

        AdminOrderDetailResponse reverted = adminOrderService.updateShipping(paid.orderId(),
                new AdminShippingUpdateRequest(ShippingStatus.SHIPPING, "우체국택배", "987654321098"));

        assertThat(reverted.shippingStatus()).isEqualTo(ShippingStatus.SHIPPING);
        assertThat(reverted.courier()).isEqualTo("우체국택배");
        assertThat(reverted.trackingNumber()).isEqualTo("987654321098");
    }

    @Test
    void cancelOrderAsAdmin_success_restores_stock_like_customer_cancel_and_marks_cancelled() {
        User user = createUser("admin-cancel-success@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        OrderResponse paid = createPaidOrder(user, product, 2);

        OrderStatusResponse response = paymentService.cancelOrderAsAdmin(paid.orderId());

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stockOf(product)).isEqualTo(5);
    }

    @Test
    void cancelOrderAsAdmin_on_non_paid_order_fails_with_not_cancellable() {
        User user = createUser("admin-cancel-invalid@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        OrderResponse pending = createPendingOrder(user, product, 1);

        assertThatThrownBy(() -> paymentService.cancelOrderAsAdmin(pending.orderId()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_CANCELLABLE);
    }
}
