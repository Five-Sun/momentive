"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Check, Minus, Plus, X } from "lucide-react";
import { Button } from "@/components/core/Button";
import { ShippingProgress } from "@/components/feedback/ShippingProgress";
import {
  type CartItem,
  getCart,
  removeFromCart,
  updateCartQty,
} from "@/lib/storage/cart";
import { setCheckoutSelection } from "@/lib/storage/checkoutSelection";

const COUPON_DISCOUNT = 3000;
const FREE_SHIPPING_THRESHOLD = 70000;

function formatWon(amount: number) {
  return `${amount.toLocaleString("ko-KR")}원`;
}

export default function CartPage() {
  const router = useRouter();
  const [items, setItems] = useState<CartItem[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<Set<string>>(new Set());
  const [couponApplied, setCouponApplied] = useState(false);

  useEffect(() => {
    Promise.resolve().then(() => {
      const cart = getCart();
      setItems(cart);
      setSelectedKeys(new Set(cart.map((item) => item.key)));
    });
  }, []);

  function handleQtyChange(key: string, qty: number) {
    setItems(updateCartQty(key, qty));
    if (qty <= 0) {
      setSelectedKeys((prev) => {
        const next = new Set(prev);
        next.delete(key);
        return next;
      });
    }
  }

  function handleRemove(key: string) {
    setItems(removeFromCart(key));
    setSelectedKeys((prev) => {
      const next = new Set(prev);
      next.delete(key);
      return next;
    });
  }

  function handleToggleItem(key: string) {
    setSelectedKeys((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  }

  function handleToggleAll() {
    setSelectedKeys((prev) => (prev.size === items.length ? new Set() : new Set(items.map((item) => item.key))));
  }

  function handleCheckout() {
    setCheckoutSelection(Array.from(selectedKeys));
    router.push("/checkout");
  }

  const selectedItems = items.filter((item) => selectedKeys.has(item.key));
  const subtotal = selectedItems.reduce((sum, item) => sum + item.unitPrice * item.qty, 0);
  // 쿠폰은 아직 결제 금액에 반영하지 않는 placeholder UI (실제 쿠폰 시스템은 이 spec의 범위 밖)
  // "할인금액" 표시 줄에서만 사용하는 정보성 값이며, 총 결제금액(total) 계산에는 포함하지 않는다.
  const discount = couponApplied && selectedItems.length > 0 ? COUPON_DISCOUNT : 0;
  const total = subtotal;
  const remaining = Math.max(0, FREE_SHIPPING_THRESHOLD - subtotal);
  const allSelected = items.length > 0 && selectedKeys.size === items.length;

  return (
    <div className="bg-canvas relative flex min-h-screen flex-col">
      <div className="border-hairline bg-surface-card flex h-13 flex-shrink-0 items-center px-4 border-b">
        <button onClick={() => router.back()} aria-label="뒤로가기" className="text-ink">
          <ArrowLeft className="h-5 w-5" />
        </button>
        <span className="text-title-sm text-ink flex-1 text-center">장바구니</span>
        <div className="h-5 w-5" />
      </div>

      {items.length === 0 ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-4 py-20">
          <span className="text-body text-muted">장바구니가 비어 있어요</span>
          <Button variant="secondary" onClick={() => router.push("/")}>
            쇼핑하러 가기
          </Button>
        </div>
      ) : (
        <>
          <button
            onClick={handleToggleAll}
            className="border-hairline bg-surface-card flex h-12 w-full flex-shrink-0 items-center gap-2 border-b px-4"
          >
            <span
              className={`flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full border-[1.5px] ${
                allSelected ? "bg-brand-pink border-brand-pink" : "border-hairline bg-surface-card"
              }`}
            >
              {allSelected && <Check className="h-3.5 w-3.5 text-on-brand" strokeWidth={3} />}
            </span>
            <span className="text-body-sm text-ink">
              전체선택 ({selectedKeys.size}/{items.length})
            </span>
          </button>

          <div className="flex flex-col gap-3 p-4">
            {items.map((item) => {
              const checked = selectedKeys.has(item.key);
              return (
                <div
                  key={item.key}
                  className="border-hairline bg-surface-card flex gap-3 rounded-md border p-3"
                >
                  <button
                    onClick={() => handleToggleItem(item.key)}
                    aria-label={checked ? "선택 해제" : "선택"}
                    className={`mt-1 flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full border-[1.5px] ${
                      checked ? "bg-brand-pink border-brand-pink" : "border-hairline bg-surface-card"
                    }`}
                  >
                    {checked && <Check className="h-3.5 w-3.5 text-on-brand" strokeWidth={3} />}
                  </button>
                  <div className="bg-surface-strong h-20 w-20 flex-shrink-0 rounded-sm" />
                  <div className="flex flex-1 flex-col gap-1.5">
                    <div className="flex items-start justify-between gap-2">
                      <span className="text-title-sm text-ink">{item.title}</span>
                      <button
                        onClick={() => handleRemove(item.key)}
                        aria-label="삭제"
                        className="text-muted"
                      >
                        <X className="h-4 w-4" />
                      </button>
                    </div>
                    <span className="text-caption text-muted">사이즈 {item.size}</span>
                    <div className="flex items-center justify-between">
                      <div className="border-hairline flex items-center gap-3 rounded-full border px-2 py-1">
                        <button
                          onClick={() => handleQtyChange(item.key, item.qty - 1)}
                          aria-label="수량 감소"
                          className="text-ink"
                        >
                          <Minus className="h-3.5 w-3.5" />
                        </button>
                        <span className="text-body-sm text-ink w-4 text-center">{item.qty}</span>
                        <button
                          onClick={() => handleQtyChange(item.key, item.qty + 1)}
                          aria-label="수량 증가"
                          className="text-ink"
                        >
                          <Plus className="h-3.5 w-3.5" />
                        </button>
                      </div>
                      <span className="text-price text-ink">{formatWon(item.unitPrice * item.qty)}</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          <div className="px-4">
            <ShippingProgress remaining={remaining} formatAmount={formatWon} />
          </div>

          <div className="px-4 pt-4">
            <button
              onClick={() => setCouponApplied((prev) => !prev)}
              className="border-hairline bg-surface-card flex h-14 w-full items-center justify-between rounded-md border px-4"
            >
              <span className="text-body-sm text-ink">쿠폰 할인 (3,000원)</span>
              <div
                className={`flex h-6 w-11 items-center rounded-full px-0.5 transition-colors ${
                  couponApplied ? "bg-brand-pink justify-end" : "bg-hairline justify-start"
                }`}
              >
                <div className="h-5 w-5 rounded-full bg-white" />
              </div>
            </button>
          </div>

          <div className="flex flex-col gap-2 px-4 py-4">
            <div className="flex items-center justify-between">
              <span className="text-body-sm text-muted">상품금액</span>
              <span className="text-body-sm text-ink">{formatWon(subtotal)}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-body-sm text-muted">할인금액</span>
              <span className="text-body-sm text-ink">-{formatWon(discount)}</span>
            </div>
            <div className="bg-hairline my-1.5 h-px" />
            <div className="flex items-center justify-between">
              <span className="text-title-sm text-ink">총 결제금액</span>
              <span className="text-price text-ink">{formatWon(total)}</span>
            </div>
          </div>

          <div className="border-hairline bg-surface-card sticky bottom-16 p-3.5 border-t">
            <Button variant="primary" disabled={selectedKeys.size === 0} onClick={handleCheckout} fullWidth>
              구매하기
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
