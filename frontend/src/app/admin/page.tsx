"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Plus } from "lucide-react";
import { Button } from "@/components/core/Button";
import { SearchInput } from "@/components/forms/SearchInput";
import { ApiError } from "@/lib/api/client";
import { getAdminProducts, type AdminProductSummary } from "@/lib/api/admin";
import { AdminProductTable } from "./AdminProductTable";
import { STATUS_FILTER_OPTIONS } from "./productStatus";

const PAGE_SIZE = 20;

interface ListData {
  products: AdminProductSummary[];
  totalPages: number;
  totalElements: number;
}

/**
 * 조회 상태는 하나의 유니온으로 둔다 — "불러오는 중"·"실패"·"결과 없음"이 서로 섞여 보이지
 * 않도록 렌더 조건을 배타적으로 유지하기 위함이다.
 */
type ListState =
  | { status: "loading" }
  | { status: "loaded"; data: ListData }
  | { status: "error"; message: string };

export default function AdminProductListPage() {
  const router = useRouter();

  const [state, setState] = useState<ListState>({ status: "loading" });
  const [page, setPage] = useState(0);
  const [filterKey, setFilterKey] = useState(STATUS_FILTER_OPTIONS[0].key);
  const [keywordInput, setKeywordInput] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");

  useEffect(() => {
    const statuses =
      STATUS_FILTER_OPTIONS.find((option) => option.key === filterKey)?.statuses ??
      STATUS_FILTER_OPTIONS[0].statuses;

    let cancelled = false;

    getAdminProducts({ page, size: PAGE_SIZE, statuses, q: appliedKeyword })
      .then((response) => {
        if (cancelled) return;
        setState({
          status: "loaded",
          data: {
            products: response.content,
            totalPages: response.totalPages,
            totalElements: response.totalElements,
          },
        });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        // 조회 실패와 "결과 없음"은 다른 상태다 — 실패는 실패라고 표시한다.
        setState({
          status: "error",
          message: err instanceof ApiError ? err.message : "상품 목록을 불러오지 못했어요",
        });
      });

    return () => {
      cancelled = true;
    };
  }, [page, filterKey, appliedKeyword]);

  function goToPage(next: number) {
    setState({ status: "loading" });
    setPage(next);
  }

  function applyKeyword() {
    const keyword = keywordInput.trim();
    if (keyword === appliedKeyword && page === 0) return;
    setState({ status: "loading" });
    setPage(0);
    setAppliedKeyword(keyword);
  }

  function changeFilter(key: string) {
    if (key === filterKey && page === 0) return;
    setState({ status: "loading" });
    setPage(0);
    setFilterKey(key);
  }

  const loaded = state.status === "loaded" ? state.data : null;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div className="flex flex-col gap-1">
          <h1 className="text-title text-ink">상품 관리</h1>
          <span className="text-body-sm text-muted">
            {loaded ? `전체 ${loaded.totalElements.toLocaleString("ko-KR")}개` : " "}
          </span>
        </div>
        <Button
          variant="primary"
          icon={<Plus className="h-4 w-4" />}
          onClick={() => router.push("/admin/products/new")}
        >
          상품 등록
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <form
          className="w-full max-w-[360px]"
          onSubmit={(event) => {
            event.preventDefault();
            applyKeyword();
          }}
        >
          <SearchInput
            value={keywordInput}
            onChange={setKeywordInput}
            placeholder="상품명으로 검색"
          />
        </form>

        <div className="flex flex-wrap gap-2">
          {STATUS_FILTER_OPTIONS.map((option) => (
            <button
              key={option.key}
              type="button"
              onClick={() => changeFilter(option.key)}
              className={`text-body-sm h-9 rounded-full px-4 ${
                option.key === filterKey
                  ? "bg-ink text-white"
                  : "bg-surface-card border-hairline text-body border"
              }`}
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>

      {state.status === "loading" && (
        <p className="text-body text-muted py-16 text-center">불러오는 중이에요</p>
      )}

      {state.status === "error" && (
        <p className="text-body text-error py-16 text-center">{state.message}</p>
      )}

      {loaded && loaded.products.length === 0 && (
        <p className="text-body text-muted py-16 text-center">
          {appliedKeyword ? "검색 결과가 없어요" : "등록된 상품이 없어요"}
        </p>
      )}

      {loaded && loaded.products.length > 0 && (
        <>
          <AdminProductTable products={loaded.products} />

          <div className="flex items-center justify-center gap-4">
            <Button
              variant="secondary"
              size="sm"
              disabled={page <= 0}
              onClick={() => goToPage(page - 1)}
            >
              이전
            </Button>
            <span className="text-body-sm text-body">
              {page + 1} / {Math.max(1, loaded.totalPages)}
            </span>
            <Button
              variant="secondary"
              size="sm"
              disabled={page + 1 >= loaded.totalPages}
              onClick={() => goToPage(page + 1)}
            >
              다음
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
