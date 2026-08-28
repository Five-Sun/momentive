"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Truck, Ticket, Coins, PawPrint, Headset, ChevronRight } from "lucide-react";
import { getWishlist } from "@/lib/storage/wishlist";
import { getCartCount } from "@/lib/storage/cart";
import { useAuth } from "@/lib/auth/AuthProvider";
import { Button } from "@/components/core/Button";

const MENU_ITEMS = [
  { icon: Truck, label: "배송조회" },
  { icon: Ticket, label: "쿠폰함" },
  { icon: Coins, label: "적립금" },
  { icon: PawPrint, label: "반려견 프로필 관리" },
  { icon: Headset, label: "고객센터" },
];

export default function MyPage() {
  const router = useRouter();
  const { user, logout } = useAuth();
  const [wishlistCount, setWishlistCount] = useState(0);
  const [cartCount, setCartCount] = useState(0);

  useEffect(() => {
    Promise.resolve().then(() => {
      setWishlistCount(getWishlist().length);
      setCartCount(getCartCount());
    });
  }, []);

  async function handleLogout() {
    await logout();
  }

  return (
    <main className="bg-canvas flex min-h-screen flex-col">
      <header className="flex items-center justify-center px-4 py-4">
        <span className="text-title-sm text-ink">마이</span>
      </header>

      {user ? (
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3">
            <div className="bg-surface-strong h-14 w-14 flex-shrink-0 rounded-full" />
            <span className="text-title text-ink">{user.nickname}님</span>
          </div>
          <Button variant="secondary" size="sm" onClick={handleLogout}>
            로그아웃
          </Button>
        </div>
      ) : (
        <div className="flex items-center justify-between px-4 py-3">
          <span className="text-title text-ink">로그인이 필요합니다</span>
          <Button variant="primary" size="sm" onClick={() => router.push("/login")}>
            로그인
          </Button>
        </div>
      )}

      <div className="border-hairline mx-4 flex rounded-md border">
        <button
          onClick={() => {}}
          className="flex flex-1 flex-col items-center gap-1 py-4"
        >
          <span className="text-title-sm text-ink">0</span>
          <span className="text-caption text-muted">주문내역</span>
        </button>
        <div className="bg-hairline w-px" />
        <button
          onClick={() => router.push("/wishlist")}
          className="flex flex-1 flex-col items-center gap-1 py-4"
        >
          <span className="text-title-sm text-ink">{wishlistCount}</span>
          <span className="text-caption text-muted">위시리스트</span>
        </button>
        <div className="bg-hairline w-px" />
        <button
          onClick={() => router.push("/cart")}
          className="flex flex-1 flex-col items-center gap-1 py-4"
        >
          <span className="text-title-sm text-ink">{cartCount}</span>
          <span className="text-caption text-muted">장바구니</span>
        </button>
      </div>

      <div className="mt-4 flex flex-col px-4 pb-20">
        {MENU_ITEMS.map(({ icon: Icon, label }) => (
          <button
            key={label}
            onClick={() => {}}
            className="border-hairline flex h-14 items-center justify-between border-b"
          >
            <div className="flex items-center gap-3">
              <Icon className="text-ink h-5 w-5" />
              <span className="text-body text-ink">{label}</span>
            </div>
            <ChevronRight className="text-muted h-4 w-4" />
          </button>
        ))}
      </div>
    </main>
  );
}
