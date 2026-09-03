"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { CheckCircle2 } from "lucide-react";
import { Button } from "@/components/core/Button";
import { confirmOrder, getOrder } from "@/lib/api/orders";
import { cartKeyOf, removeCartItems } from "@/lib/storage/cart";
import { clearCheckoutSelection } from "@/lib/storage/checkoutSelection";

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
    const confirmRequest = {
      paymentKey,
      orderId: tossOrderId,
      amount: Number(amount),
    };

    async function finalize() {
      try {
        await confirmOrder(numericOrderId, confirmRequest);
      } catch {
        router.replace(`/checkout/fail?orderId=${numericOrderId}`);
        return;
      }

      // 여기서부터 결제는 이미 확정됐다. 아래 장바구니 정리가 실패하더라도
      // 실패 화면으로 보내면 안 되므로 confirm과 분리해 처리한다.
      setResult({ orderId: numericOrderId });

      try {
        // 부분결제를 지원하므로 장바구니 전체를 비우지 않는다. 서버가 확정한 주문 항목만
        // 키로 되돌려 제거해, 결제하지 않고 남겨둔 항목은 그대로 유지한다.
        const order = await getOrder(numericOrderId);
        removeCartItems(order.items.map((item) => cartKeyOf(item.productId, item.size)));
      } catch (err) {
        // 정리에 실패해도 주문 자체엔 영향이 없다. 원인 파악용으로 남기기만 한다.
        console.error("결제 완료 후 장바구니 정리 실패", err);
      }
      clearCheckoutSelection();
    }

    finalize();
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
      <div className="flex w-full flex-col gap-2 lg:max-w-[480px]">
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
