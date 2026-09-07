"use client";

import { useEffect } from "react";
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
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  useEffect(() => {
    if (!isAdmin) router.replace("/");
  }, [isAdmin, router]);

  if (!isAdmin) {
    return (
      <p className="text-body text-muted py-20 text-center">관리자만 접근할 수 있는 화면이에요</p>
    );
  }

  return <>{children}</>;
}
