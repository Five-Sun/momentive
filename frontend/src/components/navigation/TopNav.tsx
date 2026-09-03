"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { ShoppingBag } from "lucide-react";
import { getCartCount } from "@/lib/storage/cart";

const LINKS = [
  { key: "home", label: "홈", href: "/" },
  { key: "category", label: "카테고리", href: "/category" },
  { key: "wishlist", label: "위시", href: "/wishlist" },
  { key: "mypage", label: "마이", href: "/mypage" },
] as const;

export function TopNav() {
  const pathname = usePathname();
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [cartCount, setCartCount] = useState(0);

  const readCartCount = () => setCartCount(getCartCount());

  // 경로가 바뀔 때마다(장바구니 담기/비우기 후 이동 포함) 최신 수량을 반영
  useEffect(() => {
    Promise.resolve().then(readCartCount);
  }, [pathname]);

  function handleSearch() {
    const trimmed = query.trim();
    if (!trimmed) return;
    router.push(`/search?q=${encodeURIComponent(trimmed)}`);
  }

  return (
    <div className="border-hairline hidden h-20 items-center justify-between border-b px-10 lg:flex">
      <div className="flex items-center gap-14">
        <Link href="/" className="shrink-0">
          <Image
            src="/logo/momentive-logo.jpeg"
            alt="momentive"
            width={38}
            height={38}
            className="rounded-full"
          />
        </Link>
        <nav className="flex items-center gap-9">
          {LINKS.map((link) => {
            const active = pathname === link.href;
            return (
              <Link
                key={link.key}
                href={link.href}
                className={`text-body ${active ? "text-brand-pink-active font-bold" : "text-ink"}`}
              >
                {link.label}
              </Link>
            );
          })}
        </nav>
      </div>
      <div className="flex items-center gap-5">
        <div className="bg-surface-soft border-hairline flex h-[42px] w-[300px] items-center gap-2 rounded-full border px-4">
          <span className="text-muted text-body-sm">⌕</span>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            placeholder="브랜드, 상품 검색"
            className="text-body-sm text-ink flex-1 bg-transparent outline-none"
          />
        </div>
        <Link href="/cart" className="relative inline-flex">
          <ShoppingBag className="text-ink h-6 w-6" />
          {cartCount > 0 && (
            <span className="bg-brand-pink-active absolute -top-1 -right-2 rounded-full px-[5px] py-px text-[10px] font-bold text-white">
              {cartCount}
            </span>
          )}
        </Link>
      </div>
    </div>
  );
}
