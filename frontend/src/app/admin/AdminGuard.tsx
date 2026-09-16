"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth/AuthProvider";

/**
 * `/admin` 접근 보호. `AuthProvider`가 들고 있는 사용자의 `role`이 `ADMIN`이 아니면 홈으로 보낸다.
 *
 * 이 검사는 **UX 차원**이다 — 관리자가 아닌 사람에게 빈 관리자 화면을 보여주지 않기 위한 것일 뿐,
 * 실제 방어선은 백엔드 `SecurityConfig`의 `hasRole("ADMIN")`이다. 관리자 데이터는 전부
 * `/admin/**` API를 통해서만 오고 그 API가 권한 없는 호출을 403으로 끊으므로, 이 컴포넌트를
 * 우회하더라도 데이터가 노출되지 않는다.
 *
 * 검사는 `admin/layout.tsx`에서 이 컴포넌트를 통해 한 번만 수행하고, 하위 페이지에 복붙하지 않는다.
 */
export function AdminGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { loadCurrentUser } = useAuth();
  const [status, setStatus] = useState<"checking" | "authorized" | "unauthorized">("checking");

  useEffect(() => {
    let cancelled = false;

    loadCurrentUser()
      .then((user) => {
        if (cancelled) return;
        if (user.role === "ADMIN") {
          setStatus("authorized");
          return;
        }
        setStatus("unauthorized");
        router.replace("/");
      })
      .catch(() => {
        if (cancelled) return;
        setStatus("unauthorized");
        router.replace("/");
      });

    return () => {
      cancelled = true;
    };
  }, [loadCurrentUser, router]);

  if (status === "checking") {
    return (
      <p className="text-body text-muted py-20 text-center">관리자 권한을 확인하는 중이에요</p>
    );
  }

  if (status === "unauthorized") return null;

  return <>{children}</>;
}
