"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { loadPaymentWidget, type PaymentWidgetInstance } from "@tosspayments/payment-widget-sdk";
import { getOrder, type OrderResponse } from "@/lib/api/orders";
import { ApiError } from "@/lib/api/client";
import { Toast } from "@/components/feedback/Toast";

const TOSS_CLIENT_KEY = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY ?? "";
const PAYMENT_METHODS_SELECTOR = "#toss-payment-methods";
const AGREEMENT_SELECTOR = "#toss-agreement";

interface TossSdkError {
  code: string;
  message: string;
}

function isTossSdkError(err: unknown): err is TossSdkError {
  return (
    typeof err === "object" &&
    err !== null &&
    "code" in err &&
    typeof (err as { code: unknown }).code === "string"
  );
}

function PaymentWidgetContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const orderId = searchParams.get("orderId");

  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const widgetRef = useRef<PaymentWidgetInstance | null>(null);

  function showToast(message: string) {
    setToastMessage(message);
    setTimeout(() => setToastMessage(null), 1800);
  }

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

    loadPaymentWidget(TOSS_CLIENT_KEY, "ANONYMOUS")
      .then((widget) => {
        if (cancelled) return;
        widgetRef.current = widget;
        widget.renderPaymentMethods(PAYMENT_METHODS_SELECTOR, order.totalAmount);
        widget.renderAgreement(AGREEMENT_SELECTOR);
      })
      .catch(() => {
        if (cancelled) return;
        setErrorMessage("결제위젯을 불러오지 못했어요. 잠시 후 다시 시도해주세요");
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
    } catch (err) {
      const code = isTossSdkError(err) ? err.code : null;
      if (code === "USER_CANCEL") {
        // 사용자가 결제창을 직접 닫은 경우: 결제위젯 화면에 그대로 남겨 재시도하게 한다.
        return;
      }
      showToast(
        code === "NETWORK_ERROR"
          ? "네트워크 오류로 결제를 진행하지 못했어요. 다시 시도해주세요"
          : "결제를 진행하지 못했어요. 다시 시도해주세요",
      );
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
    <div className="bg-canvas relative flex min-h-screen flex-col">
      <div className="border-hairline bg-surface-card flex h-13 flex-shrink-0 items-center justify-center px-4 border-b lg:hidden">
        <span className="text-title-sm text-ink">결제</span>
      </div>

      <div className="mx-auto w-full lg:max-w-[720px]">
        <div className="hidden px-0 pt-7 pb-4 lg:block">
          <span className="text-title-sm text-ink">결제</span>
        </div>

        <div className="flex flex-1 flex-col gap-4 px-4 py-5 pb-28 lg:px-0 lg:py-0 lg:pb-8">
          <div id="toss-payment-methods" />
          <div id="toss-agreement" />

          <div className="hidden lg:block">
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
      </div>

      <div className="border-hairline bg-surface-card fixed bottom-0 left-1/2 w-full max-w-[480px] -translate-x-1/2 p-3.5 border-t lg:hidden">
        <button
          type="button"
          onClick={handlePay}
          disabled={!order}
          className="bg-brand-pink text-on-brand disabled:bg-brand-pink-soft disabled:text-muted-soft h-12 w-full rounded-full font-semibold"
        >
          {order ? `${order.totalAmount.toLocaleString("ko-KR")}원 결제하기` : "불러오는 중..."}
        </button>
      </div>

      {toastMessage && <Toast message={toastMessage} visible={!!toastMessage} />}
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
