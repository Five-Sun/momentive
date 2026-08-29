"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { loadPaymentWidget, type PaymentWidgetInstance } from "@tosspayments/payment-widget-sdk";
import { getOrder, type OrderResponse } from "@/lib/api/orders";
import { ApiError } from "@/lib/api/client";

const TOSS_CLIENT_KEY = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY ?? "";
const PAYMENT_METHODS_SELECTOR = "#toss-payment-methods";
const AGREEMENT_SELECTOR = "#toss-agreement";

function PaymentWidgetContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const orderId = searchParams.get("orderId");

  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const widgetRef = useRef<PaymentWidgetInstance | null>(null);

  useEffect(() => {
    if (!orderId) {
      router.replace("/cart");
      return;
    }

    getOrder(Number(orderId))
      .then((res) => {
        if (res.status !== "PENDING") {
          router.replace(`/checkout/fail?orderId=${orderId}`);
          return;
        }
        setOrder(res);
      })
      .catch((err) => {
        if (err instanceof ApiError) {
          setErrorMessage(err.message);
        } else {
          setErrorMessage("주문 정보를 불러오지 못했어요");
        }
      });
  }, [orderId, router]);

  useEffect(() => {
    if (!order) return;

    let cancelled = false;

    loadPaymentWidget(TOSS_CLIENT_KEY, "ANONYMOUS").then((widget) => {
      if (cancelled) return;
      widgetRef.current = widget;
      widget.renderPaymentMethods(PAYMENT_METHODS_SELECTOR, order.totalAmount);
      widget.renderAgreement(AGREEMENT_SELECTOR);
    });

    return () => {
      cancelled = true;
    };
  }, [order]);

  async function handlePay() {
    if (!order || !widgetRef.current) return;
    const origin = window.location.origin;

    try {
      await widgetRef.current.requestPayment({
        orderId: String(order.orderId),
        orderName:
          order.items.length > 1
            ? `${order.items[0].productName} 외 ${order.items.length - 1}건`
            : order.items[0].productName,
        successUrl: `${origin}/checkout/success?orderId=${order.orderId}`,
        failUrl: `${origin}/checkout/fail?orderId=${order.orderId}`,
      });
    } catch {
      // 사용자가 결제창을 닫거나 이탈한 경우 Toss SDK가 reject한다.
      // 별도 처리 없이 결제위젯 화면에 남겨 재시도할 수 있게 한다.
    }
  }

  if (errorMessage) {
    return (
      <div className="bg-canvas flex min-h-screen flex-col items-center justify-center gap-3 px-4">
        <span className="text-body text-muted">{errorMessage}</span>
      </div>
    );
  }

  return (
    <div className="bg-canvas flex min-h-screen flex-col">
      <div className="border-hairline bg-surface-card flex h-13 flex-shrink-0 items-center justify-center px-4 border-b">
        <span className="text-title-sm text-ink">결제</span>
      </div>

      <div className="flex flex-1 flex-col gap-4 px-4 py-5 pb-28">
        <div id="toss-payment-methods" />
        <div id="toss-agreement" />
      </div>

      <div className="border-hairline bg-surface-card fixed bottom-0 left-1/2 w-full max-w-[480px] -translate-x-1/2 p-3.5 border-t">
        <button
          type="button"
          onClick={handlePay}
          disabled={!order}
          className="bg-brand-pink text-on-brand disabled:bg-brand-pink-soft disabled:text-muted-soft h-12 w-full rounded-full font-semibold"
        >
          {order ? `${order.totalAmount.toLocaleString("ko-KR")}원 결제하기` : "불러오는 중..."}
        </button>
      </div>
    </div>
  );
}

export default function PaymentPage() {
  return (
    <Suspense fallback={null}>
      <PaymentWidgetContent />
    </Suspense>
  );
}
