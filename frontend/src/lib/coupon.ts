/**
 * 쿠폰 할인액 계산 정책을 클라이언트에도 mirror한 것.
 * 기준: `backend/src/main/java/com/momentive/backend/coupon/domain/CouponDiscountPolicy.java`
 *
 * 체크아웃에서 쿠폰을 선택/해제할 때 서버 왕복 없이 금액 요약을 즉시 갱신하는 용도로만 사용한다.
 * 실제 결제 금액 확정은 항상 서버 응답(POST /orders의 OrderResponse.totalAmount)을 따르며,
 * 이 함수의 계산 결과를 결제/주문 요청에 실어 보내지 않는다.
 */

import type { UserCouponResponse } from "@/lib/api/coupon";

/** 서비스 기준 시간대. 백엔드도 같은 값으로 JVM 기본 시간대를 고정한다. */
const SERVICE_TIME_ZONE = "Asia/Seoul";
const SERVICE_UTC_OFFSET = "+09:00";

/**
 * 백엔드는 `LocalDateTime`을 오프셋 없이 직렬화한다(`2026-12-31T23:59:59`).
 * 이를 `new Date()`로 그대로 넘기면 **브라우저 로컬** 시간으로 해석되어,
 * 서버가 KST로 판정하는 만료 시각과 최대 하루 가까이 어긋난다.
 * 오프셋이 없으면 서비스 기준 시간대를 붙여 해석을 고정한다.
 */
export function parseServerDateTime(value: string): Date {
  const hasZone = /(?:Z|[+-]\d{2}:?\d{2})$/.test(value);
  return new Date(hasZone ? value : `${value}${SERVICE_UTC_OFFSET}`);
}

/** 쿠폰 만료 여부. 서버 `Coupon.isExpired()`와 같은 기준으로 판정한다. */
export function isCouponExpired(coupon: UserCouponResponse): boolean {
  return parseServerDateTime(coupon.expiresAt).getTime() < Date.now();
}

/** 유효기간 표시. 보는 사람의 위치와 무관하게 정책상의 날짜를 그대로 보여준다. */
export function formatExpiresAt(expiresAt: string): string {
  const formatted = parseServerDateTime(expiresAt).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    timeZone: SERVICE_TIME_ZONE,
  });
  return `${formatted}까지`;
}

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
