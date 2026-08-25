# 모멘티브 디자인 시스템

`frontend/src/app/globals.css`의 CSS 변수 토큰과 `frontend/src/components/`에 이미 구현된 컴포넌트를 역추출해 정리한 문서다. `docs/domain-overview.md`와 같은 성격의 **살아있는 참고 문서** — spec-format.md/plan-format.md 같은 규격이 아니며, 컴포넌트가 늘어나거나 토큰이 바뀌면 그때그때 갱신한다.

새 화면/컴포넌트를 만들 때는 여기 정의된 토큰과 패턴을 우선 재사용하고, 없는 게 필요하면 이 문서에 먼저 추가한 뒤 구현한다.

## 브랜드 컨셉

- **톤**: 다정하고 부드러운 핑크 계열. 강아지 쇼핑몰다운 따뜻하고 캐주얼한 무드
- **형태 언어**: 버튼/뱃지/칩/검색바/아이콘 버튼 등 인터랙션 요소는 거의 전부 완전 원형(pill/circle)으로 통일 — 각지지 않고 둥근 인상이 브랜드의 핵심
- **타이포**: 제목/디스플레이용 `Jua`(둥글고 귀여운 한글 디스플레이 폰트) + 본문용 `Noto Sans KR` 조합으로 캐주얼함과 가독성을 동시에 확보

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

- **Display 폰트**: `Jua` (`--font-jua`) — 제목/히어로 문구
- **Body 폰트**: `Noto Sans KR` (`--font-noto-sans-kr`) — 본문/UI 텍스트 전반
- 폰트는 `layout.tsx`에서 `next/font/google`로 로드, CSS 변수로 주입

| 유틸리티 클래스 | 스타일 | 용도 |
|---|---|---|
| `text-display-lg` | 600 30px/1.25, display 폰트 | 큰 제목 |
| `text-display-md` | 400 24px/1.3, display 폰트 | 페이지 제목 |
| `text-title` | 700 18px/1.35 | 섹션 제목 |
| `text-title-sm` | 600 16px/1.4 | 상품명 등 |
| `text-body` | 400 15px/1.55 | 기본 본문 |
| `text-body-sm` | 400 13px/1.5 | 보조 본문 |
| `text-caption` | 500 12px/1.4 | 캡션, 평점 개수 |
| `text-price` | 700 17px/1.3 | 가격 강조 |
| `text-button` | 600 15px/1 | 버튼 텍스트 |
| `text-tag` | 600 11px/1 | 뱃지/태그 |

## 레이아웃 토큰

- **Radius**: `xs` 6px / `sm` 10px / `md` 16px / `lg` 22px — `rounded-xs`~`rounded-lg` 유틸리티로 매핑됨
  - ⚠ 완전 원형(`rounded-full`)이 버튼/칩/뱃지/아이콘버튼/검색바의 기본값이고, 위 4단계는 카드·이미지 등 각진 요소에 쓰임
  - `ProductCard`의 이미지 컨테이너는 현재 Tailwind 기본 `rounded-2xl`(16px)을 쓰고 있는데, 값이 우연히 `radius-md`(16px)와 같음 — 새로 만들 때는 기본 `rounded-2xl` 대신 토큰인 `rounded-md`/`rounded-lg`를 명시적으로 쓰는 걸 권장
- **Shadow**: `shadow-card`(옅음, 이미지 위 뜬 아이콘 버튼 등) / `shadow-float`(강함, Toast 등 완전히 떠 있는 요소)
- **Spacing**: Tailwind 기본 4px 스텝 스케일 그대로 사용, `--spacing-section`(64px)만 섹션 간격용으로 추가 정의

## 컴포넌트 패턴 (`frontend/src/components/`)

| 컴포넌트 | 경로 | 핵심 패턴 |
|---|---|---|
| `Button` | `core/Button.tsx` | `rounded-full`. variant: primary(핑크 배경)/secondary(흰 배경+ink 보더)/ghost(밑줄 텍스트). size: md(h-12)/sm(h-[38px]) |
| `IconButton` | `core/IconButton.tsx` | 원형, size prop(기본 40px). variant: outline(흰 배경+hairline 보더)/filled(surface-strong+shadow-card). active 시 아이콘 색이 brand-pink |
| `Badge` | `core/Badge.tsx` | `rounded-full` pill. tone: new(ink 배경)/sale(sale 배경)/soldout(surface-strong 배경)/neutral(yellow 배경) |
| `Chip` | `core/Chip.tsx` | pill. selected 시 ink 배경+흰 텍스트, 아니면 흰 배경+hairline 보더 |
| `SearchInput` | `forms/SearchInput.tsx` | `rounded-full`, surface-soft 배경 + hairline 보더, 검색 아이콘(⌕) prefix |
| `ProductCard` | `commerce/ProductCard.tsx` | 이미지 `aspect-square` + surface-strong 배경(플레이스홀더), 좌상단 뱃지, 우상단 원형 하트 버튼(흰 반투명+shadow-card), 하단 제목/평점/가격(할인 시 정가 취소선) |
| `Rating` | `commerce/Rating.tsx` | 별 아이콘 + 굵은 평점 + 흐린 리뷰 수, 인라인 컴팩트 |
| `SizeSelector` | `commerce/SizeSelector.tsx` | 정사각형에 가까운 `rounded-[10px]`(radius-sm) 버튼, selected 시 ink 배경+2px 보더 |
| `BottomNav` | `navigation/BottomNav.tsx` | 하단 고정 바, active 탭은 brand-pink-active 텍스트 + 굵은 라벨 |
| `Toast` | `feedback/Toast.tsx` | ink 배경 pill, 화면 하단 중앙 고정, opacity+translateY 트랜지션으로 페이드인 |

전체 사용 예시는 `frontend/src/app/style-guide/page.tsx`(`/style-guide` 라우트)에서 실제 렌더링 결과를 확인할 수 있다.

## 사용 방법

- 새 화면/컴포넌트 작업 전 이 문서에서 재사용할 토큰·컴포넌트가 있는지 먼저 확인한다
- 컴포넌트나 토큰이 추가/변경되면 이 문서도 함께 갱신한다 (코드가 원본, 이 문서는 그 요약)
- 이후 이 문서를 기반으로 디자인 시스템 스킬화, `frontend/CLAUDE.md`의 "스타일링 방식" 항목 작성을 진행할 예정 (Todo.md 참고)
