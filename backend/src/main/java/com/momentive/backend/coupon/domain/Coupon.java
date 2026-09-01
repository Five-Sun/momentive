package com.momentive.backend.coupon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    @Column(nullable = false)
    private Integer discountValue;

    private Integer maxDiscountAmount;

    @Column(nullable = false)
    private Integer minOrderAmount;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Coupon(String code, String name, DiscountType discountType, Integer discountValue,
            Integer maxDiscountAmount, Integer minOrderAmount, LocalDateTime expiresAt) {
        this.code = code.toUpperCase();
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public static Coupon create(String code, String name, DiscountType discountType, Integer discountValue,
            Integer maxDiscountAmount, Integer minOrderAmount, LocalDateTime expiresAt) {
        return new Coupon(code, name, discountType, discountValue, maxDiscountAmount, minOrderAmount, expiresAt);
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean meetsMinOrderAmount(int itemsSubtotal) {
        return itemsSubtotal >= minOrderAmount;
    }
}
