"use client";

import { Suspense } from "react";
import { useRouter } from "next/navigation";
import { XCircle } from "lucide-react";
import { Button } from "@/components/core/Button";

function CheckoutFailContent() {
  const router = useRouter();

  return (
    <div className="bg-canvas flex min-h-screen flex-col items-center justify-center gap-6 px-4">
      <XCircle className="text-error h-16 w-16" strokeWidth={1.5} />
      <div className="flex flex-col items-center gap-1">
        <span className="text-title text-ink">결제에 실패했어요</span>
        <span className="text-body-sm text-muted">
          결제가 완료되지 않았어요. 장바구니에서 다시 시도해주세요
        </span>
      </div>
      <Button variant="primary" fullWidth onClick={() => router.push("/cart")}>
        장바구니로 돌아가기
      </Button>
    </div>
  );
}

/**
 * confirm 실패 또는 사용자 결제창 이탈 시 도달하는 화면.
 * spec에 따라 같은 주문으로 재결제하는 경로는 만들지 않는다 — "장바구니로 돌아가기"만 제공.
 */
export default function CheckoutFailPage() {
  return (
    <Suspense fallback={null}>
      <CheckoutFailContent />
    </Suspense>
  );
}
