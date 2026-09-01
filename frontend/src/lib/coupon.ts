/**
 * 쿠폰 할인액 계산 정책을 클라이언트에도 mirror한 것.
 * 기준: `backend/src/main/java/com/momentive/backend/coupon/domain/CouponDiscountPolicy.java`
 *
 * 체크아웃에서 쿠폰을 선택/해제할 때 서버 왕복 없이 금액 요약을 즉시 갱신하는 용도로만 사용한다.
 * 실제 결제 금액 확정은 항상 서버 응답(POST /orders의 OrderResponse.totalAmount)을 따르며,
 * 이 함수의 계산 결과를 결제/주문 요청에 실어 보내지 않는다.
 */

import type { UserCouponResponse } from "@/lib/api/coupon";

export function calculateCouponDiscount(coupon: UserCouponResponse, itemsSubtotal: number): number {
  const rawDiscount =
    coupon.discountType === "FIXED"
      ? coupon.discountValue
      : Math.floor((itemsSubtotal * coupon.discountValue) / 100);

  let discount = rawDiscount;
  if (coupon.discountType === "PERCENT" && coupon.maxDiscountAmount != null) {
    discount = Math.min(discount, coupon.maxDiscountAmount);
  }
  return Math.min(discount, itemsSubtotal);
}
