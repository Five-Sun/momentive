"use client";

import { usePathname, useRouter } from "next/navigation";
import { BottomNav } from "./BottomNav";

const TABS = [
  { key: "home", icon: "⌂", label: "홈", href: "/" },
  { key: "search", icon: "⌕", label: "검색", href: "/search" },
  { key: "cart", icon: "🛍", label: "장바구니", href: "/cart" },
  { key: "mypage", icon: "👤", label: "마이페이지", href: "/mypage" },
] as const;

export function GlobalBottomNav() {
  const pathname = usePathname();
  const router = useRouter();

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
