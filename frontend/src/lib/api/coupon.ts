import { apiFetch } from "./client";

export type CouponDiscountType = "FIXED" | "PERCENT";
export type UserCouponStatus = "AVAILABLE" | "USED";

export interface CouponRegisterRequest {
  code: string;
}

export interface UserCouponResponse {
  id: number;
  couponName: string;
  discountType: CouponDiscountType;
  discountValue: number;
  maxDiscountAmount: number | null;
  minOrderAmount: number;
  expiresAt: string;
  status: UserCouponStatus;
  usedOrderId?: number | null;
}

export function registerCoupon(code: string): Promise<UserCouponResponse> {
  return apiFetch<UserCouponResponse>("/coupons/register", {
    method: "POST",
    body: JSON.stringify({ code } satisfies CouponRegisterRequest),
  });
}

export function fetchMyCoupons(): Promise<UserCouponResponse[]> {
  return apiFetch<UserCouponResponse[]>("/coupons");
}
