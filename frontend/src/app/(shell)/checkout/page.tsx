"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Check, ChevronLeft } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/core/Button";
import { Toast } from "@/components/feedback/Toast";
import { AddressFields, type AddressFormValues } from "@/components/forms/AddressFields";
import { getCart, type CartItem } from "@/lib/storage/cart";
import { getCheckoutSelection } from "@/lib/storage/checkoutSelection";
import { getAddresses, type AddressResponse } from "@/lib/api/addresses";
import { createOrder, type OrderItemRequest } from "@/lib/api/orders";
import { ApiError } from "@/lib/api/client";

const addressSchema = z.object({
  recipient: z.string().min(1, "받는 사람을 입력해주세요"),
  phone: z.string().min(1, "연락처를 입력해주세요"),
  zipcode: z.string().min(1, "우편번호를 입력해주세요"),
  address1: z.string().min(1, "주소를 입력해주세요"),
  address2: z.string().optional(),
});

function formatWon(amount: number) {
  return `${amount.toLocaleString("ko-KR")}원`;
}

export default function CheckoutPage() {
  const router = useRouter();
  const [selectedItems, setSelectedItems] = useState<CartItem[]>([]);
  const [addresses, setAddresses] = useState<AddressResponse[]>([]);
  const [addressesLoaded, setAddressesLoaded] = useState(false);
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);
  const [showNewAddressForm, setShowNewAddressForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<AddressFormValues>({
    resolver: zodResolver(addressSchema),
  });

  useEffect(() => {
    Promise.resolve().then(() => {
      const keys = new Set(getCheckoutSelection());
      const items = getCart().filter((item) => keys.has(item.key));
      setSelectedItems(items);
      if (items.length === 0) {
        router.replace("/cart");
      }
    });
  }, [router]);

  useEffect(() => {
    getAddresses()
      .then((list) => {
        setAddresses(list);
        const defaultAddress = list.find((a) => a.isDefault) ?? list[0] ?? null;
        if (defaultAddress) {
          setSelectedAddressId(defaultAddress.id);
        } else {
          setShowNewAddressForm(true);
        }
      })
      .catch(() => {
        setShowNewAddressForm(true);
      })
      .finally(() => setAddressesLoaded(true));
  }, []);

  const totalAmount = useMemo(
    () => selectedItems.reduce((sum, item) => sum + item.unitPrice * item.qty, 0),
    [selectedItems],
  );

  function showToast(message: string) {
    setToastMessage(message);
    setTimeout(() => setToastMessage(null), 1800);
  }

  async function onSubmit(values: AddressFormValues) {
    if (selectedItems.length === 0) return;
    setSubmitting(true);
    try {
      const items: OrderItemRequest[] = selectedItems.map((item) => ({
        productId: item.id,
        quantity: item.qty,
        size: item.size || null,
      }));

      const request =
        !showNewAddressForm && selectedAddressId !== null
          ? { items, addressId: selectedAddressId }
          : {
              items,
              address: {
                recipient: values.recipient,
                phone: values.phone,
                zipcode: values.zipcode,
                address1: values.address1,
                address2: values.address2,
                isDefault: true,
              },
            };

      const order = await createOrder(request);
      router.push(`/checkout/payment?orderId=${order.orderId}`);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.errorCode === "OUT_OF_STOCK") {
          showToast("재고가 부족한 상품이 있어요. 장바구니를 확인해주세요");
          return;
        }
        if (err.errorCode === "VALIDATION_FAILED") {
          const addressFieldErrors = Object.entries(err.fieldErrors ?? {}).filter(([field]) =>
            field.startsWith("address."),
          );
          if (showNewAddressForm && addressFieldErrors.length > 0) {
            for (const [field, message] of addressFieldErrors) {
              const fieldName = field.replace("address.", "") as keyof AddressFormValues;
              setError(fieldName, { message });
            }
            return;
          }
          showToast(err.message);
          return;
        }
        if (err.errorCode === "PRODUCT_NOT_FOUND") {
          showToast(err.message);
          return;
        }
      }
      showToast("주문 생성에 실패했어요. 잠시 후 다시 시도해주세요");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="bg-canvas relative flex min-h-screen flex-col">
      <div className="border-hairline bg-surface-card flex h-13 flex-shrink-0 items-center px-4 border-b">
        <button onClick={() => router.back()} aria-label="뒤로가기" className="text-ink">
          <ChevronLeft className="h-5 w-5" />
        </button>
        <span className="text-title-sm text-ink flex-1 text-center">주문서 작성</span>
        <div className="h-5 w-5" />
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-1 flex-col gap-6 px-4 py-5 pb-28">
        <section className="flex flex-col gap-3">
          <span className="text-title-sm text-ink">배송지</span>

          {addressesLoaded && addresses.length > 0 && (
            <div className="flex flex-col gap-2">
              {addresses.map((address) => {
                const checked = !showNewAddressForm && selectedAddressId === address.id;
                return (
                  <button
                    type="button"
                    key={address.id}
                    onClick={() => {
                      setSelectedAddressId(address.id);
                      setShowNewAddressForm(false);
                    }}
                    className={`flex items-start gap-3 rounded-md border p-3 text-left ${
                      checked ? "border-brand-pink" : "border-hairline"
                    }`}
                  >
                    <span
                      className={`mt-0.5 flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full border-[1.5px] ${
                        checked ? "bg-brand-pink border-brand-pink" : "border-hairline bg-surface-card"
                      }`}
                    >
                      {checked && <Check className="h-3.5 w-3.5 text-on-brand" strokeWidth={3} />}
                    </span>
                    <div className="flex flex-col gap-0.5">
                      <div className="flex items-center gap-1.5">
                        <span className="text-body-sm text-ink font-semibold">{address.recipient}</span>
                        {address.isDefault && (
                          <span className="text-tag bg-brand-pink-tint text-brand-pink-deep rounded-full px-2 py-0.5">
                            기본배송지
                          </span>
                        )}
                      </div>
                      <span className="text-caption text-muted">{address.phone}</span>
                      <span className="text-body-sm text-body">
                        ({address.zipcode}) {address.address1} {address.address2}
                      </span>
                    </div>
                  </button>
                );
              })}

              <button
                type="button"
                onClick={() => setShowNewAddressForm((prev) => !prev)}
                className="border-hairline text-body-sm text-ink rounded-md border border-dashed py-3"
              >
                {showNewAddressForm ? "저장된 배송지 사용" : "새 배송지 추가"}
              </button>
            </div>
          )}

          {showNewAddressForm && <AddressFields register={register} errors={errors} />}
        </section>

        <section className="flex flex-col gap-3">
          <span className="text-title-sm text-ink">주문 상품 ({selectedItems.length}개)</span>
          <div className="flex flex-col gap-2">
            {selectedItems.map((item) => (
              <div key={item.key} className="border-hairline bg-surface-card flex gap-3 rounded-md border p-3">
                <div className="bg-surface-strong h-16 w-16 flex-shrink-0 rounded-sm" />
                <div className="flex flex-1 flex-col gap-1">
                  <span className="text-body-sm text-ink">{item.title}</span>
                  <span className="text-caption text-muted">
                    사이즈 {item.size} · 수량 {item.qty}개
                  </span>
                  <span className="text-body-sm text-ink">{formatWon(item.unitPrice * item.qty)}</span>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-title-sm text-ink">총 결제금액</span>
            <span className="text-price text-ink">{formatWon(totalAmount)}</span>
          </div>
        </section>

        <div className="border-hairline bg-surface-card fixed bottom-0 left-1/2 w-full max-w-[480px] -translate-x-1/2 p-3.5 border-t">
          <Button type="submit" variant="primary" fullWidth disabled={submitting || selectedItems.length === 0}>
            결제하기
          </Button>
        </div>
      </form>

      {toastMessage && <Toast message={toastMessage} visible={!!toastMessage} />}
    </div>
  );
}
