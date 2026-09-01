package com.momentive.backend.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CouponDiscountPolicyTest {

    private Coupon fixedCoupon(int discountValue, int minOrderAmount) {
        return Coupon.create("FIXED-TEST", "정액 쿠폰", DiscountType.FIXED, discountValue, null, minOrderAmount,
                LocalDateTime.now().plusDays(1));
    }

    private Coupon percentCoupon(int percent, Integer maxDiscountAmount, int minOrderAmount) {
        return Coupon.create("PERCENT-TEST", "정률 쿠폰", DiscountType.PERCENT, percent, maxDiscountAmount,
                minOrderAmount, LocalDateTime.now().plusDays(1));
    }

    @Test
    void fixed_discount_applies_full_value_when_below_items_subtotal() {
        Coupon coupon = fixedCoupon(3000, 0);

        int discount = CouponDiscountPolicy.calculate(coupon, 30000);

        assertThat(discount).isEqualTo(3000);
    }

    @Test
    void fixed_discount_is_capped_at_items_subtotal_when_discount_value_exceeds_it() {
        Coupon coupon = fixedCoupon(5000, 0);

        int discount = CouponDiscountPolicy.calculate(coupon, 3000);

        assertThat(discount).isEqualTo(3000);
    }

    @Test
    void percent_discount_applies_ratio_when_below_max_discount_amount() {
        Coupon coupon = percentCoupon(10, 5000, 0);

        int discount = CouponDiscountPolicy.calculate(coupon, 20000);

        assertThat(discount).isEqualTo(2000);
    }

    @Test
    void percent_discount_is_capped_at_max_discount_amount() {
        Coupon coupon = percentCoupon(20, 10000, 0);

        int discount = CouponDiscountPolicy.calculate(coupon, 100000);

        assertThat(discount).isEqualTo(10000);
    }

    @Test
    void percent_discount_is_also_capped_at_items_subtotal() {
        Coupon coupon = percentCoupon(50, 100000, 0);

        int discount = CouponDiscountPolicy.calculate(coupon, 1000);

        assertThat(discount).isEqualTo(500);
    }
}
