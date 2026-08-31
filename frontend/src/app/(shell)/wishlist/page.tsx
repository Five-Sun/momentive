"use client";

import { useEffect, useState } from "react";
import { ProductCard } from "@/components/commerce/ProductCard";
import { Badge } from "@/components/core/Badge";
import { Rating } from "@/components/commerce/Rating";
import { Button } from "@/components/core/Button";
import { useRouter } from "next/navigation";
import { getProducts, type ProductSummary } from "@/lib/api/products";
import { getWishlist, toggleWishlist } from "@/lib/storage/wishlist";

function formatWon(amount: number) {
  return `${amount.toLocaleString("ko-KR")}원`;
}

export default function WishlistPage() {
  const router = useRouter();
  const [wishlistIds, setWishlistIds] = useState<number[]>([]);
  const [allProducts, setAllProducts] = useState<ProductSummary[] | null>(null);

  useEffect(() => {
    Promise.resolve().then(() => setWishlistIds(getWishlist()));
    getProducts(0, 100).then((response) => setAllProducts(response.content));
  }, []);

  function handleToggle(id: number) {
    setWishlistIds(toggleWishlist(id));
  }

  const items = allProducts?.filter((p) => wishlistIds.includes(p.id)) ?? [];

  return (
    <main className="bg-canvas flex min-h-screen flex-col">
      <header className="flex items-center justify-center px-4 py-4">
        <span className="text-title-sm text-ink">위시리스트</span>
      </header>

      {allProducts !== null && items.length === 0 ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-4 py-20">
          <span className="text-body text-muted">위시리스트가 비어 있어요</span>
          <Button variant="secondary" onClick={() => router.push("/")}>
            쇼핑하러 가기
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-4 px-4 pb-20">
          {items.map((product) => {
            const hasDiscount = !product.soldOut && product.discountPrice != null;
            const badge = product.soldOut ? (
              <Badge tone="soldout" label="품절" />
            ) : hasDiscount ? (
              <Badge
                tone="sale"
                label={`${Math.round((1 - product.discountPrice! / product.price) * 100)}%`}
              />
            ) : undefined;

            return (
              <div key={product.id} onClick={() => router.push(`/products/${product.id}`)}>
                <ProductCard
                  image={
                    product.thumbnailUrl ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img
                        src={product.thumbnailUrl}
                        alt={product.name}
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <div className="bg-surface-strong text-muted flex h-full items-center justify-center p-2 text-center text-xs">
                        {product.name}
                      </div>
                    )
                  }
                  title={product.name}
                  price={formatWon(hasDiscount ? product.discountPrice! : product.price)}
                  originalPrice={hasDiscount ? formatWon(product.price) : undefined}
                  badge={badge}
                  favorited={true}
                  onToggleFavorite={() => handleToggle(product.id)}
                  rating={
                    product.averageRating != null ? (
                      <Rating value={product.averageRating} count={product.reviewCount} />
                    ) : undefined
                  }
                />
              </div>
            );
          })}
        </div>
      )}
    </main>
  );
}
