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
