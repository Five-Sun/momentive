import type { ProductStatus } from "@/lib/api/admin";

/** 관리자 화면에서만 쓰는 판매 상태 표기. 고객 화면에는 status 자체가 노출되지 않는다. */
export const PRODUCT_STATUS_LABEL: Record<ProductStatus, string> = {
  ON_SALE: "판매중",
  HIDDEN: "숨김",
  DELETED: "삭제됨",
};

export const PRODUCT_STATUS_TONE: Record<ProductStatus, "new" | "neutral" | "soldout"> = {
  ON_SALE: "new",
  HIDDEN: "neutral",
  DELETED: "soldout",
};

/** 폼의 상태 선택지. `DELETED`도 포함해, 삭제된 상품을 다시 판매중으로 되돌릴 수 있게 한다. */
export const PRODUCT_STATUS_VALUES = ["ON_SALE", "HIDDEN", "DELETED"] as const satisfies readonly ProductStatus[];

/** 목록 상태 필터의 선택지. 값은 서버로 보낼 status 조합이다. */
export const STATUS_FILTER_OPTIONS: { key: string; label: string; statuses: ProductStatus[] }[] = [
  { key: "default", label: "판매중 · 숨김", statuses: ["ON_SALE", "HIDDEN"] },
  { key: "on_sale", label: "판매중", statuses: ["ON_SALE"] },
  { key: "hidden", label: "숨김", statuses: ["HIDDEN"] },
  { key: "deleted", label: "삭제됨", statuses: ["DELETED"] },
];
