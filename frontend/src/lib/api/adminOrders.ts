import { apiFetch } from "./client";
import type { AddressResponse } from "./addresses";
import type { OrderItemResponse, OrderStatus, OrderStatusResponse, ShippingStatus } from "./orders";

/**
 * 관리자 주문 API 클라이언트. `src/lib/api/admin.ts`(상품)와 동일한 패턴 —
 * 모든 호출은 공통 `apiFetch`를 거쳐 401 자동 refresh와 `ApiError` 변환을 그대로 물려받는다.
 */

export interface AdminOrderSummary {
  orderId: number;
  userEmail: string;
  userNickname: string;
  totalAmount: number;
  status: OrderStatus;
  /** 결제 완료 이전이거나 취소·실패한 주문은 null */
  shippingStatus: ShippingStatus | null;
  createdAt: string;
}

export interface AdminOrderListResponse {
  content: AdminOrderSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AdminOrderDetail {
  orderId: number;
  userEmail: string;
  userNickname: string;
  status: OrderStatus;
  itemsSubtotal: number;
  shippingFee: number;
  discountAmount: number;
  couponName: string | null;
  totalAmount: number;
  items: OrderItemResponse[];
  address: AddressResponse;
  shippingStatus: ShippingStatus | null;
  courier: string | null;
  trackingNumber: string | null;
  createdAt: string;
}

export interface AdminShippingUpdateRequest {
  shippingStatus: ShippingStatus;
  courier: string | null;
  trackingNumber: string | null;
}

export interface GetAdminOrdersOptions {
  page?: number;
  size?: number;
  /** 비어있으면 파라미터를 보내지 않는다 — 서버 기본값(PAID만)이 적용된다 */
  statuses?: OrderStatus[];
}

export function getAdminOrders({
  page = 0,
  size = 20,
  statuses,
}: GetAdminOrdersOptions = {}): Promise<AdminOrderListResponse> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  // 서버는 status를 콤마로 이어붙인 복수 값으로 받는다(전체보기는 4개 상태를 모두 나열).
  if (statuses && statuses.length > 0) params.set("status", statuses.join(","));

  return apiFetch<AdminOrderListResponse>(`/admin/orders?${params.toString()}`);
}

/** 존재하지 않는 주문ID는 `ORDER_NOT_FOUND` 404 */
export function getAdminOrder(orderId: number): Promise<AdminOrderDetail> {
  return apiFetch<AdminOrderDetail>(`/admin/orders/${orderId}`);
}

export function updateAdminOrderShipping(
  orderId: number,
  request: AdminShippingUpdateRequest,
): Promise<AdminOrderDetail> {
  return apiFetch<AdminOrderDetail>(`/admin/orders/${orderId}/shipping`, {
    method: "PATCH",
    body: JSON.stringify(request),
  });
}

/** `PAID`가 아닌 주문에 요청하면 `ORDER_NOT_CANCELLABLE` 409 */
export function cancelAdminOrder(orderId: number): Promise<OrderStatusResponse> {
  return apiFetch<OrderStatusResponse>(`/admin/orders/${orderId}/cancel`, {
    method: "POST",
  });
}
