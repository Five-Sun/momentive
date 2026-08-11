const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

async function getBackendHealth() {
  if (!API_BASE_URL) return { ok: false, message: "NEXT_PUBLIC_API_BASE_URL 미설정" };
  try {
    const res = await fetch(`${API_BASE_URL}/health`, { cache: "no-store" });
    if (!res.ok) return { ok: false, message: `status ${res.status}` };
    return { ok: true, message: await res.text() };
  } catch (e) {
    return { ok: false, message: String(e) };
  }
}

export default async function Home() {
  const health = await getBackendHealth();

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 p-8">
      <h1 className="text-2xl font-bold">모멘티브</h1>
      <p className="text-sm text-gray-500">워킹 스켈레톤 — 백엔드 연결 확인용</p>
      <div className="rounded-lg border p-4">
        <p>백엔드 상태: {health.ok ? "✅ 연결됨" : "❌ 연결 안 됨"}</p>
        <p className="text-xs text-gray-500 mt-1">{health.message}</p>
      </div>
    </main>
  );
}
