import { apiFetch } from "./client";
import type { AddressRequest, AddressResponse } from "./addresses";

export type OrderStatus = "PENDING" | "PAID" | "FAILED" | "CANCELLED";

export interface OrderItemRequest {
  productId: number;
  /** 재고 단위(사이즈) ID. 사이즈가 없는 상품도 단일 variant의 ID를 보낸다 (서버 `@NotNull`) */
  variantId: number;
  quantity: number;
}

export interface OrderItemResponse {
  productId: number;
  productName: string;
  /** 재고 단위 ID. variant 도입 이전에 생성된 주문 항목은 null */
  variantId: number | null;
  quantity: number;
  size: string | null;
  unitPrice: number;
}

export interface OrderCreateRequest {
  items: OrderItemRequest[];
  addressId?: number;
  address?: AddressRequest;
  userCouponId?: number;
}

export interface OrderResponse {
  orderId: number;
  status: OrderStatus;
  itemsSubtotal: number;
  shippingFee: number;
  discountAmount: number;
  couponName: string | null;
  totalAmount: number;
  items: OrderItemResponse[];
  address: AddressResponse;
  createdAt: string;
}

export interface OrderSummaryResponse {
  orderId: number;
  status: OrderStatus;
  totalAmount: number;
  createdAt: string;
  itemsSummary: string;
}

export interface OrderStatusResponse {
  orderId: number;
  status: OrderStatus;
}

export interface OrderConfirmRequest {
  paymentKey: string;
  orderId: string;
  amount: number;
}

export function createOrder(request: OrderCreateRequest): Promise<OrderResponse> {
  return apiFetch<OrderResponse>("/orders", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function confirmOrder(orderId: number, request: OrderConfirmRequest): Promise<OrderStatusResponse> {
  return apiFetch<OrderStatusResponse>(`/orders/${orderId}/confirm`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function getOrders(): Promise<OrderSummaryResponse[]> {
  return apiFetch<OrderSummaryResponse[]>("/orders");
}

export function getOrder(orderId: number): Promise<OrderResponse> {
  return apiFetch<OrderResponse>(`/orders/${orderId}`);
}

export function cancelOrder(orderId: number): Promise<OrderStatusResponse> {
  return apiFetch<OrderStatusResponse>(`/orders/${orderId}/cancel`, {
    method: "POST",
  });
}
