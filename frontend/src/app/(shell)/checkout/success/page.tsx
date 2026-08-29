"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { CheckCircle2 } from "lucide-react";
import { Button } from "@/components/core/Button";
import { confirmOrder } from "@/lib/api/orders";

interface ConfirmedResult {
  orderId: number;
}

function CheckoutSuccessContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [result, setResult] = useState<ConfirmedResult | null>(null);

  useEffect(() => {
    const paymentKey = searchParams.get("paymentKey");
    const tossOrderId = searchParams.get("orderId");
    const amount = searchParams.get("amount");

    if (!paymentKey || !tossOrderId || !amount) {
      router.replace("/checkout/fail");
      return;
    }

    const numericOrderId = Number(tossOrderId);

    confirmOrder(numericOrderId, {
      paymentKey,
      orderId: tossOrderId,
      amount: Number(amount),
    })
      .then(() => setResult({ orderId: numericOrderId }))
      .catch(() => router.replace(`/checkout/fail?orderId=${numericOrderId}`));
  }, [router, searchParams]);

  if (!result) {
    return (
      <div className="bg-canvas flex min-h-screen flex-col items-center justify-center gap-3 px-4">
        <span className="text-body text-muted">결제를 확인하고 있어요...</span>
      </div>
    );
  }

  return (
    <div className="bg-canvas flex min-h-screen flex-col items-center justify-center gap-6 px-4">
      <CheckCircle2 className="text-brand-pink h-16 w-16" strokeWidth={1.5} />
      <div className="flex flex-col items-center gap-1">
        <span className="text-title text-ink">주문이 완료됐어요</span>
        <span className="text-body-sm text-muted">결제가 정상적으로 처리됐어요</span>
      </div>
      <div className="flex w-full flex-col gap-2">
        <Button variant="primary" fullWidth onClick={() => router.push(`/mypage/orders/${result.orderId}`)}>
          주문내역 보기
        </Button>
        <Button variant="secondary" fullWidth onClick={() => router.push("/")}>
          쇼핑 계속하기
        </Button>
      </div>
    </div>
  );
}

export default function CheckoutSuccessPage() {
  return (
    <Suspense fallback={null}>
      <CheckoutSuccessContent />
    </Suspense>
  );
}
