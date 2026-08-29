package com.momentive.backend.order.domain;

import com.momentive.backend.address.domain.Address;
import com.momentive.backend.auth.domain.User;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    private String tossPaymentKey;

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

    /**
     * 주문 생성 중 항목별 재고 차감이 끝난 뒤 실제 합계 금액을 확정한다.
     */
    public void confirmTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
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
}
