# 모멘티브 디자인 시스템

강아지 쇼핑몰이라는 도메인에 맞춰, 따뜻하고 친근하면서도 신뢰감 있는 톤을 지향한다. 실제 코드(`frontend/src/app/globals.css`, `frontend/src/components/`)에 이미 구현된 토큰·컴포넌트를 기준으로 정리한 문서이며, 새 화면/컴포넌트를 만들 때 이 문서를 기준 삼아 톤을 통일한다. 실물 확인은 `/style-guide` 페이지에서 가능하다.

## 브랜드 무드

- **따뜻함·친근함**: 강아지를 키우는 보호자가 편안하게 느낄 수 있는 톤. 채도 높은 원색보다 부드러운 핑크·옐로 파스텔 계열을 기본으로 한다.
- **아기자기함**: 큰 radius(pill 버튼, 22px 카드)와 부드러운 shadow로 각진 느낌을 지운다.
- **손글씨 같은 다정함**: 제목/디스플레이 텍스트에 Jua(손글씨풍 한글 폰트)를 써서 딱딱한 쇼핑몰 느낌을 줄인다.

## 색상 팔레트

### Brand
| 토큰 | 값 | 용도 |
|---|---|---|
| `brand-pink` | `#f57ea0` | 기본 브랜드 컬러, primary 버튼/포인트 |
| `brand-pink-active` | `#e8547f` | hover/active, 찜(favorite) 활성 상태, sale 뱃지 |
| `brand-pink-deep` | `#d6396a` | 강조가 더 필요한 경우 |
| `brand-pink-soft` | `#fce0ea` | 비활성 버튼 배경 등 옅은 배경 |
| `brand-pink-tint` | `#fff2f6` | 아주 옅은 틴트 배경 |
| `brand-yellow` | `#fef8c4` | 서브 포인트 컬러 |
| `brand-yellow-soft` | `#fffdf0` | 옅은 옐로 배경 |

### Ink & Neutrals
텍스트/보더용 무채색 계열이되, 완전한 흑백이 아니라 핑크 톤이 살짝 섞인 웜톤 그레이(`ink #33242b`, `body #5c454f`, `muted #93818a`)를 써서 브랜드 톤과 이질감이 없게 한다. `hairline`, `border-strong`은 구분선/보더용.

### Surface
`canvas`(페이지 배경) → `surface-soft` → `surface-card`(흰색, 카드) → `surface-strong`(핑크빛 강조 배경) 순으로 옅은 배경부터 강조 배경까지 단계화.

### Semantic
`success`(#5a8a6b), `error`(#c1543b), `sale`(#e8547f, brand-pink-active와 동일 — 할인은 브랜드 컬러로 표현). 일반적인 초록/빨강 대신 채도를 낮춰 브랜드 톤에서 튀지 않게 했다.

## 타이포그래피

두 폰트를 역할로 분리한다.

- **`--font-display` (Jua)**: 제목, 디스플레이 텍스트 전용. `text-display-lg`(30px), `text-display-md`(24px).
- **`--font-body` (Noto Sans KR)**: 본문 전체. `text-title`(18px/700), `text-title-sm`(16px/600), `text-body`(15px), `text-body-sm`(13px), `text-caption`(12px), `text-price`(17px/700 — 가격 강조), `text-button`(15px/600), `text-tag`(11px/600).

Tailwind 기본 `font-*` 유틸로는 size/weight/line-height를 한 토큰으로 묶기 어려워, 컴포지트 클래스(`.text-*`)를 `globals.css`의 `@layer utilities`에 별도 정의해 사용한다.

## 형태 언어

- **Radius**: `xs`(6px) ~ `lg`(22px) 4단계. 카드·이미지처럼 면적이 큰 요소일수록 큰 radius(`lg`)를, 버튼은 `rounded-full`(pill)을 쓴다.
- **Shadow**: `shadow-card`(카드 기본 그림자), `shadow-float`(플로팅 요소, 버튼 위 아이콘 등) 2단계. 그림자 색은 검정이 아니라 `ink` 계열(`rgba(43,38,33,...)`)로 톤을 맞춘다.

## 컴포넌트

카테고리별로 분류되어 있으며 (`frontend/src/components/<category>/`), `/style-guide` 페이지에서 실물을 확인할 수 있다.

| 카테고리 | 컴포넌트 |
|---|---|
| `core` | Button, Badge, Chip, IconButton |
| `commerce` | ProductCard, Rating, SizeSelector |
| `forms` | SearchInput |
| `navigation` | BottomNav |
| `feedback` | Toast |

공통 원칙:
- 버튼은 항상 pill 형태(`rounded-full`), variant는 `primary`(브랜드 핑크 배경) / `secondary`(테두리) / `ghost`(밑줄 텍스트) 3종.
- 뱃지(`Badge`)는 `tone`으로 의미 구분: `new`, `sale`, `soldout`, `neutral`.
- 카드류(`ProductCard`)는 이미지 영역에 `surface-strong` 배경 + `rounded-2xl`, 찜 버튼은 우상단에 원형으로 플로팅.
