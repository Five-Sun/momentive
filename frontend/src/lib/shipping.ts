/**
 * 배송비 정책(2026-08-31 확정)을 클라이언트에도 mirror한 것.
 * 기준: `backend/src/main/java/com/momentive/backend/order/domain/ShippingFeePolicy.java`
 *
 * 체크아웃에서 배송지를 고르거나 새로 입력할 때 서버 왕복 없이 즉시 미리보기를 보여주는
 * 용도로만 사용한다. 실제 결제 금액 확정은 항상 서버 응답(POST /orders의
 * OrderResponse.totalAmount)을 따르며, 이 함수의 계산 결과를 결제/주문 요청에 실어 보내지 않는다.
 */

export const SHIPPING_BASE_FEE = 3_400;
export const FREE_SHIPPING_THRESHOLD = 70_000;
export const JEJU_SURCHARGE = 4_000;

const JEJU_ZIPCODE_MIN = 63000;
const JEJU_ZIPCODE_MAX = 63644;

export function calculateShippingFee(itemsSubtotal: number, zipcode: string | null | undefined): number {
  const baseFee = itemsSubtotal >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_BASE_FEE;
  const surcharge = isJeju(zipcode) ? JEJU_SURCHARGE : 0;
  return baseFee + surcharge;
}

function isJeju(zipcode: string | null | undefined): boolean {
  if (!zipcode) return false;
  const parsed = Number.parseInt(zipcode.trim(), 10);
  if (Number.isNaN(parsed)) return false;
  return parsed >= JEJU_ZIPCODE_MIN && parsed <= JEJU_ZIPCODE_MAX;
}
