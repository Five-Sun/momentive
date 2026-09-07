"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { CheckCircle2 } from "lucide-react";
import { Button } from "@/components/core/Button";
import { confirmOrder, getOrder } from "@/lib/api/orders";
import { cartKeyOf, removeCartItems } from "@/lib/storage/cart";
import { clearCheckoutSelection, getCheckoutSelection } from "@/lib/storage/checkoutSelection";

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

      // 부분결제를 지원하므로 장바구니 전체를 비우지 않는다. 서버가 확정한 주문 항목의
      // variant만 골라 걷어내고, 결제하지 않고 남겨둔 항목은 그대로 유지한다.
      //
      // `checkoutSelection`(sessionStorage)을 근거로 쓰지 않는 이유: 간편결제 앱 전환처럼
      // 결제사 리다이렉트가 원래 탭 컨텍스트를 벗어나면 sessionStorage가 비어 있어
      // 장바구니가 그대로 남는다(중복 구매를 유발했던 원래 버그의 재발). 주문 응답은
      // 그런 경우에도 서버에서 그대로 받아올 수 있다.
      try {
        const order = await getOrder(numericOrderId);
        removeCartItems(
          order.items
            .map((item) => item.variantId)
            .filter((id): id is number => id != null)
            .map(cartKeyOf),
        );
      } catch (err) {
        // 조회에 실패해도 결제 자체엔 영향이 없다. 선택 목록으로 한 번 더 시도한다.
        console.error("결제 완료 후 주문 조회 실패, 선택 목록으로 대체", err);
        removeCartItems(getCheckoutSelection());
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
