import Link from "next/link";
import { AuthProvider } from "@/lib/auth/AuthProvider";
import { fetchServerUser } from "@/lib/auth/serverUser";
import { AdminGuard } from "./AdminGuard";

/**
 * 관리자 레이아웃. `(shell)` 밖이라 모바일 프레임·하단 탭이 없는 데스크톱 폭 레이아웃이다.
 *
 * 화면 하단(또는 상단)에 고정되는 공통 UI를 두지 않는다 — 하위 페이지의 저장/등록 버튼이
 * 공통 고정 UI에 덮여 클릭되지 않는 사고를 애초에 만들지 않기 위함이다
 * (`docs/backlog/2026-08-29-cart-order-payment-phase6-01.md` 재발 방지).
 *
 * 접근 보호는 `AdminGuard` 하나로 여기서만 수행하고 하위 페이지에는 복붙하지 않는다.
 */
export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const initialUser = await fetchServerUser();

  return (
    <AuthProvider initialUser={initialUser}>
      <div className="bg-canvas flex min-h-screen flex-col">
        <header className="border-hairline bg-surface-card border-b">
          <div className="mx-auto flex h-16 w-full max-w-[1200px] items-center justify-between px-6">
            <Link href="/admin" className="text-title text-ink">
              모멘티브 관리자
            </Link>
            <Link href="/" className="text-body-sm text-muted underline">
              고객 화면으로
            </Link>
          </div>
        </header>

        <main className="mx-auto w-full max-w-[1200px] flex-1 px-6 py-8">
          <AdminGuard>{children}</AdminGuard>
        </main>
      </div>
    </AuthProvider>
  );
}
