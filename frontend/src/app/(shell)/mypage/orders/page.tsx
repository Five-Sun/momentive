"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { Badge } from "@/components/core/Badge";
import { getOrders, type OrderStatus, type OrderSummaryResponse } from "@/lib/api/orders";

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

export default function MyOrdersPage() {
  const router = useRouter();
  const [orders, setOrders] = useState<OrderSummaryResponse[]>([]);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    getOrders()
      .then(setOrders)
      .finally(() => setLoaded(true));
  }, []);

  return (
    <div className="bg-canvas relative flex min-h-screen flex-col">
      <div className="border-hairline bg-surface-card flex h-13 flex-shrink-0 items-center px-4 border-b">
        <button onClick={() => router.back()} aria-label="뒤로가기" className="text-ink">
          <ArrowLeft className="h-5 w-5" />
        </button>
        <span className="text-title-sm text-ink flex-1 text-center">주문내역</span>
        <div className="h-5 w-5" />
      </div>

      {loaded && orders.length === 0 ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-2 px-4 py-20">
          <span className="text-body text-muted">주문내역이 없어요</span>
        </div>
      ) : (
        <div className="flex flex-col gap-3 p-4">
          {orders.map((order) => (
            <button
              key={order.orderId}
              onClick={() => router.push(`/mypage/orders/${order.orderId}`)}
              className="border-hairline bg-surface-card flex flex-col gap-2 rounded-md border p-3.5 text-left"
            >
              <div className="flex items-center justify-between">
                <span className="text-caption text-muted">{formatDateTime(order.createdAt)}</span>
                <Badge label={STATUS_LABEL[order.status]} tone={STATUS_TONE[order.status]} />
              </div>
              <span className="text-body-sm text-ink">{order.itemsSummary}</span>
              <span className="text-price text-ink">{formatWon(order.totalAmount)}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
