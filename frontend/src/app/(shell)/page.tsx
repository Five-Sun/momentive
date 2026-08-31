"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { Search, ShoppingBag } from "lucide-react";
import { IconButton } from "@/components/core/IconButton";
import { Chip } from "@/components/core/Chip";
import { ProductGridItem } from "@/components/commerce/ProductGridItem";
import { ProductMiniCard } from "@/components/commerce/ProductMiniCard";
import { ProductCardSkeleton } from "@/components/skeleton/ProductCardSkeleton";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import { getProducts, getProduct, type ProductSummary, type Category } from "@/lib/api/products";
import { getRecentlyViewed } from "@/lib/storage/recentlyViewed";

const PAGE_SIZE = 20;

const CATEGORY_CHIPS: { key: Category | "ALL"; label: string }[] = [
  { key: "ALL", label: "전체" },
  { key: "OUTER", label: "아우터" },
  { key: "KNIT", label: "니트" },
  { key: "INNERWEAR", label: "이너웨어" },
  { key: "ACCESSORY", label: "악세서리" },
];

export default function Home() {
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [pageToLoad, setPageToLoad] = useState(0);
  const [loadedPage, setLoadedPage] = useState<number | null>(null);
  const [totalPages, setTotalPages] = useState<number | null>(null);
  const [category, setCategory] = useState<Category | "ALL">("ALL");

  const [ranked, setRanked] = useState<ProductSummary[]>([]);
  const [recentlyViewed, setRecentlyViewed] = useState<ProductSummary[]>([]);

  const loading = loadedPage !== pageToLoad;
  const initialLoadDone = loadedPage !== null;

  useEffect(() => {
    let cancelled = false;

    getProducts(pageToLoad, PAGE_SIZE, { category: category === "ALL" ? undefined : category }).then(
      (response) => {
        if (cancelled) return;
        setProducts((prev) => (pageToLoad === 0 ? response.content : [...prev, ...response.content]));
        setTotalPages(response.totalPages);
        setLoadedPage(pageToLoad);
      }
    );

    return () => {
      cancelled = true;
    };
  }, [pageToLoad, category]);

  function selectCategory(next: Category | "ALL") {
    if (next === category) return;
    setCategory(next);
    setProducts([]);
    setLoadedPage(null);
    setTotalPages(null);
    setPageToLoad(0);
  }

  // "지금 인기 있는" — 실 리뷰 집계가 없어, sort=popular(현재는 신상순과 동일 동작)의 상위 4개를 인기 기준으로 대체 사용한다.
  // 카테고리 필터와 무관하게 항상 전체 상품 기준으로 노출한다.
  useEffect(() => {
    let cancelled = false;
    getProducts(0, 4, { sort: "popular" }).then((response) => {
      if (!cancelled) setRanked(response.content);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  // "최근 본 상품" — 방문 기록(id 목록)은 Phase 0의 localStorage 유틸에 있음. 상세 API로 개별 조회해 렌더링한다.
  // 방문 시 실제 기록 로직은 Phase 3(상품상세)에서 추가된다.
  useEffect(() => {
    let cancelled = false;
    const ids = getRecentlyViewed();
    if (ids.length === 0) return;
    Promise.all(ids.map((id) => getProduct(id))).then((results) => {
      if (cancelled) return;
      const found = results.filter((product): product is NonNullable<typeof product> => product != null);
      setRecentlyViewed(
        found.map((product) => ({
          id: product.id,
          name: product.name,
          price: product.price,
          discountPrice: product.discountPrice,
          soldOut: product.soldOut,
          category: product.category,
          thumbnailUrl: product.images[0]?.url ?? null,
          averageRating: product.averageRating,
          reviewCount: product.reviewCount,
        }))
      );
    });
    return () => {
      cancelled = true;
    };
  }, []);

  const hasMore = totalPages == null ? true : pageToLoad + 1 < totalPages;

  const sentinelRef = useInfiniteScroll({
    hasMore,
    loading,
    onLoadMore: () => setPageToLoad((prev) => prev + 1),
  });

  return (
    <main className="bg-canvas flex min-h-screen flex-col">
      <header className="flex flex-col gap-3 px-4 py-4">
        <div className="flex items-center justify-between">
          <Image
            src="/logo/momentive-logo.jpeg"
            alt="momentive"
            width={32}
            height={32}
            className="rounded-full"
          />
          <Link href="/cart">
            <IconButton>
              <ShoppingBag className="h-5 w-5" />
            </IconButton>
          </Link>
        </div>
        <Link href="/search">
          <div className="bg-surface-soft border-hairline flex h-11 items-center gap-2 rounded-full border px-4">
            <Search className="text-muted h-4 w-4" />
            <span className="text-body-sm text-muted flex-1 truncate">브랜드, 상품 검색</span>
          </div>
        </Link>
      </header>

      <section className="flex-1 pb-20">
        <div className="from-brand-pink-soft to-brand-pink-tint mx-4 mb-[18px] flex flex-col gap-1.5 rounded-lg bg-linear-to-br p-5">
          <span className="text-caption text-brand-pink-deep font-bold">WINTER SALE</span>
          <span className="text-display-md text-ink">겨울 신상 최대 20%</span>
          <span className="text-body-sm text-body">소중한 우리 강아지를 위한 첫 겨울 옷</span>
        </div>

        {ranked.length > 0 && (
          <div className="mb-5">
            <div className="flex items-baseline justify-between px-4 pb-2.5">
              <span className="text-title-sm text-ink">지금 인기 있는</span>
              <span className="text-caption text-muted">리뷰 많은순</span>
            </div>
            <div className="flex gap-3 overflow-x-auto px-4 pb-1">
              {ranked.map((product, i) => (
                <ProductMiniCard key={product.id} product={product} rank={i + 1} />
              ))}
            </div>
          </div>
        )}

        <div className="flex gap-2 overflow-x-auto px-4 pb-3.5">
          {CATEGORY_CHIPS.map((chip) => (
            <Chip
              key={chip.key}
              label={chip.label}
              selected={category === chip.key}
              onClick={() => selectCategory(chip.key)}
            />
          ))}
        </div>

        <div className="px-4">
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
        </div>

        {recentlyViewed.length > 0 && (
          <div className="mt-6">
            <div className="px-4 pb-2.5">
              <span className="text-title-sm text-ink">최근 본 상품</span>
            </div>
            <div className="flex gap-3 overflow-x-auto px-4">
              {recentlyViewed.map((product) => (
                <ProductMiniCard key={product.id} product={product} />
              ))}
            </div>
          </div>
        )}
      </section>
    </main>
  );
}
