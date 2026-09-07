package com.momentive.backend.order.domain;

import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.domain.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * 재고를 차감한 variant. variant 도입(V16) 이전에 생성된 주문 행은 {@code null}이다 —
     * 과거 주문의 size 문자열이 어느 variant인지 정할 방법이 없어 소급 매핑은 하지 않는다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(nullable = false)
    private Integer quantity;

    /** 주문 시점의 사이즈 스냅샷. variant가 나중에 삭제·변경돼도 주문 이력은 그대로 남는다. */
    private String size;

    @Column(nullable = false)
    private Integer unitPrice;

    OrderItem(Order order, Product product, ProductVariant variant, Integer quantity, String size, Integer unitPrice) {
        this.order = order;
        this.product = product;
        this.variant = variant;
        this.quantity = quantity;
        this.size = size;
        this.unitPrice = unitPrice;
    }

    /**
     * 신규 주문 항목을 만든다. size는 variant의 값을 그대로 복사해 스냅샷으로 보존한다.
     */
    public static OrderItem create(Order order, Product product, ProductVariant variant, Integer quantity,
                                   Integer unitPrice) {
        return new OrderItem(order, product, variant, quantity, variant.getSize(), unitPrice);
    }

    public int getSubtotal() {
        return unitPrice * quantity;
    }
}
