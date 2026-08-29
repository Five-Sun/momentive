package com.momentive.backend.order.domain;

import com.momentive.backend.product.domain.Product;
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

    @Column(nullable = false)
    private Integer quantity;

    private String size;

    @Column(nullable = false)
    private Integer unitPrice;

    OrderItem(Order order, Product product, Integer quantity, String size, Integer unitPrice) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.size = size;
        this.unitPrice = unitPrice;
    }

    public static OrderItem create(Order order, Product product, Integer quantity, String size, Integer unitPrice) {
        return new OrderItem(order, product, quantity, size, unitPrice);
    }

    public int getSubtotal() {
        return unitPrice * quantity;
    }
}
