"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Truck, Ticket, PawPrint, Headset, ChevronRight } from "lucide-react";
import { getWishlist } from "@/lib/storage/wishlist";
import { getCartCount } from "@/lib/storage/cart";
import { useAuth } from "@/lib/auth/AuthProvider";
import { Button } from "@/components/core/Button";
import { getOrders } from "@/lib/api/orders";

export default function MyPage() {
  const router = useRouter();

  // 적립금: 제도 도입 여부가 미정이라 메뉴에서 내려둔 상태. 도입이 확정되면
  // { icon: Coins, label: "적립금", onClick: () => router.push("/mypage/points") }를
  // 쿠폰함 다음 자리에 복원한다 (lucide-react의 Coins 아이콘 import도 함께).
  const MENU_ITEMS = [
    { icon: Truck, label: "배송조회", onClick: () => {} },
    { icon: Ticket, label: "쿠폰함", onClick: () => router.push("/mypage/coupons") },
    { icon: PawPrint, label: "반려견 프로필 관리", onClick: () => router.push("/mypage/pets") },
    { icon: Headset, label: "고객센터", onClick: () => router.push("/mypage/support") },
  ];

  const { user, logout } = useAuth();
  const [wishlistCount, setWishlistCount] = useState(0);
  const [cartCount, setCartCount] = useState(0);
  const [orderCount, setOrderCount] = useState(0);

  useEffect(() => {
    Promise.resolve().then(() => {
      setWishlistCount(getWishlist().length);
      setCartCount(getCartCount());
    });
  }, []);

  useEffect(() => {
    if (!user) return;
    getOrders()
      .then((orders) => setOrderCount(orders.length))
      .catch(() => setOrderCount(0));
  }, [user]);

  async function handleLogout() {
    await logout();
  }

  return (
    <main className="bg-canvas flex min-h-screen flex-col">
      <header className="flex items-center justify-center px-4 py-4 lg:justify-start lg:px-0 lg:py-7">
        <span className="text-title-sm text-ink">마이</span>
      </header>

      {user ? (
        <div className="flex items-center justify-between px-4 py-3 lg:px-0">
          <div className="flex items-center gap-3">
            <div className="bg-surface-strong h-14 w-14 flex-shrink-0 rounded-full" />
            <span className="text-title text-ink">{user.nickname}님</span>
          </div>
          <Button variant="secondary" size="sm" onClick={handleLogout}>
            로그아웃
          </Button>
        </div>
      ) : (
        <div className="flex items-center justify-between px-4 py-3 lg:px-0">
          <span className="text-title text-ink">로그인이 필요합니다</span>
          <Button variant="primary" size="sm" onClick={() => router.push("/login")}>
            로그인
          </Button>
        </div>
      )}

      <div className="border-hairline mx-4 flex rounded-md border lg:mx-0">
        <button
          onClick={() => router.push("/mypage/orders")}
          className="flex flex-1 flex-col items-center gap-1 py-4"
        >
          <span className="text-title-sm text-ink">{orderCount}</span>
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

      <div className="mt-4 flex flex-col px-4 pb-20 lg:grid lg:grid-cols-3 lg:gap-3 lg:px-0 lg:pb-16">
        {MENU_ITEMS.map(({ icon: Icon, label, onClick }) => (
          <button
            key={label}
            onClick={onClick}
            className="border-hairline flex h-14 items-center justify-between border-b lg:rounded-md lg:border lg:px-4"
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
