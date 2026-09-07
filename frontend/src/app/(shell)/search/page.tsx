"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { Search, ChevronDown } from "lucide-react";
import { Button } from "@/components/core/Button";
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

const RESULT_PAGE_SIZE = 100;
const SUGGESTION_SIZE = 5;
/** 타이핑마다 자동완성 API를 때리지 않도록 하는 지연(ms). 호출량을 줄이는 목적. */
const SUGGESTION_DEBOUNCE_MS = 250;

/**
 * 화면 상태 3종. 스펙의 "미입력 / 입력 중 / 검색 실행됨"에 1:1로 대응하며, 하나의 값으로
 * 계산해 서로 배타적임을 구조적으로 보장한다(조건을 각각 조합하다 겹쳤던 과거 회귀 방지).
 */
type ViewState = "idle" | "typing" | "results";

function SearchPageInner() {
  const searchParams = useSearchParams();
  const initialCategory = searchParams.get("category");
  const category = isCategory(initialCategory) ? initialCategory : undefined;
  /** 데스크톱 상단 네비(`TopNav`)가 `/search?q=...`로 넘겨주는 검색어. */
  const queryParam = searchParams.get("q")?.trim() ?? "";

  const [query, setQuery] = useState(queryParam);
  const [submitted, setSubmitted] = useState(Boolean(category) || Boolean(queryParam));
  /** 실제로 검색을 실행한 시점의 검색어. 입력 중인 `query`와 분리해 결과가 흔들리지 않게 한다. */
  const [submittedQuery, setSubmittedQuery] = useState(queryParam);
  const [sheetOpen, setSheetOpen] = useState(false);
  const [sortLabel, setSortLabel] = useState<SortLabel>("인기순");
  const [recentSearches, setRecentSearches] = useState<string[]>([]);

  /** 어떤 검색어에 대한 자동완성인지 함께 들고 있어, 이전 검색어의 결과가 잠깐 비치지 않게 한다. */
  const [suggestions, setSuggestions] = useState<{ term: string; items: ProductSummary[] }>({
    term: "",
    items: [],
  });
  const [results, setResults] = useState<ProductSummary[] | null>(null);
  const [resultsFailed, setResultsFailed] = useState(false);
  const [retryToken, setRetryToken] = useState(0);

  const trimmedQuery = query.trim();
  const viewState: ViewState = submitted ? "results" : trimmedQuery ? "typing" : "idle";
  const suggestionItems = suggestions.term === trimmedQuery ? suggestions.items : [];

  useEffect(() => {
    let cancelled = false;
    Promise.resolve().then(() => {
      if (!cancelled) setRecentSearches(getRecentSearches());
    });
    return () => {
      cancelled = true;
    };
  }, []);

  // `TopNav`가 `/search?q=...`로 이동시킬 때, 이미 이 화면에 있으면 컴포넌트가 다시
  // 마운트되지 않는다. 그래서 초기 state만으로는 두 번째 검색부터 반영되지 않으므로
  // 파라미터 변화에 반응해 검색을 실행한다.
  useEffect(() => {
    if (!queryParam) return;
    let cancelled = false;
    Promise.resolve().then(() => {
      if (cancelled) return;
      setQuery(queryParam);
      setSubmittedQuery(queryParam);
      setSubmitted(true);
      setRecentSearches(recordRecentSearch(queryParam));
    });
    return () => {
      cancelled = true;
    };
  }, [queryParam]);

  // 검색 결과: 서버가 `q`로 필터링한다. 클라이언트에서 다시 거르지 않으므로
  // 상품이 100개를 넘어도 이후 상품이 검색에서 누락되지 않는다.
  useEffect(() => {
    if (viewState !== "results") return;
    let cancelled = false;
    // 이 프로젝트의 다른 화면과 같은 패턴 — effect 본문에서 곧바로 setState하면
    // react-hooks/set-state-in-effect에 걸리므로 마이크로태스크로 미룬다.
    Promise.resolve().then(() => {
      if (cancelled) return;
      setResults(null);
      setResultsFailed(false);
    });
    getProducts(0, RESULT_PAGE_SIZE, {
      category,
      sort: SORT_LABEL_TO_VALUE[sortLabel],
      q: submittedQuery || undefined,
    })
      .then((response) => {
        if (!cancelled) setResults(response.content);
      })
      .catch((error) => {
        if (cancelled) return;
        // 실패를 빈 결과로 뭉개면 "검색 결과가 없어요"로 잘못 안내된다. 원인은 콘솔에 남기고
        // 화면에는 결과 없음과 구분되는 실패 안내 + 재시도를 노출한다.
        console.error("상품 검색 실패", error);
        setResultsFailed(true);
      });
    return () => {
      cancelled = true;
    };
  }, [viewState, category, sortLabel, submittedQuery, retryToken]);

  // 자동완성: 같은 API를 작은 size로 호출한다.
  useEffect(() => {
    if (viewState !== "typing") return;
    let cancelled = false;
    const timer = setTimeout(() => {
      getProducts(0, SUGGESTION_SIZE, { category, q: trimmedQuery })
        .then((response) => {
          if (!cancelled) setSuggestions({ term: trimmedQuery, items: response.content });
        })
        .catch((error) => {
          if (cancelled) return;
          // 자동완성은 보조 UI라 실패해도 화면을 막지 않는다. 다만 조용히 삼키지는 않는다.
          console.error("검색어 자동완성 조회 실패", error);
          setSuggestions({ term: trimmedQuery, items: [] });
        });
    }, SUGGESTION_DEBOUNCE_MS);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [viewState, trimmedQuery, category]);

  function runSearch(term: string) {
    const trimmed = term.trim();
    if (!trimmed) return;
    setQuery(trimmed);
    setSubmittedQuery(trimmed);
    setSubmitted(true);
    setRecentSearches(recordRecentSearch(trimmed));
  }

  return (
    <main className="bg-canvas relative flex min-h-screen flex-col">
      <div className="border-hairline flex items-center gap-2.5 border-b px-4 py-2.5 lg:px-0">
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

      {viewState === "typing" && suggestionItems.length > 0 && (
        <div className="border-hairline shadow-card bg-surface-card absolute inset-x-0 top-[52px] z-20 border-b lg:inset-x-0 lg:rounded-md lg:border">
          {suggestionItems.map((p) => (
            <button
              key={p.id}
              onClick={() => runSearch(p.name)}
              className="text-body-sm text-ink flex w-full items-center gap-2 px-4 py-2.5 text-left lg:px-4"
            >
              <Search className="text-muted h-3.5 w-3.5" />
              {p.name}
            </button>
          ))}
        </div>
      )}

      <div className="flex-1 px-4 py-4 lg:px-0">
        {viewState === "idle" && (
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

        {viewState === "results" && (
          <div>
            <div className="mb-3 flex items-center justify-between">
              <span className="text-caption text-muted">
                {resultsFailed
                  ? "검색 실패"
                  : results == null
                    ? "검색 중"
                    : `${results.length}개 결과`}
              </span>
              <button
                onClick={() => setSheetOpen(true)}
                className="border-hairline text-caption text-ink flex items-center gap-1 rounded-full border px-3 py-1.5"
              >
                {sortLabel}
                <ChevronDown className="h-3 w-3" />
              </button>
            </div>
            {resultsFailed ? (
              <div className="flex flex-col items-center justify-center gap-3 py-24 text-center">
                <p className="text-title text-ink">검색에 실패했어요</p>
                <p className="text-body-sm text-muted">
                  일시적인 문제일 수 있어요. 잠시 후 다시 시도해주세요
                </p>
                <Button variant="secondary" onClick={() => setRetryToken((n) => n + 1)}>
                  다시 시도
                </Button>
              </div>
            ) : results == null ? (
              <div className="grid grid-cols-2 gap-4 lg:grid-cols-3 lg:gap-10 xl:grid-cols-4">
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
              <div className="grid grid-cols-2 gap-4 lg:grid-cols-3 xl:grid-cols-4">
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
