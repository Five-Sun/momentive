import { cookies } from "next/headers";
import { GlobalBottomNav } from "@/components/navigation/GlobalBottomNav";
import { TopNav } from "@/components/navigation/TopNav";
import { AuthProvider } from "@/lib/auth/AuthProvider";
import type { AuthUser } from "@/lib/api/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

async function fetchInitialUser(): Promise<AuthUser | null> {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const res = await fetch(`${API_BASE_URL}/auth/me`, {
      headers: cookieHeader ? { cookie: cookieHeader } : undefined,
      cache: "no-store",
    });

    if (!res.ok) return null;
    return (await res.json()) as AuthUser;
  } catch {
    // 백엔드 연결 실패 시에도 화면 렌더링이 막히지 않도록 비로그인 상태로 처리
    return null;
  }
}

export default async function ShellLayout({ children }: { children: React.ReactNode }) {
  const initialUser = await fetchInitialUser();

  return (
    <AuthProvider initialUser={initialUser}>
      <div className="bg-canvas flex min-h-screen flex-col">
        <TopNav />
        <div className="mx-auto flex w-full max-w-[480px] flex-1 flex-col lg:max-w-[1400px] lg:px-10">
          <div className="flex-1">{children}</div>
          <div className="sticky bottom-0">
            <GlobalBottomNav />
          </div>
        </div>
      </div>
    </AuthProvider>
  );
}
