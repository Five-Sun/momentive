"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/core/Button";
import { Badge } from "@/components/core/Badge";
import { Toast } from "@/components/feedback/Toast";
import { cancelOrder, getOrder, type OrderResponse, type OrderStatus } from "@/lib/api/orders";
import { ApiError } from "@/lib/api/client";

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

export default function OrderDetailPage() {
  const router = useRouter();
  const params = useParams<{ orderId: string }>();
  const orderId = Number(params.orderId);

  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const loadOrder = useCallback(() => {
    getOrder(orderId)
      .then(setOrder)
      .finally(() => setLoaded(true));
  }, [orderId]);

  useEffect(() => {
    loadOrder();
  }, [loadOrder]);

  function showToast(message: string) {
    setToastMessage(message);
    setTimeout(() => setToastMessage(null), 1800);
  }

  async function handleCancel() {
    if (!order) return;
    setCancelling(true);
    try {
      await cancelOrder(order.orderId);
      setOrder({ ...order, status: "CANCELLED" });
    } catch (err) {
      if (err instanceof ApiError && err.errorCode === "ORDER_NOT_CANCELLABLE") {
        showToast("이미 취소할 수 없는 주문이에요");
        loadOrder();
        return;
      }
      showToast("주문 취소에 실패했어요. 잠시 후 다시 시도해주세요");
    } finally {
      setCancelling(false);
    }
  }

  if (!loaded) {
    return (
      <div className="bg-canvas flex min-h-screen flex-col">
        <div className="border-hairline bg-surface-card flex h-13 flex-shrink-0 items-center px-4 border-b">
          <button onClick={() => router.back()} aria-label="뒤로가기" className="text-ink">
            <ArrowLeft className="h-5 w-5" />
          </button>
          <span className="text-title-sm text-ink flex-1 text-center">주문 상세</span>
          <div className="h-5 w-5" />
        </div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="bg-canvas flex min-h-screen flex-col items-center justify-center gap-2 px-4">
        <span className="text-body text-muted">주문을 찾을 수 없어요</span>
        <Button variant="secondary" onClick={() => router.push("/mypage/orders")}>
          주문내역으로 돌아가기
        </Button>
      </div>
    );
  }

  // 상태별 취소 버튼 노출은 PAID일 때만 true이고 나머지(PENDING/FAILED/CANCELLED)는 모두 false로
  // 배타적으로 조건화한다. (docs/backlog/2026-08-26-app-redesign-phase2-01.md 참고)
  const canCancel = order.status === "PAID";

  return (
    <div className="bg-canvas relative flex min-h-screen flex-col">
      <div className="border-hairline bg-surface-card flex h-13 flex-shrink-0 items-center px-4 border-b">
        <button onClick={() => router.back()} aria-label="뒤로가기" className="text-ink">
          <ArrowLeft className="h-5 w-5" />
        </button>
        <span className="text-title-sm text-ink flex-1 text-center">주문 상세</span>
        <div className="h-5 w-5" />
      </div>

      <div className="flex flex-col gap-6 px-4 py-5 pb-28">
        <section className="flex flex-col gap-1">
          <div className="flex items-center justify-between">
            <span className="text-caption text-muted">{formatDateTime(order.createdAt)}</span>
            <Badge label={STATUS_LABEL[order.status]} tone={STATUS_TONE[order.status]} />
          </div>
        </section>

        <section className="flex flex-col gap-3">
          <span className="text-title-sm text-ink">주문 상품 ({order.items.length}개)</span>
          <div className="flex flex-col gap-2">
            {order.items.map((item, idx) => (
              <div
                key={`${item.productId}-${idx}`}
                className="border-hairline bg-surface-card flex gap-3 rounded-md border p-3"
              >
                <div className="bg-surface-strong h-16 w-16 flex-shrink-0 rounded-sm" />
                <div className="flex flex-1 flex-col gap-1">
                  <span className="text-body-sm text-ink">{item.productName}</span>
                  <span className="text-caption text-muted">
                    {item.size ? `사이즈 ${item.size} · ` : ""}수량 {item.quantity}개
                  </span>
                  <span className="text-body-sm text-ink">{formatWon(item.unitPrice * item.quantity)}</span>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="flex flex-col gap-2">
          <span className="text-title-sm text-ink">배송지</span>
          <div className="border-hairline bg-surface-card flex flex-col gap-0.5 rounded-md border p-3">
            <span className="text-body-sm text-ink font-semibold">{order.address.recipient}</span>
            <span className="text-caption text-muted">{order.address.phone}</span>
            <span className="text-body-sm text-body">
              ({order.address.zipcode}) {order.address.address1} {order.address.address2}
            </span>
          </div>
        </section>

        <section className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-title-sm text-ink">총 결제금액</span>
            <span className="text-price text-ink">{formatWon(order.totalAmount)}</span>
          </div>
        </section>

        {canCancel && (
          <div className="border-hairline bg-surface-card fixed bottom-0 left-1/2 w-full max-w-[480px] -translate-x-1/2 p-3.5 border-t">
            <Button variant="secondary" fullWidth disabled={cancelling} onClick={handleCancel}>
              주문 취소
            </Button>
          </div>
        )}
      </div>

      {toastMessage && <Toast message={toastMessage} visible={!!toastMessage} />}
    </div>
  );
}
