package com.momentive.backend.coupon.domain;

/**
 * 쿠폰 할인액 계산 정책.
 * {@code FIXED}는 정액을 그대로 적용하되 상품금액을 넘지 않도록 방어하고,
 * {@code PERCENT}는 정률 계산 결과를 {@code maxDiscountAmount}와 상품금액 양쪽으로 상한을 둔다.
 */
public final class CouponDiscountPolicy {

    private CouponDiscountPolicy() {
    }

    public static int calculate(Coupon coupon, int itemsSubtotal) {
        int rawDiscount = switch (coupon.getDiscountType()) {
            case FIXED -> coupon.getDiscountValue();
            case PERCENT -> itemsSubtotal * coupon.getDiscountValue() / 100;
        };

        int discount = rawDiscount;
        if (coupon.getDiscountType() == DiscountType.PERCENT && coupon.getMaxDiscountAmount() != null) {
            discount = Math.min(discount, coupon.getMaxDiscountAmount());
        }
        return Math.min(discount, itemsSubtotal);
    }
}
