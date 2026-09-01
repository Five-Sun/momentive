"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { ArrowLeft, Ticket } from "lucide-react";
import { Button } from "@/components/core/Button";
import { TextField } from "@/components/forms/TextField";
import { Toast } from "@/components/feedback/Toast";
import { useAuth } from "@/lib/auth/AuthProvider";
import { fetchMyCoupons, registerCoupon, type UserCouponResponse } from "@/lib/api/coupon";
import { ApiError } from "@/lib/api/client";

const couponSchema = z.object({
  code: z.string().min(1, "쿠폰 코드를 입력해주세요"),
});

type CouponFormValues = z.infer<typeof couponSchema>;

function formatWon(amount: number) {
  return `${amount.toLocaleString("ko-KR")}원`;
}

function formatDiscount(coupon: UserCouponResponse) {
  if (coupon.discountType === "FIXED") {
    return `${formatWon(coupon.discountValue)} 할인`;
  }
  const maxLabel = coupon.maxDiscountAmount != null ? ` (최대 ${formatWon(coupon.maxDiscountAmount)})` : "";
  return `${coupon.discountValue}% 할인${maxLabel}`;
}

function formatExpiresAt(expiresAt: string) {
  const date = new Date(expiresAt);
  return `${date.toLocaleDateString("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit" })}까지`;
}

function isExpired(coupon: UserCouponResponse) {
  return new Date(coupon.expiresAt).getTime() < Date.now();
}

function CouponCard({ coupon, inactive }: { coupon: UserCouponResponse; inactive: boolean }) {
  return (
    <div
      className={`border-hairline bg-surface-card flex gap-3 rounded-md border p-3.5 ${
        inactive ? "opacity-50" : ""
      }`}
    >
      <div className="bg-surface-strong flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full">
        <Ticket className="text-brand-pink-deep h-5 w-5" />
      </div>
      <div className="flex flex-1 flex-col gap-0.5">
        <span className="text-body-sm text-ink font-semibold">{coupon.couponName}</span>
        <span className="text-body-sm text-body">{formatDiscount(coupon)}</span>
        <div className="text-caption text-muted flex flex-wrap gap-x-2">
          {coupon.minOrderAmount > 0 && <span>{formatWon(coupon.minOrderAmount)} 이상 구매 시</span>}
          <span>{formatExpiresAt(coupon.expiresAt)}</span>
        </div>
      </div>
    </div>
  );
}

export default function MyCouponsPage() {
  const router = useRouter();
  const { user } = useAuth();
  const [coupons, setCoupons] = useState<UserCouponResponse[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<CouponFormValues>({
    resolver: zodResolver(couponSchema),
    defaultValues: { code: "" },
  });

  useEffect(() => {
    if (!user) return;
    fetchMyCoupons()
      .then(setCoupons)
      .catch(() => {
        setLoadFailed(true);
        showToast("쿠폰 목록을 불러오지 못했어요");
      })
      .finally(() => setLoaded(true));
  }, [user]);

  function showToast(message: string) {
    setToastMessage(message);
    setTimeout(() => setToastMessage(null), 1800);
  }

  async function onSubmit(values: CouponFormValues) {
    setSubmitting(true);
    try {
      const created = await registerCoupon(values.code.trim());
      setCoupons((prev) => [created, ...prev]);
      reset({ code: "" });
      showToast("쿠폰을 등록했어요");
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors?.code) {
        setError("code", { message: err.fieldErrors.code });
        return;
      }
      showToast(err instanceof ApiError ? err.message : "등록에 실패했어요. 잠시 후 다시 시도해주세요");
    } finally {
      setSubmitting(false);
    }
  }

  const availableCoupons = coupons.filter((c) => c.status === "AVAILABLE" && !isExpired(c));
  const inactiveCoupons = coupons.filter((c) => c.status === "USED" || isExpired(c));

  return (
    <div className="bg-canvas relative flex min-h-screen flex-col">
      <div className="border-hairline bg-surface-card flex h-13 flex-shrink-0 items-center border-b px-4">
        <button onClick={() => router.back()} aria-label="뒤로가기" className="text-ink">
          <ArrowLeft className="h-5 w-5" />
        </button>
        <span className="text-title-sm text-ink flex-1 text-center">쿠폰함</span>
        <div className="h-5 w-5" />
      </div>

      {!user ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-4 py-20">
          <span className="text-body text-muted">로그인이 필요합니다</span>
          <Button variant="primary" onClick={() => router.push("/login")}>
            로그인
          </Button>
        </div>
      ) : (
        <div className="flex flex-1 flex-col gap-6 p-4 pb-28">
          <form
            onSubmit={handleSubmit(onSubmit)}
            className="border-hairline bg-surface-card flex flex-col gap-3 rounded-md border p-3.5"
          >
            <span className="text-title-sm text-ink">쿠폰 코드 등록</span>
            <div className="flex items-start gap-2">
              <div className="flex-1">
                <TextField
                  label="쿠폰 코드"
                  placeholder="예: WELCOME3000"
                  error={errors.code?.message}
                  {...register("code")}
                />
              </div>
            </div>
            <Button type="submit" variant="primary" fullWidth disabled={submitting}>
              등록하기
            </Button>
          </form>

          {loaded && coupons.length === 0 && (
            <div className="flex flex-1 flex-col items-center justify-center gap-2 py-12">
              <span className="text-body text-muted">
                {loadFailed ? "쿠폰 목록을 불러오지 못했어요" : "보유한 쿠폰이 없어요"}
              </span>
            </div>
          )}

          {availableCoupons.length > 0 && (
            <div className="flex flex-col gap-3">
              <span className="text-title-sm text-ink">사용 가능한 쿠폰</span>
              <div className="flex flex-col gap-3">
                {availableCoupons.map((coupon) => (
                  <CouponCard key={coupon.id} coupon={coupon} inactive={false} />
                ))}
              </div>
            </div>
          )}

          {inactiveCoupons.length > 0 && (
            <div className="flex flex-col gap-3">
              <span className="text-title-sm text-ink">사용 완료・만료</span>
              <div className="flex flex-col gap-3">
                {inactiveCoupons.map((coupon) => (
                  <CouponCard key={coupon.id} coupon={coupon} inactive={true} />
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {toastMessage && <Toast message={toastMessage} visible={!!toastMessage} />}
    </div>
  );
}
