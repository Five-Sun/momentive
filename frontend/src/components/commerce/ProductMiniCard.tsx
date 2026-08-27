"use client";

import { useState } from "react";
import Link from "next/link";
import type { ProductSummary } from "@/lib/api/products";

function formatWon(amount: number) {
  return `${amount.toLocaleString("ko-KR")}원`;
}

interface ProductMiniCardProps {
  product: ProductSummary;
  rank?: number;
}

export function ProductMiniCard({ product, rank }: ProductMiniCardProps) {
  const [imageFailed, setImageFailed] = useState(false);
  const showPlaceholder = imageFailed || !product.thumbnailUrl;
  const effectivePrice = !product.soldOut && product.discountPrice != null ? product.discountPrice : product.price;

  return (
    <Link href={`/products/${product.id}`} className="flex w-32 shrink-0 flex-col gap-1.5">
      <div className="bg-surface-strong relative aspect-square overflow-hidden rounded-md">
        {showPlaceholder ? (
          <div className="text-muted flex h-full items-center justify-center p-2 text-center text-xs">
            {product.name}
          </div>
        ) : (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={product.thumbnailUrl!}
            alt={product.name}
            className="h-full w-full object-cover"
            onError={() => setImageFailed(true)}
          />
        )}
        {rank && (
          <div className="bg-ink text-tag absolute top-2 left-2 flex h-[22px] w-[22px] items-center justify-center rounded-full text-white">
            {rank}
          </div>
        )}
      </div>
      <span className="text-caption text-ink truncate font-semibold">{product.name}</span>
      <span className="text-body-sm text-ink font-bold">{formatWon(effectivePrice)}</span>
    </Link>
  );
}
