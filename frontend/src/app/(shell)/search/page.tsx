"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { Search, ChevronDown } from "lucide-react";
import { Chip } from "@/components/core/Chip";
import { FilterSheet } from "@/components/commerce/FilterSheet";
import { ProductGridItem } from "@/components/commerce/ProductGridItem";
import { ProductCardSkeleton } from "@/components/skeleton/ProductCardSkeleton";
import { getProducts, type ProductSummary, type ProductSort } from "@/lib/api/products";
import { isCategory } from "@/lib/categories";
import { getRecentSearches, recordRecentSearch } from "@/lib/storage/recentSearches";

const SORT_OPTIONS = ["인기순", "신상순", "낮은 가격순", "높은 가격순"] as const;
type SortLabel = (typeof SORT_OPTIONS)[number];

const SORT_LABEL_TO_VALUE: Record<SortLabel, ProductSort> = {
  "인기순": "popular",
  "신상순": "new",
  "낮은 가격순": "price_asc",
  "높은 가격순": "price_desc",
};

// 인기 검색어: 실 집계 인프라가 없어 하드코딩 목업 (핸드오프 원본 그대로)
const POPULAR_SEARCHES = ["겨울 아우터", "강아지 한복", "스트라이프 티"];

function SearchPageInner() {
  const searchParams = useSearchParams();
  const initialCategory = searchParams.get("category");
  const category = isCategory(initialCategory) ? initialCategory : undefined;

  const [query, setQuery] = useState("");
  const [submitted, setSubmitted] = useState(Boolean(category));
  const [sheetOpen, setSheetOpen] = useState(false);
  const [sortLabel, setSortLabel] = useState<SortLabel>("인기순");
  const [recentSearches, setRecentSearches] = useState<string[]>([]);
  const [allProducts, setAllProducts] = useState<ProductSummary[] | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.resolve().then(() => {
      if (!cancelled) setRecentSearches(getRecentSearches());
    });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    Promise.resolve().then(() => {
      if (!cancelled) setAllProducts(null);
    });
    getProducts(0, 100, { category, sort: SORT_LABEL_TO_VALUE[sortLabel] }).then((response) => {
      if (!cancelled) setAllProducts(response.content);
    });
    return () => {
      cancelled = true;
    };
  }, [category, sortLabel]);

  function runSearch(term: string) {
    setQuery(term);
    setSubmitted(true);
    setRecentSearches(recordRecentSearch(term));
  }

  const suggestions = useMemo(() => {
    if (submitted || !query || !allProducts) return [];
    return allProducts.filter((p) => p.name.includes(query)).slice(0, 5);
  }, [submitted, query, allProducts]);

  const results = useMemo(() => {
    if (!submitted || !allProducts) return null;
    if (!query) return allProducts;
    return allProducts.filter((p) => p.name.includes(query));
  }, [submitted, query, allProducts]);

  return (
    <main className="bg-canvas relative flex min-h-screen flex-col">
      <div className="border-hairline flex items-center gap-2.5 border-b px-4 py-2.5">
        <div className="bg-surface-soft border-hairline flex h-10 flex-1 items-center gap-2 rounded-full border px-3.5">
          <Search className="text-muted h-4 w-4" />
          <input
            autoFocus
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setSubmitted(false);
            }}
            onKeyDown={(e) => e.key === "Enter" && runSearch(query)}
            placeholder="브랜드, 상품 검색"
            className="text-body-sm text-ink flex-1 border-none bg-transparent outline-none placeholder:text-muted"
          />
        </div>
      </div>

      {suggestions.length > 0 && (
        <div className="border-hairline shadow-card bg-surface-card absolute inset-x-0 top-[52px] z-20 border-b">
          {suggestions.map((p) => (
            <button
              key={p.id}
              onClick={() => runSearch(p.name)}
              className="text-body-sm text-ink flex w-full items-center gap-2 px-4 py-2.5 text-left"
            >
              <Search className="text-muted h-3.5 w-3.5" />
              {p.name}
            </button>
          ))}
        </div>
      )}

      <div className="flex-1 px-4 py-4">
        {!submitted && !query && (
          <div className="flex flex-col gap-5">
            {recentSearches.length > 0 && (
              <div>
                <span className="text-caption text-muted">최근 검색어</span>
                <div className="mt-2 flex flex-wrap gap-2">
                  {recentSearches.map((term) => (
                    <Chip key={term} label={term} onClick={() => runSearch(term)} />
                  ))}
                </div>
              </div>
            )}
            <div>
              <span className="text-caption text-muted">인기 검색어</span>
              <div className="mt-2 flex flex-col gap-0.5">
                {POPULAR_SEARCHES.map((term, i) => (
                  <button
                    key={term}
                    onClick={() => runSearch(term)}
                    className="text-body-sm text-ink flex h-9 items-center gap-2.5"
                  >
                    <span className="text-brand-pink-active w-3.5 font-bold">{i + 1}</span>
                    {term}
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {submitted && (
          <div>
            <div className="mb-3 flex items-center justify-between">
              <span className="text-caption text-muted">
                {results == null ? "검색 중" : `${results.length}개 결과`}
              </span>
              <button
                onClick={() => setSheetOpen(true)}
                className="border-hairline text-caption text-ink flex items-center gap-1 rounded-full border px-3 py-1.5"
              >
                {sortLabel}
                <ChevronDown className="h-3 w-3" />
              </button>
            </div>
            {results == null ? (
              <div className="grid grid-cols-2 gap-4">
                {Array.from({ length: 4 }).map((_, i) => (
                  <ProductCardSkeleton key={i} />
                ))}
              </div>
            ) : results.length === 0 ? (
              <div className="flex flex-col items-center justify-center gap-2 py-24 text-center">
                <p className="text-title text-ink">검색 결과가 없어요</p>
                <p className="text-body-sm text-muted">다른 검색어로 시도해보세요</p>
              </div>
            ) : (
              <div className="grid grid-cols-2 gap-4">
                {results.map((p) => (
                  <ProductGridItem key={p.id} product={p} />
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      <FilterSheet
        open={sheetOpen}
        sortOptions={[...SORT_OPTIONS]}
        selected={sortLabel}
        onSelect={(option) => setSortLabel(option as SortLabel)}
        onApply={() => setSheetOpen(false)}
        onClose={() => setSheetOpen(false)}
      />
    </main>
  );
}

export default function SearchPage() {
  return (
    <Suspense fallback={null}>
      <SearchPageInner />
    </Suspense>
  );
}
