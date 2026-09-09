"use client";

import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/core/Button";
import { Badge } from "@/components/core/Badge";
import { TextField } from "@/components/forms/TextField";
import { Toast } from "@/components/feedback/Toast";
import { ApiError } from "@/lib/api/client";
import type { OrderStatus } from "@/lib/api/orders";
import {
  cancelAdminOrder,
  getAdminOrder,
  updateAdminOrderShipping,
  type AdminOrderDetail,
  type AdminShippingUpdateRequest,
} from "@/lib/api/adminOrders";
import { SHIPPING_STATUS_LABEL, SHIPPING_STATUS_VALUES } from "@/lib/shippingStatus";
import { COURIER_OPTIONS, COURIER_OTHER } from "../courierOptions";

/**
 * 관리자 주문 상세. 주문 정보(좌측)와 배송 관리/취소 폼(우측)을 나란히 둔 데스크톱 2단
 * 레이아웃 — `/admin`은 `(shell)` 밖이라 모바일 프레임이 없다(`docs/design.md` 참고).
 *
 * 저장 버튼은 문서 흐름 안(카드 하단)에 두고 `fixed`로 띄우지 않는다 — `AdminProductForm`과
 * 동일하게 고정 UI 겹침으로 클릭이 막히는 사고를 만들지 않기 위함
 * (`docs/backlog/2026-08-29-cart-order-payment-phase6-01.md` 재발 방지).
 */

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

const shippingFormSchema = z.object({
  shippingStatus: z.enum(SHIPPING_STATUS_VALUES),
  courierOption: z.string().min(1, "택배사를 선택해주세요"),
  courierOther: z.string().max(50, "택배사명은 50자까지 입력할 수 있어요"),
  trackingNumber: z.string().max(100, "송장번호는 100자까지 입력할 수 있어요"),
});

type ShippingFormValues = z.infer<typeof shippingFormSchema>;

const KNOWN_COURIERS: readonly string[] = COURIER_OPTIONS;

/** 서버가 준 courier 값을 드롭다운 선택지 + "기타" 자유 텍스트로 되돌린다. */
function toCourierFields(courier: string | null): { courierOption: string; courierOther: string } {
  if (!courier) return { courierOption: COURIER_OPTIONS[0], courierOther: "" };
  if (KNOWN_COURIERS.includes(courier)) return { courierOption: courier, courierOther: "" };
  return { courierOption: COURIER_OTHER, courierOther: courier };
}

function toDefaultValues(order: AdminOrderDetail): ShippingFormValues {
  const { courierOption, courierOther } = toCourierFields(order.courier);
  return {
    shippingStatus: order.shippingStatus ?? "PREPARING",
    courierOption,
    courierOther,
    trackingNumber: order.trackingNumber ?? "",
  };
}

const FIELD_BOX_CLASS =
  "bg-surface-soft border-hairline text-body text-ink placeholder:text-muted h-12 rounded-md border px-4 outline-none focus:border-brand-pink disabled:text-muted-soft disabled:opacity-70";

function FieldShell({
  label,
  htmlFor,
  error,
  children,
}: {
  label: string;
  htmlFor: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={htmlFor} className="text-body-sm text-body">
        {label}
      </label>
      {children}
      {error && <span className="text-body-sm text-error">{error}</span>}
    </div>
  );
}

/** 조회 상태를 유니온으로 두어 "불러오는 중"·"실패"·"상세" 렌더 조건을 배타적으로 유지한다. */
type DetailState =
  | { status: "loading" }
  | { status: "loaded"; order: AdminOrderDetail }
  | { status: "error"; message: string };

export default function AdminOrderDetailPage() {
  const params = useParams<{ orderId: string }>();
  const orderId = Number(params.orderId);
  const hasValidId = Number.isInteger(orderId);

  const [state, setState] = useState<DetailState>(() =>
    hasValidId ? { status: "loading" } : { status: "error", message: "잘못된 주문 주소예요" },
  );
  const [cancelling, setCancelling] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ShippingFormValues>({
    resolver: zodResolver(shippingFormSchema),
    defaultValues: {
      shippingStatus: "PREPARING",
      courierOption: COURIER_OPTIONS[0],
      courierOther: "",
      trackingNumber: "",
    },
  });

  useEffect(() => {
    if (!hasValidId) return;

    let cancelled = false;
    getAdminOrder(orderId)
      .then((order) => {
        if (cancelled) return;
        setState({ status: "loaded", order });
        reset(toDefaultValues(order));
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setState({
          status: "error",
          message: err instanceof ApiError ? err.message : "주문을 불러오지 못했어요",
        });
      });

    return () => {
      cancelled = true;
    };
  }, [orderId, hasValidId, reset]);

  function showToast(message: string) {
    setToastMessage(message);
    setTimeout(() => setToastMessage(null), 1800);
  }

  const order = state.status === "loaded" ? state.order : null;
  // 배송 관리 폼·취소 버튼 모두 PAID일 때만 활성화된다 — 상태가 여러 개로 나뉘지 않도록 하나의
  // 조건으로 배타적으로 유지한다.
  const isManageable = order?.status === "PAID";
  const courierOption = useWatch({ control, name: "courierOption" });

  async function handleFormSubmit(values: ShippingFormValues) {
    if (!order) return;
    setFormError(null);

    const courier =
      values.courierOption === COURIER_OTHER ? values.courierOther.trim() : values.courierOption;
    const request: AdminShippingUpdateRequest = {
      shippingStatus: values.shippingStatus,
      courier: courier === "" ? null : courier,
      trackingNumber: values.trackingNumber.trim() === "" ? null : values.trackingNumber.trim(),
    };

    try {
      const updated = await updateAdminOrderShipping(order.orderId, request);
      setState({ status: "loaded", order: updated });
      reset(toDefaultValues(updated));
      showToast("배송정보를 저장했어요");
    } catch (err) {
      // 조건부 필수 검증(SHIPPING_INFO_REQUIRED)과 PAID 아닌 주문(ORDER_SHIPPING_NOT_APPLICABLE)은
      // 폼 안에 인라인으로 보여주고, 그 외(네트워크 실패 등)는 토스트로 뭉뚱그린다.
      if (
        err instanceof ApiError &&
        (err.errorCode === "SHIPPING_INFO_REQUIRED" || err.errorCode === "ORDER_SHIPPING_NOT_APPLICABLE")
      ) {
        setFormError(err.message);
        return;
      }
      showToast(err instanceof ApiError ? err.message : "저장에 실패했어요. 잠시 후 다시 시도해주세요");
    }
  }

  async function handleCancel() {
    if (!order) return;
    if (!window.confirm("이 주문을 취소할까요? 재고가 복원되고 주문이 취소완료 상태가 돼요")) return;

    setCancelling(true);
    try {
      await cancelAdminOrder(order.orderId);
      const refreshed = await getAdminOrder(order.orderId);
      setState({ status: "loaded", order: refreshed });
      reset(toDefaultValues(refreshed));
      showToast("주문을 취소했어요");
    } catch (err) {
      showToast(
        err instanceof ApiError && err.errorCode === "ORDER_NOT_CANCELLABLE"
          ? "이미 취소할 수 없는 주문이에요"
          : "주문 취소에 실패했어요. 잠시 후 다시 시도해주세요",
      );
    } finally {
      setCancelling(false);
    }
  }

  return (
    <div className="relative flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <Link href="/admin/orders" className="text-body-sm text-muted underline">
          주문 목록으로
        </Link>
        <h1 className="text-title text-ink">{order ? `주문 #${order.orderId}` : "주문 상세"}</h1>
      </div>

      {state.status === "loading" && (
        <p className="text-body text-muted py-16 text-center">불러오는 중이에요</p>
      )}

      {state.status === "error" && (
        <p className="text-body text-error py-16 text-center">{state.message}</p>
      )}

      {order && (
        <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
          <div className="flex flex-col gap-6">
            <section className="border-hairline bg-surface-card flex flex-col gap-3 rounded-md border p-5">
              <div className="flex items-center justify-between">
                <h2 className="text-title-sm text-ink">주문 정보</h2>
                <Badge label={STATUS_LABEL[order.status]} tone={STATUS_TONE[order.status]} />
              </div>
              <div className="text-body-sm text-body flex flex-col gap-1">
                <span>
                  {order.userNickname} · {order.userEmail}
                </span>
                <span className="text-caption text-muted">{formatDateTime(order.createdAt)}</span>
              </div>
            </section>

            <section className="border-hairline bg-surface-card flex flex-col gap-2 rounded-md border p-5">
              <h2 className="text-title-sm text-ink">배송지</h2>
              <div className="text-body-sm text-body flex flex-col gap-0.5">
                <span className="text-ink font-semibold">{order.address.recipient}</span>
                <span className="text-caption text-muted">{order.address.phone}</span>
                <span>
                  ({order.address.zipcode}) {order.address.address1} {order.address.address2}
                </span>
              </div>
            </section>

            <section className="border-hairline bg-surface-card flex flex-col gap-3 rounded-md border p-5">
              <h2 className="text-title-sm text-ink">주문 상품 ({order.items.length}개)</h2>
              <div className="flex flex-col gap-2">
                {order.items.map((item, idx) => (
                  <div
                    key={`${item.productId}-${idx}`}
                    className="border-hairline-soft flex items-center justify-between border-b pb-2 last:border-b-0 last:pb-0"
                  >
                    <div className="flex flex-col">
                      <span className="text-body-sm text-ink">{item.productName}</span>
                      <span className="text-caption text-muted">
                        {item.size ? `사이즈 ${item.size} · ` : ""}수량 {item.quantity}개
                      </span>
                    </div>
                    <span className="text-body-sm text-ink">
                      {formatWon(item.unitPrice * item.quantity)}
                    </span>
                  </div>
                ))}
              </div>
            </section>

            <section className="border-hairline bg-surface-card flex flex-col gap-2 rounded-md border p-5">
              <div className="flex items-center justify-between">
                <span className="text-body-sm text-body">상품금액</span>
                <span className="text-body-sm text-ink">{formatWon(order.itemsSubtotal)}</span>
              </div>
              {order.discountAmount > 0 && order.couponName && (
                <div className="flex items-center justify-between">
                  <span className="text-body-sm text-body">쿠폰 할인 · {order.couponName}</span>
                  <span className="text-body-sm text-ink">-{formatWon(order.discountAmount)}</span>
                </div>
              )}
              <div className="flex items-center justify-between">
                <span className="text-body-sm text-body">배송비</span>
                <span className="text-body-sm text-ink">
                  {order.shippingFee === 0 ? "무료" : formatWon(order.shippingFee)}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-title-sm text-ink">총 결제금액</span>
                <span className="text-price text-ink">{formatWon(order.totalAmount)}</span>
              </div>
            </section>
          </div>

          <div className="flex flex-col gap-4">
            <form
              onSubmit={handleSubmit(handleFormSubmit)}
              className="border-hairline bg-surface-card flex flex-col gap-4 rounded-md border p-5"
            >
              <h2 className="text-title-sm text-ink">배송 관리</h2>

              {!isManageable && (
                <p className="text-body-sm text-muted">
                  결제 완료된 주문만 배송상태를 관리할 수 있어요
                </p>
              )}

              <FieldShell
                label="배송상태"
                htmlFor="shippingStatus"
                error={errors.shippingStatus?.message}
              >
                <select
                  id="shippingStatus"
                  disabled={!isManageable}
                  className={FIELD_BOX_CLASS}
                  {...register("shippingStatus")}
                >
                  {SHIPPING_STATUS_VALUES.map((value) => (
                    <option key={value} value={value}>
                      {SHIPPING_STATUS_LABEL[value]}
                    </option>
                  ))}
                </select>
              </FieldShell>

              <FieldShell label="택배사" htmlFor="courierOption" error={errors.courierOption?.message}>
                <select
                  id="courierOption"
                  disabled={!isManageable}
                  className={FIELD_BOX_CLASS}
                  {...register("courierOption")}
                >
                  {COURIER_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                  <option value={COURIER_OTHER}>{COURIER_OTHER}</option>
                </select>
              </FieldShell>

              {courierOption === COURIER_OTHER && (
                <TextField
                  label="택배사명 직접 입력"
                  placeholder="택배사명을 입력해주세요"
                  disabled={!isManageable}
                  error={errors.courierOther?.message}
                  {...register("courierOther")}
                />
              )}

              <TextField
                label="송장번호"
                placeholder="예: 123456789012"
                disabled={!isManageable}
                error={errors.trackingNumber?.message}
                {...register("trackingNumber")}
              />

              {formError && <p className="text-body-sm text-error">{formError}</p>}

              <Button type="submit" variant="primary" disabled={!isManageable || isSubmitting}>
                저장하기
              </Button>
            </form>

            <div className="border-hairline bg-surface-card flex flex-col gap-3 rounded-md border p-5">
              <h2 className="text-title-sm text-ink">주문 취소</h2>
              <p className="text-body-sm text-muted">
                {isManageable
                  ? "취소하면 재고가 복원되고 주문이 취소완료 상태가 돼요"
                  : "결제 완료된 주문만 취소할 수 있어요"}
              </p>
              <Button
                variant="secondary"
                disabled={!isManageable || cancelling}
                onClick={handleCancel}
              >
                주문 취소
              </Button>
            </div>
          </div>
        </div>
      )}

      {toastMessage && <Toast message={toastMessage} visible={!!toastMessage} />}
    </div>
  );
}
