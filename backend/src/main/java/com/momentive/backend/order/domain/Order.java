package com.momentive.backend.order.domain;

import com.momentive.backend.address.domain.Address;
import com.momentive.backend.auth.domain.User;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.coupon.domain.UserCoupon;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private Integer totalAmount;

    @Column(nullable = false)
    private Integer shippingFee;

    @Column(nullable = false)
    private Integer itemsSubtotal;

    @Column(nullable = false)
    private Integer discountAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_coupon_id")
    private UserCoupon userCoupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    private String tossPaymentKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ShippingStatus shippingStatus;

    @Column(length = 50)
    private String courier;

    @Column(length = 100)
    private String trackingNumber;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    private Order(User user, Address address, Integer totalAmount) {
        this.user = user;
        this.address = address;
        this.totalAmount = totalAmount;
        this.shippingFee = 0;
        this.itemsSubtotal = 0;
        this.discountAmount = 0;
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Order createPending(User user, Address address, Integer totalAmount) {
        return new Order(user, address, totalAmount);
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public void applyCoupon(UserCoupon userCoupon) {
        this.userCoupon = userCoupon;
    }

    /**
     * 주문 생성 중 항목별 재고 차감이 끝난 뒤 상품금액·할인액·배송비를 확정하고,
     * 총 결제금액(totalAmount)을 {@code itemsSubtotal - discountAmount + shippingFee}로 계산해 저장한다.
     */
    public void confirmAmounts(int itemsSubtotal, int discountAmount, int shippingFee) {
        this.itemsSubtotal = itemsSubtotal;
        this.discountAmount = discountAmount;
        this.shippingFee = shippingFee;
        this.totalAmount = itemsSubtotal - discountAmount + shippingFee;
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public boolean isPending() {
        return this.status == OrderStatus.PENDING;
    }

    public boolean isPaid() {
        return this.status == OrderStatus.PAID;
    }

    public boolean isCancellable() {
        return this.status == OrderStatus.PAID;
    }

    /**
     * PENDING 상태로 생성된 지 {@code timeoutMinutes}분이 지났으면 만료 대상으로 본다.
     * 스케줄러의 배치 실행 및 조회 시점 lazy 체크에서 공통으로 사용한다.
     */
    public boolean isExpiredPending(long timeoutMinutes) {
        return isPending() && createdAt.isBefore(LocalDateTime.now().minusMinutes(timeoutMinutes));
    }

    public void markAsPaid(String tossPaymentKey) {
        this.status = OrderStatus.PAID;
        this.tossPaymentKey = tossPaymentKey;
        this.shippingStatus = ShippingStatus.PREPARING;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = OrderStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCancelled() {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 관리자가 배송상태·택배사·송장번호를 갱신한다. 결제 완료(PAID) 주문에만 허용되며,
     * SHIPPING/DELIVERED로 전환하려면 택배사·송장번호가 모두 있어야 한다. PREPARING으로
     * 되돌릴 때는 인자로 넘어온 courier/trackingNumber를 무시하고 기존 값을 그대로 유지한다
     * (재입력 없이 다시 SHIPPING으로 돌아갈 수 있게). 상태 전이 순서 제약은 없다.
     */
    public void updateShipping(ShippingStatus newStatus, String courier, String trackingNumber) {
        if (this.status != OrderStatus.PAID) {
            throw new CustomException(ErrorCode.ORDER_SHIPPING_NOT_APPLICABLE);
        }
        if (newStatus == ShippingStatus.PREPARING) {
            this.shippingStatus = ShippingStatus.PREPARING;
            this.updatedAt = LocalDateTime.now();
            return;
        }
        if (courier == null || courier.isBlank() || trackingNumber == null || trackingNumber.isBlank()) {
            throw new CustomException(ErrorCode.SHIPPING_INFO_REQUIRED);
        }
        this.shippingStatus = newStatus;
        this.courier = courier;
        this.trackingNumber = trackingNumber;
        this.updatedAt = LocalDateTime.now();
    }
}
