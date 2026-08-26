"use client";

import { useState } from "react";
import Image from "next/image";
import { ArrowLeft, Heart, MoreHorizontal, Home, Search, ShoppingBag } from "lucide-react";
import { Button } from "@/components/core/Button";
import { Badge } from "@/components/core/Badge";
import { Chip } from "@/components/core/Chip";
import { IconButton } from "@/components/core/IconButton";
import { SearchInput } from "@/components/forms/SearchInput";
import { BottomNav } from "@/components/navigation/BottomNav";
import { Toast } from "@/components/feedback/Toast";
import { ProductCard } from "@/components/commerce/ProductCard";
import { Rating } from "@/components/commerce/Rating";
import { SizeSelector } from "@/components/commerce/SizeSelector";

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="flex flex-col gap-4">
      <h2 className="text-title text-ink">{title}</h2>
      <div className="flex flex-wrap items-center gap-4">{children}</div>
    </section>
  );
}

export default function StyleGuidePage() {
  const [chipSelected, setChipSelected] = useState("전체");
  const [size, setSize] = useState("S");
  const [favorited, setFavorited] = useState(true);
  const [toastVisible, setToastVisible] = useState(true);
  const [tab, setTab] = useState("home");

  return (
    <main className="bg-canvas flex flex-col gap-12 p-8">
      <div className="flex items-center gap-4">
        <Image
          src="/logo/momentive-logo.jpeg"
          alt="momentive"
          width={48}
          height={48}
          className="rounded-full"
        />
        <h1 className="text-display-md text-ink">Momentive 스타일 가이드</h1>
      </div>

      <Section title="Button">
        <Button variant="primary">장바구니 담기</Button>
        <Button variant="secondary">전체보기</Button>
        <Button variant="ghost">취소</Button>
        <Button variant="primary" disabled>
          품절
        </Button>
        <Button variant="primary" size="sm">
          담기
        </Button>
      </Section>

      <Section title="IconButton">
        <IconButton>
          <ArrowLeft className="h-5 w-5" />
        </IconButton>
        <IconButton active>
          <Heart className="h-5 w-5" fill="currentColor" />
        </IconButton>
        <IconButton variant="filled">
          <MoreHorizontal className="h-5 w-5" />
        </IconButton>
      </Section>

      <Section title="Badge">
        <Badge label="NEW" tone="new" />
        <Badge label="-20%" tone="sale" />
        <Badge label="품절" tone="soldout" />
        <Badge label="BEST" tone="neutral" />
      </Section>

      <Section title="Chip">
        {["전체", "아우터", "니트", "바람막이"].map((label) => (
          <Chip
            key={label}
            label={label}
            selected={chipSelected === label}
            onClick={() => setChipSelected(label)}
          />
        ))}
      </Section>

      <Section title="SearchInput">
        <div className="w-72">
          <SearchInput value="" placeholder="브랜드, 상품 검색" />
        </div>
      </Section>

      <Section title="Rating">
        <Rating value={4.8} count={132} />
      </Section>

      <Section title="SizeSelector">
        <SizeSelector sizes={["XS", "S", "M", "L", "XL"]} selected={size} onSelect={setSize} />
      </Section>

      <Section title="ProductCard">
        <div className="w-40">
          <ProductCard
            image={
              <div className="text-muted flex h-full items-center justify-center p-2 text-center text-xs">
                플리스 후드 집업
              </div>
            }
            title="플리스 후드 집업"
            price="38,000원"
            originalPrice="48,000원"
            badge={<Badge label="-20%" tone="sale" />}
            favorited={favorited}
            onToggleFavorite={() => setFavorited((v) => !v)}
            rating={<Rating value={4.8} count={132} />}
          />
        </div>
      </Section>

      <Section title="BottomNav">
        <div className="w-90 max-w-full">
          <BottomNav
            items={[
              { key: "home", icon: <Home className="h-5 w-5" />, label: "홈" },
              { key: "search", icon: <Search className="h-5 w-5" />, label: "검색" },
              { key: "wishlist", icon: <Heart className="h-5 w-5" />, label: "위시" },
              { key: "cart", icon: <ShoppingBag className="h-5 w-5" />, label: "장바구니" },
            ]}
            activeKey={tab}
            onSelect={setTab}
          />
        </div>
      </Section>

      <Section title="Toast">
        <div className="relative h-24 w-full">
          <Toast message="장바구니에 담았어요" visible={toastVisible} />
        </div>
        <Button variant="ghost" onClick={() => setToastVisible((v) => !v)}>
          토스트 토글
        </Button>
      </Section>
    </main>
  );
}
