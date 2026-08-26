"use client";

import { useRouter } from "next/navigation";
import { ChevronRight } from "lucide-react";
import { CATEGORY_LIST } from "@/lib/categories";

export default function CategoryPage() {
  const router = useRouter();

  return (
    <main className="bg-canvas flex min-h-screen flex-col">
      <header className="flex items-center justify-center px-4 py-4">
        <span className="text-title-sm text-ink">카테고리</span>
      </header>

      <div className="flex flex-col gap-3 px-4 pb-20">
        {CATEGORY_LIST.map((c) => (
          <button
            key={c.key}
            onClick={() => router.push(`/search?category=${c.key}`)}
            className="border-hairline bg-surface-card flex h-[72px] items-center justify-between rounded-md border px-[18px] text-left"
          >
            <div className="flex flex-col gap-0.5">
              <span className="text-title-sm text-ink">{c.label}</span>
              <span className="text-caption text-muted">{c.description}</span>
            </div>
            <ChevronRight className="text-muted h-4 w-4" />
          </button>
        ))}
      </div>
    </main>
  );
}
