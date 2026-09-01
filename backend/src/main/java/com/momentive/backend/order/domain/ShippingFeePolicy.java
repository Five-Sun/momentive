package com.momentive.backend.order.domain;

/**
 * 배송비 정책(2026-08-31 확정).
 * 기본 배송비 3,400원 / 상품금액 70,000원 이상 무료배송 / 제주(우편번호 63000~63644)는
 * 상품금액과 무관하게 4,000원이 항상 추가된다.
 */
public final class ShippingFeePolicy {

    private static final int BASE_FEE = 3_400;
    private static final int FREE_SHIPPING_THRESHOLD = 70_000;
    private static final int JEJU_SURCHARGE = 4_000;
    private static final int JEJU_ZIPCODE_MIN = 63000;
    private static final int JEJU_ZIPCODE_MAX = 63644;

    private ShippingFeePolicy() {
    }

    public static int calculate(int itemsSubtotal, String zipcode) {
        int baseFee = itemsSubtotal >= FREE_SHIPPING_THRESHOLD ? 0 : BASE_FEE;
        int surcharge = isJeju(zipcode) ? JEJU_SURCHARGE : 0;
        return baseFee + surcharge;
    }

    private static boolean isJeju(String zipcode) {
        if (zipcode == null) {
            return false;
        }
        try {
            int parsed = Integer.parseInt(zipcode.trim());
            return parsed >= JEJU_ZIPCODE_MIN && parsed <= JEJU_ZIPCODE_MAX;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
