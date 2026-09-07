"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { Button } from "@/components/core/Button";
import { Toast } from "@/components/feedback/Toast";
import { ApiError } from "@/lib/api/client";
import {
  deleteAdminProduct,
  getAdminProduct,
  updateAdminProduct,
  type AdminProductDetail,
  type AdminProductRequest,
} from "@/lib/api/admin";
import { AdminProductForm } from "../AdminProductForm";

/** 조회 상태를 유니온으로 두어 "불러오는 중"·"실패"·"폼" 렌더 조건을 배타적으로 유지한다. */
type DetailState =
  | { status: "loading" }
  | { status: "loaded"; product: AdminProductDetail }
  | { status: "error"; message: string };

export default function EditAdminProductPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const productId = Number(params.id);
  const hasValidId = Number.isInteger(productId);

  const [state, setState] = useState<DetailState>(() =>
    Number.isInteger(Number(params.id))
      ? { status: "loading" }
      : { status: "error", message: "잘못된 상품 주소예요" },
  );
  const [deleting, setDeleting] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!hasValidId) return;

    let cancelled = false;
    getAdminProduct(productId)
      .then((product) => {
        if (!cancelled) setState({ status: "loaded", product });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setState({
          status: "error",
          message: err instanceof ApiError ? err.message : "상품을 불러오지 못했어요",
        });
      });

    return () => {
      cancelled = true;
    };
  }, [productId, hasValidId]);

  function showToast(message: string) {
    setToastMessage(message);
    setTimeout(() => setToastMessage(null), 1800);
  }

  function handleSubmit(request: AdminProductRequest) {
    return updateAdminProduct(productId, request);
  }

  function handleSaved(saved: AdminProductDetail) {
    setState({ status: "loaded", product: saved });
    showToast("저장했어요");
  }

  async function handleDelete() {
    if (
      !window.confirm(
        "이 상품을 삭제할까요? 고객 화면에서는 사라지지만 기존 주문 이력에는 그대로 남아요",
      )
    ) {
      return;
    }

    setDeleting(true);
    try {
      await deleteAdminProduct(productId);
      router.push("/admin");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "삭제에 실패했어요");
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="relative flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <Link href="/admin" className="text-body-sm text-muted underline">
          상품 목록으로
        </Link>
        <h1 className="text-title text-ink">
          {state.status === "loaded" ? state.product.name : "상품 수정"}
        </h1>
      </div>

      {state.status === "loading" && (
        <p className="text-body text-muted py-16 text-center">불러오는 중이에요</p>
      )}

      {state.status === "error" && (
        <p className="text-body text-error py-16 text-center">{state.message}</p>
      )}

      {state.status === "loaded" && (
        <AdminProductForm
          product={state.product}
          submitLabel="저장하기"
          onSubmit={handleSubmit}
          onSaved={handleSaved}
          onUnhandledError={showToast}
          extraActions={
            <Button variant="ghost" disabled={deleting} onClick={handleDelete}>
              상품 삭제
            </Button>
          }
        />
      )}

      {toastMessage && <Toast message={toastMessage} visible={!!toastMessage} />}
    </div>
  );
}
