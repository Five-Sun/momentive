"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { IconButton } from "@/components/core/IconButton";
import { ProductGridItem } from "@/components/commerce/ProductGridItem";
import { ProductCardSkeleton } from "@/components/skeleton/ProductCardSkeleton";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import { getProducts, type ProductSummary } from "@/lib/api/products";

const PAGE_SIZE = 20;

export default function Home() {
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [pageToLoad, setPageToLoad] = useState(0);
  const [loadedPage, setLoadedPage] = useState<number | null>(null);
  const [totalPages, setTotalPages] = useState<number | null>(null);

  const loading = loadedPage !== pageToLoad;
  const initialLoadDone = loadedPage !== null;

  useEffect(() => {
    let cancelled = false;

    getProducts(pageToLoad, PAGE_SIZE).then((response) => {
      if (cancelled) return;
      setProducts((prev) => (pageToLoad === 0 ? response.content : [...prev, ...response.content]));
      setTotalPages(response.totalPages);
      setLoadedPage(pageToLoad);
    });

    return () => {
      cancelled = true;
    };
  }, [pageToLoad]);

  const hasMore = totalPages == null ? true : pageToLoad + 1 < totalPages;

  const sentinelRef = useInfiniteScroll({
    hasMore,
    loading,
    onLoadMore: () => setPageToLoad((prev) => prev + 1),
  });

  return (
    <main className="bg-canvas flex min-h-screen flex-col">
      <header className="flex flex-col gap-4 px-4 py-5">
        <div className="flex items-center gap-3">
          <Image
            src="/logo/momentive-logo.jpeg"
            alt="momentive"
            width={40}
            height={40}
            className="rounded-full"
          />
          <Link href="/search" className="min-w-0 flex-1">
            <div className="bg-surface-soft border-hairline flex h-12 items-center gap-2 rounded-full border px-[18px]">
              <span className="text-muted">⌕</span>
              <span className="text-body text-muted flex-1 truncate">브랜드, 상품 검색</span>
            </div>
          </Link>
          <Link href="/cart">
            <IconButton>🛍</IconButton>
          </Link>
        </div>
        <p className="text-display-md text-ink">소중한 순간을 위한 옷</p>
      </header>

      <section className="flex-1 px-4 pb-20">
        {initialLoadDone && products.length === 0 && !loading ? (
          <div className="flex flex-col items-center justify-center gap-2 py-24 text-center">
            <p className="text-title text-ink">아직 준비된 상품이 없어요</p>
            <p className="text-body-sm text-muted">곧 다양한 상품으로 찾아올게요</p>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-2 gap-4">
              {products.map((product) => (
                <ProductGridItem key={product.id} product={product} />
              ))}
              {loading &&
                Array.from({ length: PAGE_SIZE }).map((_, i) => <ProductCardSkeleton key={`skeleton-${i}`} />)}
            </div>
            {hasMore && <div ref={sentinelRef} className="h-1 w-full" />}
          </>
        )}
      </section>
    </main>
  );
}
