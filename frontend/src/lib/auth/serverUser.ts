import { cookies } from "next/headers";
import type { AuthUser } from "@/lib/api/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

/**
 * 서버 컴포넌트(레이아웃)에서 요청 쿠키로 `/auth/me`를 조회해 `AuthProvider`의 초기값을 만든다.
 *
 * 공통 `apiFetch`는 브라우저가 쿠키를 자동으로 실어 보내는 것을 전제로 하므로, 쿠키를 헤더에
 * 직접 넣어야 하는 이 서버 전용 경로만 예외적으로 fetch를 직접 호출한다.
 */
export async function fetchServerUser(): Promise<AuthUser | null> {
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
