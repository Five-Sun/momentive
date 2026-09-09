"use client";

import Link from "next/link";
import { Badge } from "@/components/core/Badge";
import { SHIPPING_STATUS_LABEL, SHIPPING_STATUS_TONE } from "@/lib/shippingStatus";
import type { OrderStatus } from "@/lib/api/orders";
import type { AdminOrderSummary } from "@/lib/api/adminOrders";

/**
 * 관리자 주문 목록 표. `AdminProductTable.tsx`와 동일하게 `/admin` 안의 로컬 표 컴포넌트로 둔다
 * (표는 고객 화면에 없는 패턴이라 공용 `src/components/`로 올리지 않는다).
 */

const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "결제대기",
  PAID: "결제완료",
  FAILED: "결제실패",
  CANCELLED: "취소완료",
};

const STATUS_TONE: Record<OrderStatus, "new" | "sale" | "soldout" | "neutral"> = {
  PENDING: "neutral",
  PAID: "new",
  FAILED: "sale",
  CANCELLED: "soldout",
};

function formatWon(amount: number) {
  return `${amount.toLocaleString("ko-KR")}원`;
}

function formatDateTime(iso: string) {
  const date = new Date(iso);
  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

interface AdminOrderTableProps {
  orders: AdminOrderSummary[];
}

export function AdminOrderTable({ orders }: AdminOrderTableProps) {
  return (
    <div className="border-hairline bg-surface-card overflow-x-auto rounded-md border">
      <table className="w-full min-w-[860px] border-collapse">
        <thead>
          <tr className="border-hairline bg-surface-soft border-b">
            <th className="text-caption text-muted w-20 px-4 py-3 text-left">주문ID</th>
            <th className="text-caption text-muted px-4 py-3 text-left">주문자</th>
            <th className="text-caption text-muted w-32 px-4 py-3 text-right">총액</th>
            <th className="text-caption text-muted w-24 px-4 py-3 text-left">주문상태</th>
            <th className="text-caption text-muted w-24 px-4 py-3 text-left">배송상태</th>
            <th className="text-caption text-muted w-40 px-4 py-3 text-left">생성일시</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => (
            <tr key={order.orderId} className="border-hairline-soft border-b last:border-b-0">
              <td className="px-4 py-3">
                <Link
                  href={`/admin/orders/${order.orderId}`}
                  className="text-body-sm text-ink font-semibold underline"
                >
                  #{order.orderId}
                </Link>
              </td>
              <td className="text-body-sm text-body px-4 py-3">
                <span className="text-ink">{order.userNickname}</span>
                <span className="text-muted"> · {order.userEmail}</span>
              </td>
              <td className="text-body-sm text-ink px-4 py-3 text-right">
                {formatWon(order.totalAmount)}
              </td>
              <td className="px-4 py-3">
                <Badge label={STATUS_LABEL[order.status]} tone={STATUS_TONE[order.status]} />
              </td>
              <td className="px-4 py-3">
                {order.shippingStatus ? (
                  <Badge
                    label={SHIPPING_STATUS_LABEL[order.shippingStatus]}
                    tone={SHIPPING_STATUS_TONE[order.shippingStatus]}
                  />
                ) : (
                  <span className="text-body-sm text-muted">-</span>
                )}
              </td>
              <td className="text-body-sm text-body px-4 py-3">{formatDateTime(order.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
