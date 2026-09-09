"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/core/Button";
import { ApiError } from "@/lib/api/client";
import type { OrderStatus } from "@/lib/api/orders";
import { getAdminOrders, type AdminOrderSummary } from "@/lib/api/adminOrders";
import { AdminOrderTable } from "./AdminOrderTable";

const PAGE_SIZE = 20;
const ALL_STATUSES: OrderStatus[] = ["PENDING", "PAID", "FAILED", "CANCELLED"];

interface ListData {
  orders: AdminOrderSummary[];
  totalPages: number;
  totalElements: number;
}

/**
 * 조회 상태는 하나의 유니온으로 둔다 — "불러오는 중"·"실패"·"결과 없음"이 서로 섞여 보이지
 * 않도록 렌더 조건을 배타적으로 유지하기 위함이다. `AdminProductListPage`와 동일한 패턴.
 */
type ListState =
  | { status: "loading" }
  | { status: "loaded"; data: ListData }
  | { status: "error"; message: string };

export default function AdminOrderListPage() {
  const [state, setState] = useState<ListState>({ status: "loading" });
  const [page, setPage] = useState(0);
  // 기본은 PAID만(파라미터 없이 호출) — 서버 기본값에 그대로 맡긴다. "전체보기"는 4개 상태를
  // 모두 지정해서 보낸다(spec: "전체보기는 프론트에서 PENDING,PAID,FAILED,CANCELLED를 모두
  // 넘기는 방식으로 구현").
  const [showAll, setShowAll] = useState(false);

  useEffect(() => {
    let cancelled = false;

    getAdminOrders({ page, size: PAGE_SIZE, statuses: showAll ? ALL_STATUSES : undefined })
      .then((response) => {
        if (cancelled) return;
        setState({
          status: "loaded",
          data: {
            orders: response.content,
            totalPages: response.totalPages,
            totalElements: response.totalElements,
          },
        });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setState({
          status: "error",
          message: err instanceof ApiError ? err.message : "주문 목록을 불러오지 못했어요",
        });
      });

    return () => {
      cancelled = true;
    };
  }, [page, showAll]);

  function goToPage(next: number) {
    setState({ status: "loading" });
    setPage(next);
  }

  function toggleShowAll() {
    setState({ status: "loading" });
    setPage(0);
    setShowAll((prev) => !prev);
  }

  const loaded = state.status === "loaded" ? state.data : null;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div className="flex flex-col gap-1">
          <h1 className="text-title text-ink">주문 관리</h1>
          <span className="text-body-sm text-muted">
            {loaded ? `전체 ${loaded.totalElements.toLocaleString("ko-KR")}건` : " "}
          </span>
        </div>
        <Button variant={showAll ? "primary" : "secondary"} size="sm" onClick={toggleShowAll}>
          {showAll ? "전체보기 · 켜짐" : "전체보기"}
        </Button>
      </div>

      {state.status === "loading" && (
        <p className="text-body text-muted py-16 text-center">불러오는 중이에요</p>
      )}

      {state.status === "error" && (
        <p className="text-body text-error py-16 text-center">{state.message}</p>
      )}

      {loaded && loaded.orders.length === 0 && (
        <p className="text-body text-muted py-16 text-center">
          {showAll ? "주문이 없어요" : "결제완료 주문이 없어요"}
        </p>
      )}

      {loaded && loaded.orders.length > 0 && (
        <>
          <AdminOrderTable orders={loaded.orders} />

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
