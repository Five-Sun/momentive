package com.momentive.backend.order.service;

import com.momentive.backend.address.domain.Address;
import com.momentive.backend.address.dto.AddressRequest;
import com.momentive.backend.address.repository.AddressRepository;
import com.momentive.backend.address.service.AddressService;
import com.momentive.backend.auth.domain.User;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.coupon.domain.CouponDiscountPolicy;
import com.momentive.backend.coupon.domain.UserCoupon;
import com.momentive.backend.coupon.repository.UserCouponRepository;
import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderItem;
import com.momentive.backend.order.domain.ShippingFeePolicy;
import com.momentive.backend.order.dto.OrderCreateRequest;
import com.momentive.backend.order.dto.OrderItemRequest;
import com.momentive.backend.order.dto.OrderResponse;
import com.momentive.backend.order.dto.OrderSummaryResponse;
import com.momentive.backend.order.repository.OrderRepository;
import com.momentive.backend.order.scheduler.OrderExpirationScheduler;
import com.momentive.backend.payment.service.OrderExpirationService;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.domain.ProductVariant;
import com.momentive.backend.product.repository.ProductVariantRepository;
import jakarta.persistence.OptimisticLockException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final int MAX_STOCK_RETRY = 2;
    private static final int MAX_COUPON_RETRY = 2;

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final UserCouponRepository userCouponRepository;
    private final AddressService addressService;
    private final OrderExpirationService orderExpirationService;

    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        User user = getUser(userId);
        Address address = resolveAddress(user, request);

        // 재고가 하나라도 부족하면 어떤 항목도 차감하지 않아야 하므로, 먼저 전체 항목의 재고 가용성을 검증한다.
        assertAllInStock(request.items());

        Order order = Order.createPending(user, address, 0);
        int itemsSubtotal = 0;
        for (OrderItemRequest itemRequest : request.items()) {
            ProductVariant variant = deductStockWithRetry(itemRequest);
            Product product = variant.getProduct();
            int unitPrice = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
            OrderItem item = OrderItem.create(order, product, variant, itemRequest.quantity(), unitPrice);
            order.addItem(item);
            itemsSubtotal += item.getSubtotal();
        }

        // 배송비는 반드시 할인 전 itemsSubtotal 기준으로 계산한다 — 무료배송 임계값이 할인으로 흔들리지 않게 하기 위함.
        int shippingFee = ShippingFeePolicy.calculate(itemsSubtotal, address.getZipcode());
        // 쿠폰의 usedOrderId를 채우려면 order.getId()가 필요하므로, 금액이 확정되지 않은 채로 먼저 저장해 ID를 확보한다.
        order.confirmAmounts(itemsSubtotal, 0, shippingFee);
        orderRepository.save(order);

        int discountAmount = applyCouponIfRequested(order, user, request.userCouponId(), itemsSubtotal);
        order.confirmAmounts(itemsSubtotal, discountAmount, shippingFee);

        return OrderResponse.from(order);
    }

    /**
     * userCouponId가 있으면 소유자 일치·상태·유효기간·최소 주문금액을 재검증한 뒤
     * 할인액을 계산하고 쿠폰을 선점(USED)한다. 검증 실패 시 400.
     *
     * <p>상태와 유효기간은 {@link UserCoupon#isAvailable()}이 함께 판정한다
     * (status가 AVAILABLE이면서 coupon.expiresAt이 지나지 않았을 것).
     *
     * <p>선점은 재고 차감과 같은 낙관적 락 + 재시도로 보호한다. 동시에 같은 쿠폰을 쓰려던
     * 요청 중 진 쪽은 재조회 시 이미 USED이므로 {@code USER_COUPON_NOT_AVAILABLE}로 실패한다.
     */
    private int applyCouponIfRequested(Order order, User user, Long userCouponId, int itemsSubtotal) {
        if (userCouponId == null) {
            return 0;
        }
        int retryCount = 0;
        while (true) {
            try {
                return claimCoupon(order, user, userCouponId, itemsSubtotal);
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                retryCount++;
                if (retryCount > MAX_COUPON_RETRY) {
                    throw new CustomException(ErrorCode.USER_COUPON_NOT_AVAILABLE);
                }
            }
        }
    }

    private int claimCoupon(Order order, User user, Long userCouponId, int itemsSubtotal) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_COUPON_NOT_FOUND));
        if (!userCoupon.isOwnedBy(user.getId())) {
            throw new CustomException(ErrorCode.USER_COUPON_NOT_FOUND);
        }
        if (!userCoupon.isAvailable()) {
            throw new CustomException(ErrorCode.USER_COUPON_NOT_AVAILABLE);
        }
        if (!userCoupon.getCoupon().meetsMinOrderAmount(itemsSubtotal)) {
            throw new CustomException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
        }

        int discountAmount = CouponDiscountPolicy.calculate(userCoupon.getCoupon(), itemsSubtotal);
        userCoupon.use(order.getId());
        // 낙관적 락 충돌을 이 지점에서 즉시 드러내기 위해 flush한다. 커밋까지 미루면
        // 재시도 없이 트랜잭션 전체가 실패한다.
        userCouponRepository.saveAndFlush(userCoupon);
        order.applyCoupon(userCoupon);
        return discountAmount;
    }

    private void assertAllInStock(List<OrderItemRequest> items) {
        Map<String, String> outOfStock = new LinkedHashMap<>();
        for (OrderItemRequest itemRequest : items) {
            ProductVariant variant = findVariant(itemRequest);
            if (!variant.hasEnoughStock(itemRequest.quantity())) {
                // 같은 상품의 서로 다른 사이즈가 동시에 부족할 수 있으므로 키는 variantId로 둔다.
                // (productId를 키로 쓰면 뒤 항목이 앞 항목의 안내 문구를 덮어써 정보가 사라진다.)
                outOfStock.put(String.valueOf(variant.getId()), outOfStockMessage(variant));
            }
        }
        if (!outOfStock.isEmpty()) {
            throw new CustomException(ErrorCode.OUT_OF_STOCK, outOfStock);
        }
    }

    private String outOfStockMessage(ProductVariant variant) {
        String name = variant.getProduct().getName();
        String label = variant.getSize() == null ? name : name + "(" + variant.getSize() + ")";
        return label + "의 재고가 부족합니다. (재고 " + variant.getStock() + "개)";
    }

    /**
     * 요청의 variantId로 재고 단위를 찾는다. 없는 variant이거나 요청한 상품에 속하지 않는
     * variant면 존재하지 않는 조합으로 보고 {@code PRODUCT_NOT_FOUND}로 거부한다
     * (다른 상품의 variant를 섞어 보내면 차감 대상과 결제 단가가 어긋난다).
     */
    private ProductVariant findVariant(OrderItemRequest itemRequest) {
        ProductVariant variant = productVariantRepository.findById(itemRequest.variantId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!variant.belongsTo(itemRequest.productId())) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return variant;
    }

    public List<OrderSummaryResponse> getOrders(Long userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = getOwnedOrder(userId, orderId);
        // 배치 스케줄러 주기 사이의 공백을 보완하기 위해 조회 시점에도 만료 대상이면 lazy하게 FAILED 처리한다.
        if (order.isExpiredPending(OrderExpirationScheduler.PENDING_TIMEOUT_MINUTES)) {
            orderExpirationService.expireOrder(orderId);
            order = getOwnedOrder(userId, orderId);
        }
        return OrderResponse.from(order);
    }

    private Order getOwnedOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return order;
    }

    private Address resolveAddress(User user, OrderCreateRequest request) {
        if (request.addressId() != null) {
            Address address = addressRepository.findById(request.addressId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ADDRESS_NOT_FOUND));
            if (!address.isOwnedBy(user.getId())) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
            return address;
        }
        if (request.address() != null) {
            // 주문서에서 새로 입력한 배송지는 spec 사용자 시나리오(2-3)에 따라 항상 기본배송지로 지정한다.
            return addressService.createAddressEntity(user, forceDefault(request.address()));
        }
        throw new CustomException(ErrorCode.VALIDATION_FAILED,
                Map.of("addressId", "배송지를 선택하거나 새로 입력해야 합니다."));
    }

    /**
     * 재고 검증 후 차감을 낙관적 락 하에 최초 시도 + 최대 2회까지 재시도한다(총 최대 3회 시도).
     * 재고 부족은 재시도로 해결되지 않으므로 즉시 OUT_OF_STOCK으로 실패시킨다.
     *
     * <p>락 단위가 {@link ProductVariant}이므로 같은 상품이라도 사이즈가 다르면 서로 충돌하지 않는다.
     */
    private ProductVariant deductStockWithRetry(OrderItemRequest itemRequest) {
        int retryCount = 0;
        while (true) {
            try {
                ProductVariant variant = findVariant(itemRequest);
                variant.deductStock(itemRequest.quantity());
                productVariantRepository.saveAndFlush(variant);
                return variant;
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                retryCount++;
                if (retryCount > MAX_STOCK_RETRY) {
                    throw new CustomException(ErrorCode.STOCK_CONFLICT);
                }
            }
        }
    }

    private AddressRequest forceDefault(AddressRequest request) {
        return new AddressRequest(
                request.recipient(), request.phone(), request.zipcode(), request.address1(), request.address2(), true);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));
    }
}
