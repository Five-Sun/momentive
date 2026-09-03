"use client";

import { useState } from "react";
import Image from "next/image";
import { ArrowLeft, Heart, MoreHorizontal, Home, Grid2x2, Search, User } from "lucide-react";
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

const TYPE_SCALE = [
  { className: "text-display-lg", label: "text-display-lg", spec: "600 30px/1.25" },
  { className: "text-display-md", label: "text-display-md", spec: "400 24px/1.3" },
  { className: "text-title", label: "text-title", spec: "700 18px/1.35" },
  { className: "text-title-sm", label: "text-title-sm", spec: "600 16px/1.4" },
  { className: "text-body", label: "text-body", spec: "400 15px/1.55" },
  { className: "text-body-sm", label: "text-body-sm", spec: "400 13px/1.5" },
  { className: "text-caption", label: "text-caption", spec: "500 12px/1.4 · synthetic bold 확인 대상" },
  { className: "text-price", label: "text-price", spec: "700 17px/1.3" },
  { className: "text-button", label: "text-button", spec: "600 15px/1" },
  { className: "text-tag", label: "text-tag", spec: "600 11px/1 · synthetic bold 확인 대상" },
] as const;

const BOTTOM_NAV_TABS = [
  { key: "home", icon: <Home className="h-5 w-5" />, label: "홈" },
  { key: "category", icon: <Grid2x2 className="h-5 w-5" />, label: "카테고리" },
  { key: "search", icon: <Search className="h-5 w-5" />, label: "검색" },
  { key: "wishlist", icon: <Heart className="h-5 w-5" />, label: "위시" },
  { key: "mypage", icon: <User className="h-5 w-5" />, label: "마이" },
];

export default function StyleGuidePage() {
  const [chipSelected, setChipSelected] = useState("전체");
  const [size, setSize] = useState("S");
  const [favorited, setFavorited] = useState(true);
  const [toastVisible, setToastVisible] = useState(true);
  const [tab, setTab] = useState("home");
  const [pawPopKey, setPawPopKey] = useState(0);
  const [bumpUpKey, setBumpUpKey] = useState(0);
  const [springKey, setSpringKey] = useState(0);
  const [springVisible, setSpringVisible] = useState(true);

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

      <section className="flex flex-col gap-4">
        <h2 className="text-title text-ink">타이포그래피 스케일</h2>
        <p className="text-body-sm text-muted">
          디스플레이·본문 모두 어비 세현체(UhBeeSehyun) + Noto Sans KR fallback. weight 500 이상은
          synthetic bold로 렌더된다.
        </p>
        <div className="border-hairline flex flex-col gap-3 rounded-md border p-5">
          {TYPE_SCALE.map(({ className, label, spec }) => (
            <div key={className} className="flex flex-wrap items-baseline gap-3">
              <span className={`${className} text-ink`}>모멘티브 어비 세현체 Aa 123</span>
              <span className="text-body-sm text-muted">
                {label} · {spec}
              </span>
            </div>
          ))}
        </div>
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-title text-ink">모션</h2>
        <p className="text-body-sm text-muted">
          하트 토글·칩 선택·탭 활성화·토스트 등장 등 미세 인터랙션 전용. 버튼을 눌러 재생을
          확인한다. OS &quot;동작 줄이기&quot;가 켜져 있으면 모두 비활성화된다.
        </p>
        <div className="flex flex-wrap items-center gap-8">
          <div className="flex flex-col items-center gap-2">
            <button
              key={pawPopKey}
              onClick={() => setPawPopKey((k) => k + 1)}
              className="animate-paw-pop bg-surface-strong flex h-14 w-14 items-center justify-center rounded-full"
              aria-label="paw-pop 재생"
            >
              <Heart className="text-brand-pink-active h-6 w-6" fill="currentColor" />
            </button>
            <span className="text-body-sm text-ink">paw-pop</span>
            <span className="text-caption text-muted">위시 하트 · 칩 · Badge</span>
          </div>

          <div className="flex flex-col items-center gap-2">
            <button
              onClick={() => setBumpUpKey((k) => k + 1)}
              className="text-brand-pink-active flex h-14 w-14 flex-col items-center justify-center gap-1"
              aria-label="bump-up 재생"
            >
              <span key={bumpUpKey} className="animate-bump-up text-xl">
                <Home className="h-5 w-5" />
              </span>
              <span className="text-caption font-bold">홈</span>
            </button>
            <span className="text-body-sm text-ink">bump-up</span>
            <span className="text-caption text-muted">하단 탭 활성화</span>
          </div>

          <div className="flex flex-col items-center gap-2">
            <div className="relative h-16 w-40">
              <div
                key={springKey}
                style={{
                  transform: `translateX(-50%) translateY(${springVisible ? "0" : "12px"}) scale(${springVisible ? 1 : 0.9})`,
                  opacity: springVisible ? 1 : 0,
                  transitionTimingFunction: "var(--ease-spring)",
                }}
                className="text-body-sm shadow-float bg-ink pointer-events-none absolute bottom-0 left-1/2 whitespace-nowrap rounded-full px-5 py-3 text-white transition-all duration-300"
              >
                장바구니에 담았어요
              </div>
            </div>
            <Button
              variant="ghost"
              onClick={() => {
                setSpringVisible(false);
                setSpringKey((k) => k + 1);
                setTimeout(() => setSpringVisible(true), 50);
              }}
            >
              --ease-spring 재생
            </Button>
            <span className="text-caption text-muted">Toast 등장</span>
          </div>
        </div>
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-title text-ink">브레이크포인트 · 상품 그리드</h2>
        <p className="text-body-sm text-muted">
          &lt;1024px 2열 / ≥1024px 3열 / ≥1280px 4열. 창 폭을 조절해 열 수 전환을 확인한다.
          네비게이션은 &lt;1024px에서 하단 탭바, ≥1024px에서 상단 네비로 전환된다(이 페이지는
          `(shell)` 밖이라 실제 네비는 홈 등 셸 하위 화면에서 확인).
        </p>
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-3 lg:gap-10 xl:grid-cols-4">
          {["플리스 후드 집업", "니트 스웨터", "바람막이 자켓", "기본 리드줄", "겨울 패딩", "면 손수건"].map(
            (name) => (
              <div
                key={name}
                className="bg-surface-strong text-muted flex aspect-square items-center justify-center rounded-md text-center text-xs"
              >
                {name}
              </div>
            ),
          )}
        </div>
        <div className="text-caption text-muted flex flex-wrap gap-4">
          <span>&lt;1024px: 2열</span>
          <span className="lg:font-bold">≥1024px: 3열 (lg:grid-cols-3)</span>
          <span className="xl:font-bold">≥1280px: 4열 (xl:grid-cols-4)</span>
        </div>
      </section>

      <Section title="BottomNav (5탭)">
        <div className="w-90 max-w-full">
          <BottomNav items={BOTTOM_NAV_TABS} activeKey={tab} onSelect={setTab} />
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
