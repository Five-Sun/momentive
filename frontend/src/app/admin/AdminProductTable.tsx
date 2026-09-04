"use client";

import Link from "next/link";
import { Badge } from "@/components/core/Badge";
import { ProductImage } from "@/components/commerce/ProductImage";
import { CATEGORY_LABEL } from "@/lib/categories";
import type { AdminProductSummary } from "@/lib/api/admin";
import { PRODUCT_STATUS_LABEL, PRODUCT_STATUS_TONE } from "./productStatus";

/**
 * 관리자 상품 목록 표. 표는 고객 화면에 없는 패턴이라 공용 `src/components/`로 올리지 않고
 * `/admin` 안의 로컬 컴포넌트로 둔다(spec 명시).
 */

function formatWon(amount: number) {
  return `${amount.toLocaleString("ko-KR")}원`;
}

interface AdminProductTableProps {
  products: AdminProductSummary[];
}

export function AdminProductTable({ products }: AdminProductTableProps) {
  return (
    <div className="border-hairline bg-surface-card overflow-x-auto rounded-md border">
      <table className="w-full min-w-[860px] border-collapse">
        <thead>
          <tr className="border-hairline bg-surface-soft border-b">
            <th className="text-caption text-muted w-20 px-4 py-3 text-left">썸네일</th>
            <th className="text-caption text-muted px-4 py-3 text-left">이름</th>
            <th className="text-caption text-muted w-28 px-4 py-3 text-left">카테고리</th>
            <th className="text-caption text-muted w-40 px-4 py-3 text-right">가격</th>
            <th className="text-caption text-muted w-24 px-4 py-3 text-right">재고 합</th>
            <th className="text-caption text-muted w-24 px-4 py-3 text-left">상태</th>
            <th className="text-caption text-muted w-20 px-4 py-3 text-left">수정</th>
          </tr>
        </thead>
        <tbody>
          {products.map((product) => (
            <tr key={product.id} className="border-hairline-soft border-b last:border-b-0">
              <td className="px-4 py-3">
                <div className="h-14 w-14">
                  {product.thumbnailUrl ? (
                    <ProductImage url={product.thumbnailUrl} name={product.name} />
                  ) : (
                    <div className="bg-surface-strong text-caption text-muted flex aspect-square items-center justify-center rounded-2xl">
                      없음
                    </div>
                  )}
                </div>
              </td>
              <td className="text-body-sm text-ink px-4 py-3 font-semibold">{product.name}</td>
              <td className="text-body-sm text-body px-4 py-3">
                {CATEGORY_LABEL[product.category]}
              </td>
              <td className="text-body-sm text-ink px-4 py-3 text-right">
                {product.discountPrice != null ? (
                  <span className="flex flex-col items-end">
                    <span className="text-sale font-semibold">
                      {formatWon(product.discountPrice)}
                    </span>
                    <span className="text-caption text-muted line-through">
                      {formatWon(product.price)}
                    </span>
                  </span>
                ) : (
                  formatWon(product.price)
                )}
              </td>
              <td className="text-body-sm text-ink px-4 py-3 text-right">
                {product.totalStock.toLocaleString("ko-KR")}
              </td>
              <td className="px-4 py-3">
                <Badge
                  label={PRODUCT_STATUS_LABEL[product.status]}
                  tone={PRODUCT_STATUS_TONE[product.status]}
                />
              </td>
              <td className="px-4 py-3">
                <Link
                  href={`/admin/products/${product.id}`}
                  className="text-body-sm text-ink font-semibold underline"
                >
                  수정
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
