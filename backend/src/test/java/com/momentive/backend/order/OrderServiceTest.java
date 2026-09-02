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
import com.momentive.backend.order.dto.OrderCreateRequest;
import com.momentive.backend.order.dto.OrderItemRequest;
import com.momentive.backend.order.dto.OrderResponse;
import com.momentive.backend.order.repository.OrderRepository;
import com.momentive.backend.order.service.OrderService;
import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.repository.ProductRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

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

    // user_coupon이 users를 참조하므로, 남아 있으면 userRepository.deleteAll()이 FK 제약에 걸린다.
    @Autowired
    private UserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        addressRepository.deleteAll();
        productRepository.deleteAll();
        userCouponRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
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

    private Product createProduct(String name, int price, int stock) {
        return productRepository.save(new Product(name, "desc", price, null, false, Category.ACCESSORY, stock));
    }

    private AddressRequest newAddressRequest() {
        return new AddressRequest("몽이", "010-1111-2222", "12345", "서울시 강남구", "101호", true);
    }

    @Test
    void createOrder_deducts_stock_and_creates_pending_order_with_new_address() {
        User user = createUser("order1@momentive.com");
        Product product = createProduct("사료", 10000, 5);

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderItemRequest(product.getId(), 2, "M")),
                null,
                newAddressRequest(),
                null
        );

        OrderResponse response = orderService.createOrder(user.getId(), request);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.itemsSubtotal()).isEqualTo(20000);
        assertThat(response.shippingFee()).isEqualTo(3400);
        assertThat(response.totalAmount()).isEqualTo(23400);
        assertThat(response.items()).hasSize(1);
        assertThat(response.address().isDefault()).isTrue();

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getStock()).isEqualTo(3);
    }

    @Test
    void createOrder_applies_free_shipping_when_items_subtotal_meets_threshold() {
        User user = createUser("order-freeship@momentive.com");
        Product product = createProduct("사료", 70000, 5);

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderItemRequest(product.getId(), 1, null)),
                null,
                newAddressRequest(),
                null
        );

        OrderResponse response = orderService.createOrder(user.getId(), request);

        assertThat(response.itemsSubtotal()).isEqualTo(70000);
        assertThat(response.shippingFee()).isEqualTo(0);
        assertThat(response.totalAmount()).isEqualTo(70000);
    }

    @Test
    void createOrder_applies_jeju_surcharge_regardless_of_items_subtotal() {
        User user = createUser("order-jeju@momentive.com");
        Product product = createProduct("사료", 70000, 5);

        AddressRequest jejuAddress =
                new AddressRequest("몽이", "010-1111-2222", "63000", "제주시", "101호", true);
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderItemRequest(product.getId(), 1, null)),
                null,
                jejuAddress,
                null
        );

        OrderResponse response = orderService.createOrder(user.getId(), request);

        assertThat(response.itemsSubtotal()).isEqualTo(70000);
        assertThat(response.shippingFee()).isEqualTo(4000);
        assertThat(response.totalAmount()).isEqualTo(74000);
    }

    @Test
    void createOrder_fails_with_out_of_stock_and_does_not_change_stock() {
        User user = createUser("order2@momentive.com");
        Product product = createProduct("사료", 10000, 1);

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderItemRequest(product.getId(), 2, null)),
                null,
                newAddressRequest(),
                null
        );

        assertThatThrownBy(() -> orderService.createOrder(user.getId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.OUT_OF_STOCK);

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getStock()).isEqualTo(1);
        assertThat(orderRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())).isEmpty();
    }

    @Test
    void createOrder_with_existing_address_reuses_saved_address() {
        User user = createUser("order3@momentive.com");
        Product product = createProduct("사료", 10000, 5);
        Long addressId = orderService.createOrder(user.getId(), new OrderCreateRequest(
                List.of(new OrderItemRequest(product.getId(), 1, null)), null, newAddressRequest(), null
        )).address().id();

        Product product2 = createProduct("간식", 5000, 5);
        OrderResponse response = orderService.createOrder(user.getId(), new OrderCreateRequest(
                List.of(new OrderItemRequest(product2.getId(), 1, null)), addressId, null, null
        ));

        assertThat(response.address().id()).isEqualTo(addressId);
    }

    @Test
    void createOrder_fails_when_neither_addressId_nor_address_given() {
        User user = createUser("order4@momentive.com");
        Product product = createProduct("사료", 10000, 5);

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderItemRequest(product.getId(), 1, null)), null, null, null
        );

        assertThatThrownBy(() -> orderService.createOrder(user.getId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void getOrder_fails_with_forbidden_for_other_users_order() {
        User owner = createUser("owner2@momentive.com");
        User other = createUser("other2@momentive.com");
        Product product = createProduct("사료", 10000, 5);

        OrderResponse order = orderService.createOrder(owner.getId(), new OrderCreateRequest(
                List.of(new OrderItemRequest(product.getId(), 1, null)), null, newAddressRequest(), null
        ));

        assertThatThrownBy(() -> orderService.getOrder(other.getId(), order.orderId()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void getOrder_fails_with_not_found_for_unknown_order() {
        User user = createUser("order5@momentive.com");

        assertThatThrownBy(() -> orderService.getOrder(user.getId(), 999999L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void concurrent_orders_on_same_product_only_one_succeeds() throws InterruptedException {
        User userA = createUser("concurrentA@momentive.com");
        User userB = createUser("concurrentB@momentive.com");
        Product product = createProduct("한정판 사료", 10000, 1);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        Runnable task = () -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                orderService.createOrder(userA.getId(), new OrderCreateRequest(
                        List.of(new OrderItemRequest(product.getId(), 1, null)), null, newAddressRequest(), null));
                successCount.incrementAndGet();
            } catch (CustomException e) {
                failureCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        executor.submit(task);
        executor.submit(() -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                orderService.createOrder(userB.getId(), new OrderCreateRequest(
                        List.of(new OrderItemRequest(product.getId(), 1, null)), null, newAddressRequest(), null));
                successCount.incrementAndGet();
            } catch (CustomException e) {
                failureCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        readyLatch.await();
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getStock()).isEqualTo(0);
    }
}
