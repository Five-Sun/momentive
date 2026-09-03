# 모멘티브 디자인 시스템

`frontend/src/app/globals.css`의 CSS 변수 토큰과 `frontend/src/components/`에 이미 구현된 컴포넌트를 역추출해 정리한 문서다. `docs/domain-overview.md`와 같은 성격의 **살아있는 참고 문서** — spec-format.md/plan-format.md 같은 규격이 아니며, 컴포넌트가 늘어나거나 토큰이 바뀌면 그때그때 갱신한다.

새 화면/컴포넌트를 만들 때는 여기 정의된 토큰과 패턴을 우선 재사용하고, 없는 게 필요하면 이 문서에 먼저 추가한 뒤 구현한다.

## 브랜드 컨셉

- **톤**: 다정하고 부드러운 핑크 계열. 강아지 쇼핑몰다운 따뜻하고 캐주얼한 무드
- **형태 언어**: 버튼/뱃지/칩/검색바/아이콘 버튼 등 인터랙션 요소는 거의 전부 완전 원형(pill/circle)으로 통일 — 각지지 않고 둥근 인상이 브랜드의 핵심
- **타이포**: 디스플레이·본문 공통으로 어비 세현체(UhBeeSehyun, 손글씨 톤 한글 서체) + `Noto Sans KR` fallback 조합으로 브랜드가 지정한 손글씨 톤을 확보 (2차 핸드오프에서 `Jua`를 대체)
- **모션**: 하트 토글·칩 선택·탭 활성화·토스트 등장 같은 "작고 다정한 미세 인터랙션"에 스프링 이징을 사용. 화면 전환처럼 큰 모션에는 쓰지 않음

## 스타일링 기반 기술

**Tailwind CSS v4** (`@import "tailwindcss"`) + CSS 변수 토큰. `globals.css`의 `:root`에 디자인 토큰을 정의하고 `@theme inline`으로 Tailwind 유틸리티(`bg-brand-pink`, `text-ink`, `rounded-lg` 등)에 연결하는 방식. 별도 컴포넌트 라이브러리(shadcn/ui 등) 없이 자체 컴포넌트로 구성.

## 색상 토큰

### Brand
| 토큰 | 값 | 용도 |
|---|---|---|
| `brand-pink` | `#f57ea0` | Primary 버튼 배경, 브랜드 기본색 |
| `brand-pink-active` | `#e8547f` | 눌림/active 상태, 하단 네비 active 텍스트 |
| `brand-pink-deep` | `#d6396a` | 진한 강조 (예약) |
| `brand-pink-soft` | `#fce0ea` | Primary 버튼 disabled 배경 |
| `brand-pink-tint` | `#fff2f6` | 가장 옅은 핑크 틴트 |
| `brand-yellow` | `#fef8c4` | Neutral 톤 뱃지(BEST 등) 배경 |
| `brand-yellow-soft` | `#fffdf0` | 가장 옅은 옐로 틴트 |

### Ink & Neutral
| 토큰 | 값 | 용도 |
|---|---|---|
| `ink` | `#33242b` | 기본 텍스트, NEW 뱃지 배경, 선택된 Chip/SizeSelector 배경 |
| `body` | `#5c454f` | 본문 보조 텍스트 |
| `muted` | `#93818a` | 흐린 텍스트, 아이콘 |
| `muted-soft` | `#c4aeb7` | 정가 취소선 등 가장 흐린 텍스트 |
| `hairline` | `#f3e2e9` | 기본 보더 |
| `hairline-soft` | `#f9edf1` | 더 옅은 보더 |
| `border-strong` | `#e3c3d0` | 강조 보더 |

### Surface
| 토큰 | 값 | 용도 |
|---|---|---|
| `canvas` | `#fffbfc` | 페이지 배경 |
| `surface-soft` | `#fdf1f4` | SearchInput 등 옅은 배경 |
| `surface-card` | `#ffffff` | 카드/컴포넌트 기본 배경 |
| `surface-strong` | `#fbe4ea` | 이미지 플레이스홀더, 품절 뱃지, filled 아이콘 버튼 배경 |

### On-color / Semantic
| 토큰 | 값 | 용도 |
|---|---|---|
| `on-brand` | `#2b2621` | 브랜드 색 위 텍스트(Primary 버튼 텍스트) |
| `on-dark` | `#ffffff` | 어두운 배경 위 텍스트 |
| `success` | `#5a8a6b` | 성공 상태 |
| `error` | `#c1543b` | 에러 상태 |
| `sale` | `#e8547f` | 할인 뱃지/가격 강조 (`brand-pink-active`와 동일값) |
| `scrim` | `rgba(51,36,43,0.5)` | 모달/오버레이 딤 |

## 타이포그래피

- **Display 폰트**: 어비 세현체(UhBeeSehyun, `--font-uhbee`) — 제목/히어로 문구
- **Body 폰트**: 어비 세현체(UhBeeSehyun, `--font-uhbee`) — 본문/UI 텍스트 전반 (display와 동일 서체, 1차 이관과 달리 두 토큰 모두 같은 폰트를 가리킴)
- **Fallback**: `Noto Sans KR` (`--font-noto-sans-kr`) — self-host 폰트 로드 실패 시, 또는 어비 세현체 미수록 글리프(한글 2,449자 외) 발생 시
- **출처/라이선스**: `frontend/public/fonts/UhBeeSehyun.woff2` self-host. 웹사이트·임베딩·상업사용 허용(`fsType=8`). 외부 CDN(`cdn.jsdelivr.net` 등) 직접 참조는 사용하지 않는다
- **로드 방식**: `frontend/src/app/layout.tsx`에서 `next/font/local`로 로드(`display: "swap"`), CSS 변수 `--font-uhbee`로 주입. `Noto Sans KR`은 `next/font/google`로 유지
- **weight**: 어비 세현체는 **Regular(400) 1종만 존재**(Bold 미배포). 아래 스케일 중 weight 500 이상 7종은 브라우저 synthetic bold로 렌더된다
- 타이포 스케일 10종의 크기·줄높이·weight 값은 1차 이관과 **동일** (이번 폰트 교체로 변경되지 않음)

| 유틸리티 클래스 | 스타일 | 용도 | synthetic bold 여부 |
|---|---|---|---|
| `text-display-lg` | 600 30px/1.25, display 폰트 | 큰 제목 | O |
| `text-display-md` | 400 24px/1.3, display 폰트 | 페이지 제목 | X |
| `text-title` | 700 18px/1.35 | 섹션 제목 | O |
| `text-title-sm` | 600 16px/1.4 | 상품명 등 | O |
| `text-body` | 400 15px/1.55 | 기본 본문 | X |
| `text-body-sm` | 400 13px/1.5 | 보조 본문 | X |
| `text-caption` | 500 12px/1.4 | 캡션, 평점 개수 | O (육안 확인 게이트 대상) |
| `text-price` | 700 17px/1.3 | 가격 강조 | O |
| `text-button` | 600 15px/1 | 버튼 텍스트 | O |
| `text-tag` | 600 11px/1 | 뱃지/태그 | O (육안 확인 게이트 대상) |

### 굵기 육안 확인 게이트 (완료, 2026-09-03)

작은 크기(11~12px)에서 synthetic bold는 획이 뭉개져 보일 위험이 가장 크다는 우려로 도입한 수동 게이트.

- **대상**: `text-tag`(11px, weight 600), `text-caption`(12px, weight 500)
- **확인 위치**: 홈·상품상세·장바구니(390px/1024px/1440px)와 `/style-guide` 타이포그래피 스케일 샘플에서 실제 렌더링된 뱃지/태그/캡션 텍스트, 3배 확대 클로즈업 스크린샷 포함
- **판정 결과**: **뭉개짐 없음 — 평탄화 불필요**. 어비 세현체 자체가 손글씨체 특성상 획이 두꺼워 synthetic bold가 적용돼도 두 토큰 모두 또렷하게 판독됨
- **결정**: 위 표의 weight 값은 원안(`text-tag` 600, `text-caption` 500) 그대로 유지. 코드 변경 없음

## 레이아웃 토큰

- **Radius**: `xs` 6px / `sm` 10px / `md` 16px / `lg` 22px — `rounded-xs`~`rounded-lg` 유틸리티로 매핑됨
  - ⚠ 완전 원형(`rounded-full`)이 버튼/칩/뱃지/아이콘버튼/검색바의 기본값이고, 위 4단계는 카드·이미지 등 각진 요소에 쓰임
  - `ProductCard`의 이미지 컨테이너는 현재 Tailwind 기본 `rounded-2xl`(16px)을 쓰고 있는데, 값이 우연히 `radius-md`(16px)와 같음 — 새로 만들 때는 기본 `rounded-2xl` 대신 토큰인 `rounded-md`/`rounded-lg`를 명시적으로 쓰는 걸 권장
- **Shadow**: `shadow-card`(옅음, 이미지 위 뜬 아이콘 버튼 등) / `shadow-float`(강함, Toast 등 완전히 떠 있는 요소). `shadow-float`는 toast/bottom sheet 전용 토큰이므로 앱 셸 등 다른 곳에 쓰지 않는다
- **Spacing**: Tailwind 기본 4px 스텝 스케일 그대로 사용, `--spacing-section`(64px)만 섹션 간격용으로 추가 정의

## 모션 토큰 (`globals.css`)

미세 인터랙션 전용. 화면 전환 등 큰 모션에는 사용하지 않는다. `@media (prefers-reduced-motion: reduce)`에서 전부 비활성화된다.

| 토큰 | 정의 | Tailwind 유틸 | 적용 지점 |
|---|---|---|---|
| `--ease-spring` | `cubic-bezier(.34,1.56,.64,1)` | (트랜지션에 직접 사용) | Toast 등장(translateY + scale) |
| `paw-pop` (`@keyframes`) | scale 1 → 1.28(45%) → .94(70%) → 1 | `animate-paw-pop` | 위시 하트 토글(`ProductCard`), 칩 선택(`Chip`), `Badge` 노출 |
| `bump-up` (`@keyframes`) | translateY 0 → -3px & scale 1.15(50%) → 0 | `animate-bump-up` | 하단 탭 활성화(`BottomNav`) |

- `--animate-paw-pop`/`--animate-bump-up`을 Tailwind v4 `@theme inline`의 `--animate-*` 키로 노출해 `animate-paw-pop`/`animate-bump-up` 유틸리티가 자동 생성된다.
- Framer Motion 등 모션 전용 신규 의존성은 추가하지 않는다. 순수 CSS `@keyframes` + Tailwind 유틸로만 구현한다.
- 적용 지점(`ProductCard` 하트, `Chip`, `Badge`, `BottomNav`, `Toast`)까지 전부 반영 완료.

## 브레이크포인트 규칙

`(shell)` 하위 전체 라우트에 적용되는 반응형 규칙. 데스크톱 반응형이 없던 1차 이관과 달리, 2차 이관부터 전 화면이 아래 3구간을 따른다.

| 구간 | 네비게이션 | 콘텐츠 폭 | 상품 그리드 |
|---|---|---|---|
| < 1024px | 하단 탭바 5탭(`GlobalBottomNav`) | 최대 480px 중앙 정렬(`max-w-[480px] mx-auto`), 배경 `canvas` | 2열 |
| ≥ 1024px | 상단 가로 네비(`TopNav`) 4링크 + 검색창 | 최대 1400px 중앙 정렬(`max-w-[1400px] mx-auto`), 좌우 40px 패딩 | 3열 |
| ≥ 1280px | 상단 가로 네비(동일) | 최대 1400px(동일) | 4열 |

- 앱 셸(`(shell)/layout.tsx`)은 더 이상 `bg-surface-strong` 배경 + `max-w-[480px]` 흰 프레임 + `shadow-float` 구조를 쓰지 않는다. 1024px 미만/이상에 따라 위 표의 컨테이너 규칙을 적용한 반응형 컨테이너로 대체됐다.
- `TopNav`는 ≥1024px에서만 렌더, `GlobalBottomNav`는 <1024px에서만 렌더 — 두 네비가 동시에 보이지 않는다.
- `GlobalBottomNav`의 `HIDDEN_PREFIXES`(`/checkout`, `/mypage/orders/`) 우회는 <1024px 렌더링 자체가 없는 ≥1024px 구간에는 적용되지 않는다(하단 탭바가 아예 없으므로).
- 데스크톱에서 `fixed` 하단 CTA 바를 쓰는 화면은 없다 — <1024px에서 `fixed`였던 CTA는 ≥1024px에서 콘텐츠 흐름에 인라인으로 배치되거나(예: 로그인/주문상세) sticky 요약 컬럼 내부로 옮겨진다(예: 장바구니/체크아웃).
- 화면별 데스크톱 레이아웃 규칙(그리드/2단/sticky 등 화면 단위 상세)은 `docs/specs/2026-09-02-responsive-design-handoff.md`의 "화면별 데스크톱 레이아웃" 표를 1차 근거로 삼는다. 이 문서는 전역 규칙만 다룬다.

## 컴포넌트 (`frontend/src/components/`)

기본 카테고리는 `core`, `commerce`, `forms`, `navigation`, `feedback`, `skeleton`이다(`frontend/CLAUDE.md` 필수 컨벤션). 새 UI를 만들기 전 아래 목록에서 같은 역할의 컴포넌트가 있는지 먼저 확인한다.

### core — 범용 UI 프리미티브

| 컴포넌트 | 경로 | 핵심 패턴 | 사용 지침 |
|---|---|---|---|
| `Button` | `core/Button.tsx` | `rounded-full`. variant: primary(핑크 배경)/secondary(흰 배경+ink 보더)/ghost(밑줄 텍스트). size: md(h-12)/sm(h-[38px]) | CTA·폼 submit 등 클릭 액션 전반. 화면마다 커스텀 버튼을 새로 만들지 않는다 |
| `IconButton` | `core/IconButton.tsx` | 원형, size prop(기본 40px). variant: outline(흰 배경+hairline 보더)/filled(surface-strong+shadow-card). active 시 아이콘 색이 brand-pink | 뒤로가기, 상품 이미지 위 플로팅 아이콘 등 |
| `Badge` | `core/Badge.tsx` | `rounded-full` pill. tone: new(ink 배경)/sale(sale 배경)/soldout(surface-strong 배경)/neutral(yellow 배경) | 상품 카드 좌상단 뱃지. `text-tag` 사용, 노출 시 `animate-paw-pop` 적용 |
| `Chip` | `core/Chip.tsx` | pill. selected 시 ink 배경+흰 텍스트, 아니면 흰 배경+hairline 보더 | 카테고리/필터 토글. 선택 시 `animate-paw-pop` 적용 |

### forms — 입력 필드

| 컴포넌트 | 경로 | 핵심 패턴 | 사용 지침 |
|---|---|---|---|
| `SearchInput` | `forms/SearchInput.tsx` | `rounded-full`, surface-soft 배경 + hairline 보더, 검색 아이콘 prefix | 검색 화면·`TopNav` 검색창의 기반. 모바일 전용 검색 행(`.mobile-search-row` 상당)은 데스크톱에서 숨긴다 |
| `TextField` | `forms/TextField.tsx` | RHF 필드 컴포넌트, 에러 메시지 통일 렌더링 | 일반 텍스트 입력. `useState` 수동 폼 상태를 새로 만들지 않는다 |
| `PasswordField` | `forms/PasswordField.tsx` | `TextField` 변형, 마스킹 토글 | 비밀번호 입력 |
| `AddressFields` | `forms/AddressFields.tsx` | 배송지 입력 필드 그룹 | 회원가입/체크아웃 배송지 입력 |

### commerce — 커머스 도메인 컴포넌트

| 컴포넌트 | 경로 | 핵심 패턴 | 사용 지침 |
|---|---|---|---|
| `ProductCard` | `commerce/ProductCard.tsx` | 이미지 `aspect-square` + surface-strong 배경(플레이스홀더), 좌상단 뱃지, 우상단 원형 하트 버튼(흰 반투명+shadow-card), 하단 제목/평점/가격(할인 시 정가 취소선) | 상품 그리드의 기본 카드. 하트 토글에 `animate-paw-pop` 적용, `e.stopPropagation()`으로 카드 클릭과 분리 |
| `ProductGridItem` | `commerce/ProductGridItem.tsx` | `ProductCard`를 그리드 셀에 배치하는 래퍼 | 홈/검색/위시리스트가 공유하는 상품 그리드 셀. "반복 패턴 — ProductGrid" 참고 |
| `ProductMiniCard` | `commerce/ProductMiniCard.tsx` | 소형 카드(최근 본 상품 등 가로 레일용) | 가로 스크롤 레일 |
| `ProductImage` | `commerce/ProductImage.tsx` | 상품 이미지 플레이스홀더 렌더링 | 실제 상품 사진 미도입 구간의 공통 플레이스홀더 |
| `ProductDetailView` | `commerce/ProductDetailView.tsx` | 상품상세 전체 뷰(이미지·정보·CTA·리뷰) | `/products/[id]`의 데스크톱 좌 이미지 sticky / 우 정보 2단 레이아웃 분기 지점 |
| `Rating` | `commerce/Rating.tsx` | 별 아이콘 + 굵은 평점 + 흐린 리뷰 수, 인라인 컴팩트 | 실제 집계 평점만 사용. 하드코딩 평점을 넣지 않는다 |
| `SizeSelector` | `commerce/SizeSelector.tsx` | 정사각형에 가까운 `rounded-[10px]`(radius-sm) 버튼, selected 시 ink 배경+2px 보더 | 상품상세 사이즈 선택 |
| `FilterSheet` | `commerce/FilterSheet.tsx` | 바텀시트형 정렬/필터 | `/search` 정렬 바텀시트. 데스크톱에서도 유지(삭제하지 않음) |
| `ReviewCard` | `commerce/ReviewCard.tsx` | 리뷰 행, 선택적 사진 그리드 | 상품상세 리뷰 목록 |
| `ReviewForm` | `commerce/ReviewForm.tsx` | 리뷰 작성/수정 폼 | 리뷰 작성·수정 플로우 |

### navigation

| 컴포넌트 | 경로 | 핵심 패턴 | 사용 지침 |
|---|---|---|---|
| `BottomNav` | `navigation/BottomNav.tsx` | 하단 고정 바, active 탭은 brand-pink-active 텍스트 + 굵은 라벨. 활성 아이콘에 `animate-bump-up` | <1024px 전용. `items`/`activeKey`를 받는 프레젠테이션 컴포넌트 |
| `GlobalBottomNav` | `navigation/GlobalBottomNav.tsx` | `BottomNav`에 5탭(홈/카테고리/검색/위시/마이) 라우팅을 연결. `HIDDEN_PREFIXES`로 자체 CTA 화면에서 숨김 | <1024px에서만 렌더 |
| `TopNav` | `navigation/TopNav.tsx` | 높이 80px, 하단 `border-hairline` 1px. 좌측 로고(38px 원형)+링크 4개(홈/카테고리/위시/마이), 우측 검색 입력창(h-42px w-300px, surface-soft pill)+장바구니 아이콘(수량 배지 brand-pink-active). 활성 링크는 brand-pink-active+bold | ≥1024px에서만 렌더. 검색은 상시 노출 입력창이므로 링크 목록에서 제외 |

### feedback

| 컴포넌트 | 경로 | 핵심 패턴 | 사용 지침 |
|---|---|---|---|
| `Toast` | `feedback/Toast.tsx` | ink 배경 pill, 화면 하단 중앙 고정. 등장 시 `--ease-spring`으로 translateY+scale 트랜지션, 1.8초 뒤 자동 소멸 | 전역 알림(장바구니 담기 등). `ApiError`에 `fieldErrors`가 없을 때 에러 표시에도 사용 |
| `ShippingProgress` | `feedback/ShippingProgress.tsx` | 진행 바 + 문구, 무료배송 임계값 기준 진행률 표시 | 무료배송 임계값은 **70,000원**(실운영 값). 레퍼런스의 50,000원을 유입하지 않는다. 폭 의존 레이아웃이 데스크톱에서 늘어져 깨지지 않는지 점검 대상 |

### skeleton

| 컴포넌트 | 경로 | 핵심 패턴 | 사용 지침 |
|---|---|---|---|
| `ProductCardSkeleton` | `skeleton/ProductCardSkeleton.tsx` | `ProductCard`와 동일 치수의 로딩 플레이스홀더 | 상품 그리드 로딩 상태. `ProductGrid` 열 수 규칙과 동일하게 배치 |

전체 사용 예시는 `frontend/src/app/style-guide/page.tsx`(`/style-guide` 라우트)에서 실제 렌더링 결과를 확인할 수 있다.

## 반복 패턴

아직 별도 컴포넌트로 추출되지 않은 채 여러 화면에서 같은 마크업이 반복되는 패턴. 새 화면을 만들 때 아래 패턴을 재발명하지 말고 기존 화면의 구현을 그대로 참고한다. 추출이 필요해지면(3회 이상 반복 + 변경 예정) `core`/`navigation` 등 적절한 카테고리로 컴포넌트화한다.

| 패턴 | 현재 등장 위치(예) | 형태 |
|---|---|---|
| `AppHeader` | `cart`, `checkout`, `checkout/payment`, `mypage/pets`, `mypage/coupons`, `mypage/support`, `mypage/orders`, `mypage/orders/[orderId]` | `h-13`(52px) `border-hairline border-b` 상단 바, 좌측 뒤로가기 `IconButton` + 중앙/좌측 제목. 폼·목록·문서 화면(레퍼런스 없는 11개 화면)의 공통 헤더 |
| `BottomActionBar` | `cart`, `checkout`, `checkout/payment`, `mypage/orders/[orderId]` | `border-hairline border-t` + `bg-surface-card`, <1024px에서 `fixed bottom-0 max-w-[480px]`로 화면 하단 고정. **≥1024px에서는 `fixed`를 풀고** 콘텐츠 흐름 인라인 배치(단독 CTA 화면) 또는 우측 요약 컬럼 내부 sticky(장바구니/체크아웃)로 전환 |
| `SelectableCircle` | `cart`(수량/전체선택), `checkout`(배송지/쿠폰 선택) | `size-5`류 원형 버튼, 선택 시 `bg-brand-pink border-brand-pink`, 아니면 `border-hairline bg-surface-card`. 라디오/체크박스 대체 UI |
| `SummaryRows` | `cart`, `checkout`, `mypage/orders/[orderId]` | `flex justify-between` 라벨-금액 행 반복(상품금액/배송비/할인/총 결제금액). 데스크톱 우측 340px 요약 컬럼(`.cart-layout` 상당)의 핵심 콘텐츠 |
| `ProductGrid` | `(shell)/page.tsx`(홈), `search`, `wishlist` | `ProductGridItem`을 감싸는 그리드 컨테이너. **<1024px 2열 / ≥1024px 3열 / ≥1280px 4열**로 통일. 이 문서의 "브레이크포인트 규칙" 표와 반드시 일치시킨다 |

## 사용 방법

- 새 화면/컴포넌트 작업 전 이 문서에서 재사용할 토큰·컴포넌트가 있는지 먼저 확인한다
- 컴포넌트나 토큰이 추가/변경되면 이 문서도 함께 갱신한다 (코드가 원본, 이 문서는 그 요약)
- 이후 이 문서를 기반으로 디자인 시스템 스킬화, `frontend/CLAUDE.md`의 "스타일링 방식" 항목 작성을 진행할 예정 (Todo.md 참고)
