"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Heart, Minus, Plus } from "lucide-react";
import { Badge } from "@/components/core/Badge";
import { Button } from "@/components/core/Button";
import { IconButton } from "@/components/core/IconButton";
import { Toast } from "@/components/feedback/Toast";
import { ProductImage } from "@/components/commerce/ProductImage";
import { Rating } from "@/components/commerce/Rating";
import { ReviewCard } from "@/components/commerce/ReviewCard";
import { SizeSelector } from "@/components/commerce/SizeSelector";
import type { ProductDetail } from "@/lib/api/products";
import { isWishlisted, toggleWishlist } from "@/lib/storage/wishlist";
import { addToCart } from "@/lib/storage/cart";
import { recordRecentlyViewed } from "@/lib/storage/recentlyViewed";

const SIZES = ["S", "M", "L", "XL"];

const SIZE_GUIDE: [string, string][] = [
  ["S", "3~5kg · 등길이 25cm"],
  ["M", "5~8kg · 등길이 30cm"],
  ["L", "8~12kg · 등길이 35cm"],
  ["XL", "12kg~ · 등길이 40cm"],
];

const DELIVERY_INFO = [
  "오후 2시 이전 결제 시 당일 출고",
  "제주/도서산간 지역 추가 배송비 3,000원",
  "단순 변심 교환/반품은 상품 수령 후 7일 이내 가능",
];

// 리뷰 백엔드가 없어 전 상품 공통 목업 세트를 재사용한다 (grillme 결정)
const MOCK_REVIEWS = [
  { author: "몽이맘", rating: 5, date: "2026.08.10", text: "핏이 예쁘고 소재가 부드러워요. 다음에도 재구매할게요.", photoCount: 2 },
  { author: "보리아빠", rating: 4.5, date: "2026.07.28", text: "사이즈 가이드대로 주문했는데 딱 맞았어요.", photoCount: 0 },
  { author: "코코맘", rating: 4, date: "2026.07.15", text: "배송이 빨라서 좋았어요! 다음엔 다른 색상도 사보려구요.", photoCount: 1 },
];

function formatWon(amount: number) {
  return `${amount.toLocaleString("ko-KR")}원`;
}

export function ProductDetailView({ product }: { product: ProductDetail }) {
  const router = useRouter();
  const [favorited, setFavorited] = useState(false);
  const [size, setSize] = useState<string | null>(null);
  const [openPanel, setOpenPanel] = useState<"guide" | "delivery" | null>(null);
  const [toastVisible, setToastVisible] = useState(false);

  useEffect(() => {
    recordRecentlyViewed(product.id);
    Promise.resolve().then(() => setFavorited(isWishlisted(product.id)));
  }, [product.id]);

  const hasDiscount = !product.soldOut && product.discountPrice != null;
  const unitPrice = hasDiscount ? product.discountPrice! : product.price;

  function handleToggleWishlist() {
    setFavorited(toggleWishlist(product.id).includes(product.id));
  }

  function handleAddToCart() {
    if (!size || product.soldOut) return;
    addToCart({ id: product.id, title: product.name, size, unitPrice });
    setToastVisible(true);
    setTimeout(() => setToastVisible(false), 1800);
  }

  return (
    <div className="bg-canvas relative flex min-h-screen flex-col">
      <div className="border-hairline bg-surface-card flex h-13 flex-shrink-0 items-center justify-between border-b px-4">
        <button onClick={() => router.back()} aria-label="뒤로가기" className="text-ink">
          <ArrowLeft className="h-5 w-5" />
        </button>
        <IconButton size={32} active={favorited} onClick={handleToggleWishlist}>
          <Heart className="h-4 w-4" fill={favorited ? "currentColor" : "none"} />
        </IconButton>
      </div>

      <div className="flex flex-col gap-3 p-4">
        {product.images.length === 0 ? (
          <div className="bg-surface-strong text-muted flex aspect-square items-center justify-center rounded-2xl text-center text-sm">
            {product.name}
          </div>
        ) : (
          product.images.map((image) => (
            <ProductImage key={image.id} url={image.url} name={product.name} />
          ))
        )}
      </div>

      <div className="flex flex-col gap-3 px-4">
        <div className="flex items-center gap-2">
          {product.soldOut && <Badge tone="soldout" label="품절" />}
          {hasDiscount && (
            <Badge
              tone="sale"
              label={`${Math.round((1 - product.discountPrice! / product.price) * 100)}%`}
            />
          )}
        </div>
        <h1 className="text-title text-ink">{product.name}</h1>
        <Rating value={4.5} />
        <div className="flex items-baseline gap-1.5">
          {hasDiscount && (
            <span className="text-body-sm text-muted-soft line-through">
              {formatWon(product.price)}
            </span>
          )}
          <span className="text-price text-ink">{formatWon(unitPrice)}</span>
        </div>

        <div className="bg-hairline my-1.5 h-px" />

        <div className="flex items-center justify-between">
          <span className="text-title-sm text-ink">사이즈</span>
          <button
            onClick={() => setOpenPanel(openPanel === "guide" ? null : "guide")}
            className="text-caption text-brand-pink-active underline"
          >
            사이즈 가이드
          </button>
        </div>
        <div className={product.soldOut ? "pointer-events-none opacity-50" : undefined}>
          <SizeSelector sizes={SIZES} selected={size ?? ""} onSelect={setSize} />
        </div>
        {openPanel === "guide" && (
          <div className="bg-surface-soft rounded-sm flex flex-col gap-1.5 p-3.5">
            <span className="text-caption text-ink font-bold">반려견 체형별 사이즈</span>
            {SIZE_GUIDE.map(([s, d]) => (
              <div key={s} className="text-body-sm text-body flex justify-between">
                <span>{s}</span>
                <span className="text-muted">{d}</span>
              </div>
            ))}
          </div>
        )}

        <div className="bg-hairline my-1.5 h-px" />

        <button
          onClick={() => setOpenPanel(openPanel === "delivery" ? null : "delivery")}
          className="flex items-center justify-between"
        >
          <span className="text-title-sm text-ink">배송 · 교환/반품 안내</span>
          {openPanel === "delivery" ? (
            <Minus className="text-muted h-4 w-4" />
          ) : (
            <Plus className="text-muted h-4 w-4" />
          )}
        </button>
        {openPanel === "delivery" && (
          <div className="text-body-sm text-body flex flex-col gap-1">
            {DELIVERY_INFO.map((line) => (
              <span key={line}>· {line}</span>
            ))}
          </div>
        )}

        <div className="bg-hairline my-1.5 h-px" />

        <span className="text-title-sm text-ink">상품 설명</span>
        <p className="text-body text-ink m-0">{product.description}</p>

        <div className="bg-hairline my-1.5 h-px" />

        <span className="text-title-sm text-ink">리뷰</span>
        <div className="flex flex-col gap-3">
          {MOCK_REVIEWS.map((review) => (
            <ReviewCard key={review.author} {...review} />
          ))}
        </div>
      </div>

      <div className="border-hairline bg-surface-card sticky bottom-16 flex gap-2.5 border-t p-3.5">
        <Button variant="secondary" onClick={handleToggleWishlist}>
          {favorited ? "위시 완료" : "위시 담기"}
        </Button>
        <div className="flex-1">
          <Button
            variant="primary"
            disabled={!size || product.soldOut}
            onClick={handleAddToCart}
          >
            장바구니 담기
          </Button>
        </div>
      </div>

      <Toast message="장바구니에 담았어요" visible={toastVisible} />
    </div>
  );
}
