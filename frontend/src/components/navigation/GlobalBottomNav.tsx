"use client";

import { usePathname, useRouter } from "next/navigation";
import { Grid2x2, Heart, Home, Search, User } from "lucide-react";
import { BottomNav } from "./BottomNav";

const TABS = [
  { key: "home", icon: <Home className="h-5 w-5" />, label: "홈", href: "/" },
  { key: "category", icon: <Grid2x2 className="h-5 w-5" />, label: "카테고리", href: "/category" },
  { key: "search", icon: <Search className="h-5 w-5" />, label: "검색", href: "/search" },
  { key: "wishlist", icon: <Heart className="h-5 w-5" />, label: "위시", href: "/wishlist" },
  { key: "mypage", icon: <User className="h-5 w-5" />, label: "마이", href: "/mypage" },
] as const;

// 자체 fixed bottom CTA 바를 가진 화면들. 전역 하단 네비게이션과 겹쳐 CTA를 가리므로
// 이 경로들에서는 렌더링하지 않는다. (docs/backlog/2026-08-29-cart-order-payment-phase6-01.md 참고)
const HIDDEN_PREFIXES = ["/checkout", "/mypage/orders/"] as const;

function isHidden(pathname: string) {
  return HIDDEN_PREFIXES.some((prefix) => pathname.startsWith(prefix));
}

export function GlobalBottomNav() {
  const pathname = usePathname();
  const router = useRouter();

  if (isHidden(pathname)) {
    return null;
  }

  const activeTab = TABS.find((tab) => tab.href === pathname);

  return (
    <BottomNav
      items={TABS.map(({ key, icon, label }) => ({ key, icon, label }))}
      activeKey={activeTab?.key ?? ""}
      onSelect={(key) => {
        const tab = TABS.find((t) => t.key === key);
        if (tab) router.push(tab.href);
      }}
    />
  );
}
