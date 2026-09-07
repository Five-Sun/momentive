"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Toast } from "@/components/feedback/Toast";
import { createAdminProduct, type AdminProductDetail } from "@/lib/api/admin";
import { AdminProductForm } from "../AdminProductForm";

export default function NewAdminProductPage() {
  const router = useRouter();
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  function showToast(message: string) {
    setToastMessage(message);
    setTimeout(() => setToastMessage(null), 1800);
  }

  function handleSaved(saved: AdminProductDetail) {
    // 등록 직후 수정 화면으로 옮겨, 서버가 채운 상품·variant·이미지 id를 그대로 이어받는다.
    router.replace(`/admin/products/${saved.id}`);
  }

  return (
    <div className="relative flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <Link href="/admin" className="text-body-sm text-muted underline">
          상품 목록으로
        </Link>
        <h1 className="text-title text-ink">상품 등록</h1>
      </div>

      <AdminProductForm
        product={null}
        submitLabel="등록하기"
        onSubmit={createAdminProduct}
        onSaved={handleSaved}
        onUnhandledError={showToast}
      />

      {toastMessage && <Toast message={toastMessage} visible={!!toastMessage} />}
    </div>
  );
}
