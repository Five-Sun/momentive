package com.momentive.backend.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.momentive.backend.address.domain.Address;
import com.momentive.backend.address.repository.AddressRepository;
import com.momentive.backend.address.service.AddressService;
import com.momentive.backend.auth.domain.User;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.coupon.repository.UserCouponRepository;
import com.momentive.backend.order.dto.OrderCreateRequest;
import com.momentive.backend.order.dto.OrderItemRequest;
import com.momentive.backend.order.dto.OrderResponse;
import com.momentive.backend.order.repository.OrderRepository;
import com.momentive.backend.order.service.OrderService;
import com.momentive.backend.payment.service.OrderExpirationService;
import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.domain.ProductVariant;
import com.momentive.backend.product.repository.ProductVariantRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@code OrderService.deductStockWithRetry}의 낙관적 락 재시도 횟수 경계값을 검증한다.
 * (docs/backlog/2026-08-29-cart-order-payment-phase1-01.md)
 * "최초 시도 + 최대 2회 재시도(총 최대 3회 시도)" 후에도 충돌하면 STOCK_CONFLICT여야 하고,
 * 2번째 재시도(3번째 시도)에서 성공하면 정상 처리돼야 한다.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceStockRetryTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserCouponRepository userCouponRepository;
    @Mock
    private AddressService addressService;
    @Mock
    private OrderExpirationService orderExpirationService;

    private OrderService orderService;

    private User user;
    private Address address;
    private Product product;
    private ProductVariant variant;

    private void setUp() {
        orderService = new OrderService(
                orderRepository, productVariantRepository, addressRepository, userRepository, userCouponRepository,
                addressService, orderExpirationService);

        user = User.createUser("retry@momentive.com", "hash", "재시도");
        ReflectionTestUtils.setField(user, "id", 1L);

        address = Address.create(user, "재시도", "010-0000-0000", "12345", "서울시", null, true);
        ReflectionTestUtils.setField(address, "id", 10L);

        product = new Product("사료", "desc", 10000, null, Category.ACCESSORY);
        ReflectionTestUtils.setField(product, "id", 100L);
        variant = product.addVariant(null, 5);
        ReflectionTestUtils.setField(variant, "id", 200L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
        when(productVariantRepository.findById(200L)).thenReturn(Optional.of(variant));
    }

    private OrderCreateRequest newRequest() {
        return new OrderCreateRequest(java.util.List.of(new OrderItemRequest(100L, 200L, 1)), 10L, null, null);
    }

    @Test
    void succeeds_on_third_attempt_after_two_optimistic_lock_failures() {
        setUp();
        doThrow(new ObjectOptimisticLockingFailureException(ProductVariant.class, 200L))
                .doThrow(new ObjectOptimisticLockingFailureException(ProductVariant.class, 200L))
                .doAnswer(invocation -> invocation.getArgument(0))
                .when(productVariantRepository).saveAndFlush(any());

        OrderResponse response = orderService.createOrder(1L, newRequest());

        assertThat(response.status().name()).isEqualTo("PENDING");
    }

    @Test
    void fails_with_stock_conflict_after_exhausting_initial_attempt_plus_two_retries() {
        setUp();
        doThrow(new ObjectOptimisticLockingFailureException(ProductVariant.class, 200L))
                .when(productVariantRepository).saveAndFlush(any());

        assertThatThrownBy(() -> orderService.createOrder(1L, newRequest()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STOCK_CONFLICT);

        // 최초 1회 + 재시도 2회 = 총 3회 시도 (docs/backlog/2026-08-29-cart-order-payment-phase1-01.md)
        org.mockito.Mockito.verify(productVariantRepository, org.mockito.Mockito.times(3)).saveAndFlush(any());
    }
}
