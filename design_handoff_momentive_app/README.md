# Handoff: Momentive 강아지 의류 쇼핑몰 앱 (Next.js + Tailwind 이관용)

## Overview
Momentive(모멘티브) 반려견 의류 쇼핑몰의 **반응형 웹** 디자인입니다 (모바일 웹 우선, 데스크톱 겸용). 홈, 카테고리, 검색, 상품상세, 장바구니, 위시리스트, 마이 7개 화면과 디자인 토큰/컴포넌트 세트를 포함합니다.

**필수 구현 지침**: 모바일 웹(1024px 미만)은 하단 탭바(홈/카테고리/검색/위시/마이), 데스크톱(1024px 이상)은 상단 가로 네비 + 넓은 그리드로 전환. 브레이크포인트·레이아웃은 `ui_kits/web-app/index.html`을 그대로 따를 것 — `ui_kits/mobile-app/index.html`은 참고용 구버전(네이티브 앱 형태)이니 레이아웃 기준으로 쓰지 말 것.

## About the Design Files
이 번들의 HTML 파일들은 **디자인 레퍼런스**입니다 (React CDN + inline style로 만든 프로토타입) — 그대로 복사해 쓰는 프로덕션 코드가 아닙니다. 목표는 이 디자인을 **Next.js + Tailwind CSS** 기반으로 동일하게 재구현하는 것입니다: inline style → Tailwind 유틸리티 클래스, CSS 커스텀 프로퍼티 토큰 → `tailwind.config` 테마 확장, 단일 App 컴포넌트 → Next.js 라우트/컴포넌트 구조로 변환해주세요.

## Fidelity
**High-fidelity**: 색상, 타이포, 스페이싱, 컴포넌트 구조, 인터랙션 흐름 모두 최종안입니다. 픽셀 단위로 동일하게 재현해주세요. (단, 아래 "Known gaps"의 항목은 임시 대체물이니 실제 자산으로 교체가 필요합니다.)

## Design Tokens → Tailwind 매핑 제안
`tokens/colors.css`, `tokens/typography.css`, `tokens/spacing.css`, `tokens/radius.css`, `tokens/shadow.css`를 읽고 `tailwind.config.js`의 `theme.extend`에 그대로 이식하세요.

```js
// tailwind.config.js 예시
theme: {
  extend: {
    colors: {
      brand: { pink: '#f57ea0', pinkActive: '#e8547f', pinkDeep: '#d6396a', pinkSoft: '#fce0ea', pinkTint: '#fff2f6', yellow: '#fef8c4', yellowSoft: '#fffdf0' },
      ink: '#33242b', body: '#5c454f', muted: '#93818a', mutedSoft: '#c4aeb7',
      hairline: '#f3e2e9', hairlineSoft: '#f9edf1', borderStrong: '#e3c3d0',
      canvas: '#fffbfc', surfaceSoft: '#fdf1f4', surfaceCard: '#ffffff', surfaceStrong: '#fbe4ea',
      success: '#5a8a6b', error: '#c1543b', sale: '#e8547f',
    },
    fontFamily: { display: ['UhBeeSehyun', 'Noto Sans KR', 'sans-serif'], body: ['UhBeeSehyun', 'Noto Sans KR', 'sans-serif'] },
    borderRadius: { xs: '6px', sm: '10px', md: '16px', lg: '22px', full: '9999px' },
    boxShadow: { card: '0 1px 2px rgba(43,38,33,.04), 0 6px 16px rgba(43,38,33,.08)', float: '0 4px 10px rgba(43,38,33,.06), 0 12px 28px rgba(43,38,33,.12)' },
    transitionTimingFunction: { spring: 'cubic-bezier(.34,1.56,.64,1)' },
  },
}
```
타이포 스케일(`text-display-lg` 30px, `text-title` 18px/700 등)은 `theme.extend.fontSize`에 `[size, {lineHeight, fontWeight}]` 형태로 추가하세요. `UhBeeSehyun`은 `tokens/typography.css`의 `@font-face` URL로 로드하세요 (어비 세현체, 상업적 사용 무료).

미세 인터랙션(`tokens/motion.css`): 하트/칩 토글 시 `paw-pop`(scale 1→1.28→.94→1) 바운스, 하단 탭 활성화 시 `bump-up`(아이콘 살짝 튐), 토스트 등장 시 spring 이징(translateY+scale). Tailwind에서는 `keyframes`/`animation`에 그대로 등록해 재현하세요 — 화면 전환 등 큰 모션에는 쓰지 말 것(브랜드 톤상 과한 모션 지양).

## Screens / Views
소스: `ui_kits/web-app/index.html` (단일 파일, 화면 전환은 React state `tab`으로 관리, 반응형 CSS로 모바일 웹/데스크톱 겸용). Next.js에서는 각 화면을 `app/(shop)/home`, `/category`, `/search`, `/product/[id]`, `/cart`, `/wishlist`, `/my` 라우트로 분리하는 것을 권장합니다.

1. **홈 (Home)** — 로고+장바구니 아이콘 헤더 → 검색바(탭하면 검색 화면 이동) → 프로모 배너(핑크 그라디언트) → "지금 인기 있는" 가로스크롤 랭킹(리뷰 많은순 상위 4개, 순위 배지) → 카테고리 칩 필터 → 2열 상품 그리드 → "최근 본 상품" 가로스크롤(상품상세 방문 시 누적, 최대 8개).
2. **카테고리 (Category)** — 아우터/니트/이너웨어/악세서리 4개 리스트 항목(설명 텍스트 포함), 탭하면 해당 카테고리로 필터링된 검색 결과로 이동.
3. **검색 (Search)** — 검색바(자동 포커스) → 입력 중 상품명 매칭 자동완성 드롭다운(최대 5개) → 미입력 시 최근검색어 칩 + 인기검색어 순위 리스트(1~3위 핑크 숫자) → 검색 실행 시 결과 개수 + 정렬 버튼(바텀시트 오픈) + 2열 그리드.
4. **상품상세 (Product Detail)** — 헤더(뒤로가기, 위시 하트) → 정사각 이미지 + 도트 인디케이터(3개, 정적) → 제목/평점/가격 → 사이즈 셀렉터 + "사이즈 가이드" 토글(체중/등길이 표) → "배송·교환/반품 안내" 아코디언 → 상품설명 → 리뷰 리스트(평점/날짜/텍스트/사진 썸네일) → 하단 고정 CTA(위시 담기 + 장바구니 담기).
5. **장바구니 (Cart)** — 무료배송 진행바(5만원 기준, 프로그레스) → 아이템 리스트(썸네일/사이즈/수량스테퍼/가격/삭제) → 쿠폰 적용 토글(3천원 할인) → 금액 요약(상품금액/할인/배송비/총액) → 하단 고정 결제 버튼.
6. **위시리스트 (Wishlist)** — 헤더 + 즐겨찾기한 상품 2열 그리드 (ProductCard 재사용).
7. **마이 (My)** — 프로필(아바타, 닉네임) → 주문/위시/장바구니 카운트 요약 바 → 메뉴 리스트(배송조회/쿠폰함/적립금/반려견 프로필 관리/고객센터).

공통 **하단 탭바(BottomNav)**: 홈/카테고리/검색/위시/마이 5탭, 활성 탭은 `brand-pink-active` 색상 + bold.

## Components (파일별 소스 = 스펙)
`components/<category>/<Name>.jsx` 각각에 대해 동일 폴더의 `.d.ts`(프롭 타입), `.prompt.md`(사용 예시)를 함께 참고하세요.
- **core**: `Button`(primary 핑크필 필/secondary 아웃라인/ghost 밑줄, 항상 완전 라운드), `IconButton`(원형, outline|filled), `Badge`(new/sale/soldout/neutral), `Chip`(카테고리/필터 토글)
- **forms**: `SearchInput`
- **navigation**: `BottomNav`
- **feedback**: `Toast`(하단 플로팅 확인), `ShippingProgress`(무료배송 유도 바)
- **commerce**: `ProductCard`, `Rating`(별 1개 + 숫자, 잉크색 — 골드 사용 안 함), `SizeSelector`, `FilterSheet`(정렬 바텀시트), `ReviewCard`

## Interactions & Behavior
- 화면 전환: 상태 기반 SPA 전환 → Next.js에서는 `next/navigation`의 라우팅으로 대체, 애니메이션은 필요 시 `framer-motion`으로 얕은 fade/slide 정도만 (브랜드 톤상 과한 모션 지양).
- 장바구니 담기/위시 담기: Toast 1.8초 노출 후 자동 소멸.
- 검색 자동완성: 입력값이 있고 아직 검색 미실행(Enter/클릭) 상태일 때만 드롭다운 표시.
- 필터 바텀시트: 스크림 클릭 또는 적용 버튼으로 닫힘.
- 사이즈 가이드/배송 아코디언: 아코디언 방식, 동시에 하나만 열림(같은 state로 토글).
- 장바구니 무료배송 진행바: `remaining = max(0, 50000 - (subtotal - discount))`, 0 이하면 "무료배송 조건 달성" 메시지.

## State Management
- `favorites: number[]`, `cart: {key, id, title, size, unitPrice, qty}[]`, `recentlyViewed: Product[]` (최근 8개, 중복 제거 후 최상단 삽입), `category`, `screen`(라우팅으로 대체), 검색 화면의 `query/submitted/sort/sheetOpen`, 상품상세의 `size/openPanel`, 장바구니의 `couponApplied`.
- 실제 서비스 연동 시 상품/리뷰/쿠폰 데이터는 API fetch로 교체 필요 (현재는 하드코딩된 목업 6개 상품).

## Design Tokens
`tokens/colors.css`, `tokens/typography.css`, `tokens/spacing.css`, `tokens/radius.css`, `tokens/shadow.css` 파일 그대로 참고 (위 Tailwind 매핑 참고).

## Assets
- 로고: `assets/logo/momentive-logo.jpeg` — 사용자 제공, 원본 그대로 사용 (재창작 금지).
- 상품 사진: 없음 — `<image-slot>` 플레이스홀더 사용 중. 실제 상품 컷 교체 필요.
- 아이콘: 별도 세트 없음 — 유니코드 글리프(⌂ ⌕ ♥ 🛍 ‹ ✕ ★ ▤ ☺)로 대체 중. Lucide/Heroicons 등 실제 아이콘 세트로 교체 권장.
- 폰트: 로고 서체 대체용 Google Fonts `Jua`(디스플레이) + `Noto Sans KR`(본문). 원본 로고 폰트 파일 없음.

## Known gaps (교체 필요)
1. Jua는 로고 실제 서체의 대체 폰트입니다.
2. 아이콘이 전부 유니코드 글리프입니다 — 실제 아이콘 세트로 교체하세요.
3. 상품/리뷰 사진이 전부 placeholder입니다.
4. 상품/리뷰/쿠폰 데이터가 하드코딩되어 있습니다 — API 연동 필요.

## Files
- `DESIGN_SYSTEM.md` — 시스템 전체 요약
- `tokens/*.css` — 디자인 토큰
- `components/**/*.jsx,.d.ts,.prompt.md,*.card.html` — 컴포넌트 소스 + 데모
- `ui_kits/mobile-app/index.html` — 전체 화면 프로토타입 (진짜 소스, 여기서 레이아웃/카피/로직을 그대로 읽어서 재현)
- `assets/logo/momentive-logo.jpeg` — 로고
