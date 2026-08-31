"use client";

import { useState } from "react";
import Link from "next/link";
import { ProductCard } from "@/components/commerce/ProductCard";
import { Badge } from "@/components/core/Badge";
import { Rating } from "@/components/commerce/Rating";
import type { ProductSummary } from "@/lib/api/products";

function formatWon(amount: number) {
  return `${amount.toLocaleString("ko-KR")}원`;
}

interface ProductGridItemProps {
  product: ProductSummary;
}

export function ProductGridItem({ product }: ProductGridItemProps) {
  const [imageFailed, setImageFailed] = useState(false);

  const hasDiscount = !product.soldOut && product.discountPrice != null;
  const badge = product.soldOut ? (
    <Badge tone="soldout" label="품절" />
  ) : hasDiscount ? (
    <Badge
      tone="sale"
      label={`${Math.round((1 - product.discountPrice! / product.price) * 100)}%`}
    />
  ) : undefined;

  const showPlaceholder = imageFailed || !product.thumbnailUrl;

  return (
    <Link href={`/products/${product.id}`}>
      <ProductCard
        image={
          showPlaceholder ? (
            <div className="bg-surface-strong text-muted flex h-full items-center justify-center p-2 text-center text-xs">
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
          )
        }
        title={product.name}
        price={formatWon(hasDiscount ? product.discountPrice! : product.price)}
        originalPrice={hasDiscount ? formatWon(product.price) : undefined}
        badge={badge}
        favorited={false}
        rating={
          product.averageRating != null ? (
            <Rating value={product.averageRating} count={product.reviewCount} />
          ) : undefined
        }
      />
    </Link>
  );
}
