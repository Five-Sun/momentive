import type { ShippingStatus } from "@/lib/api/orders";

/** 관리자·고객 화면이 공용으로 쓰는 배송상태 라벨/뱃지 톤. `src/lib/categories.ts`와 동일한 위치 컨벤션. */
export const SHIPPING_STATUS_LABEL: Record<ShippingStatus, string> = {
  PREPARING: "배송준비중",
  SHIPPING: "배송중",
  DELIVERED: "배송완료",
};

export const SHIPPING_STATUS_TONE: Record<ShippingStatus, "new" | "sale" | "soldout" | "neutral"> = {
  PREPARING: "neutral",
  SHIPPING: "new",
  DELIVERED: "sale",
};

export const SHIPPING_STATUS_VALUES = [
  "PREPARING",
  "SHIPPING",
  "DELIVERED",
] as const satisfies readonly ShippingStatus[];
