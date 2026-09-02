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
        // 정률 계산은 int 곱셈이 오버플로하면 할인액이 음수가 되고, 그 경우 결제금액이 상품금액보다
        // 커지는 역전이 발생하므로 long으로 계산한 뒤 상한을 적용하고 마지막에 좁힌다.
        long discount = switch (coupon.getDiscountType()) {
            case FIXED -> coupon.getDiscountValue();
            case PERCENT -> (long) itemsSubtotal * coupon.getDiscountValue() / 100;
        };

        if (coupon.getDiscountType() == DiscountType.PERCENT && coupon.getMaxDiscountAmount() != null) {
            discount = Math.min(discount, coupon.getMaxDiscountAmount());
        }
        return (int) Math.min(discount, itemsSubtotal);
    }
}
