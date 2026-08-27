---
date: 2026-08-26
feature: app-redesign
spec: 2026-08-26-app-redesign.md
status: done
---

# 앱 전체 재디자인 (7개 화면) 플랜

## 개요

`docs/specs/2026-08-26-app-redesign.md`를 기반으로, Claude Design 핸드오프(`design_handoff_momentive_app/`, `DesignSync` MCP `get_file`/`list_files`, projectId `f05007c9-8716-43a5-b06f-1982d8a1b595`)를 근거 자료로 삼아 홈·카테고리·검색·상품상세·장바구니·위시리스트·마이 7개 화면을 재구현한다. 플랜 문서 자체에는 핸드오프 파일 내용을 옮겨적지 않는다 — 각 phase를 실행하는 구현 에이전트가 필요한 시점에 `DesignSync get_file`로 직접 읽는다 (핵심 파일: `README.md`, `ui_kits/mobile-app/index.html`, `tokens/*.css`, `components/<category>/<Name>.{jsx,d.ts,prompt.md}`).

`docs/backlog/`를 훑었으나 `feature: app-redesign` 또는 `category: frontend`/`category: backend` 관련 과거 실패 항목은 없었다 (신규 feature이므로 예상된 결과). 따라서 이번 플랜에 반영할 기존 재발 방지 체크리스트는 없고, 대신 이번 실행에서 발견되는 실패는 `docs/backlog/` 규격에 맞춰 각 phase 담당 reviewer가 새로 기록해야 한다.

Phase 순서는 스펙의 "사용자 시나리오" 순서를 따르되, 화면 간 의존관계를 반영해 다음과 같이 조정했다.

1. **Phase 0**: 이후 모든 화면 phase의 공통 기반 — 백엔드 `category`/`sort` 확장, 디자인 토큰, 아이콘 세트, BottomNav 5탭 전환, localStorage 유틸 레이어. 화면 phase들이 이 기반 위에서 작업하므로 항상 먼저 끝나 있어야 한다.
2. **Phase 1 (홈)**: 기존 홈 화면을 확장하는 작업이라 두 번째로 배치. 단, "최근 본 상품" 실제 누적은 상품상세(Phase 3)에서 붙는 기능이므로, 이 phase에서는 섹션 렌더링만 구현하고 검증은 devtools로 localStorage를 직접 채워 진행한다.
3. **Phase 2 (카테고리+검색)**: 카테고리 화면이 검색 화면으로 바로 연결되는 흐름이라 하나의 phase로 묶었다 — 카테고리 화면만 따로는 "탭하면 검색 결과가 제대로 필터링되는지"를 검증할 수 없기 때문이다.
4. **Phase 3 (상품상세)**: 위시/장바구니/최근 본 상품 등 localStorage 쓰기 로직이 처음 실제로 동작하는 phase. 여기서 Phase 1에 남겨둔 "최근 본 상품 실제 누적"도 재검증한다.
5. **Phase 4 (장바구니)**: Phase 3에서 담긴 아이템을 소비하는 화면.
6. **Phase 5 (위시리스트)**: Phase 3에서 토글된 위시 데이터를 소비하는 화면.
7. **Phase 6 (마이) + 전체 회귀**: 위시/장바구니 카운트를 소비하는 마지막 화면이자, 스펙 전체 수용 기준을 최종 재검증하는 phase.

## Phase 0: 공통 기반 (백엔드 확장 + 디자인 토큰 + 아이콘 + BottomNav 5탭 + localStorage 유틸)

이 phase가 끝나면, `GET /products`가 `category`/`sort` 파라미터를 지원하고, 프론트 전역에 실제 아이콘 세트와 핸드오프 디자인 토큰이 반영되며, `GlobalBottomNav`가 5탭 구성(홈/카테고리/검색/위시/마이)으로 전환되고, 이후 화면 phase들이 재사용할 localStorage 유틸이 준비된다. `/category`, `/wishlist`는 이 phase 시점엔 아직 라우트가 없어 404가 나는 것이 정상이다.

- [x] `backend`: `Product` 엔티티에 `category`(`OUTER`/`KNIT`/`INNERWEAR`/`ACCESSORY`) 필드 추가, 관련 DTO(`ProductSummaryResponse`/`ProductDetailResponse`)에 노출, 기존 상품 데이터에 대한 마이그레이션/시드값 처리 (`V3__add_product_category.sql`, 기존 시드 15건 중 3건 `OUTER` 나머지 `ACCESSORY` — 시드 상품 대부분이 의류가 아니라 카테고리 분포가 쏠려 있음, 실제 의류 상품 추가 전까지 알려진 한계)
- [x] `backend`: `GET /products`에 `category`(선택), `sort`(선택, 기본값 `new` — `new`/`popular`/`price_asc`/`price_desc`, `popular`은 `new`와 동일 결과) 쿼리 파라미터 추가. 응답 페이지네이션 메타데이터 형태는 변경 없음 (잘못된 `sort` 값은 400 + `ErrorCode.INVALID_SORT`)
- [x] `backend`: `ProductService` 단위/통합 테스트로 `category` 필터링, 4가지 `sort` 값 각각의 정렬 결과, `popular`==`new` 동일성 검증 (`./gradlew test` 통과, 9/9 — 기존에 있던 Windows 클록 해상도 관련 flaky 테스트도 함께 수정)
- [x] `frontend`: `lucide-react`(또는 동등 아이콘 라이브러리) 설치, 기존 화면(홈/상품상세/BottomNav 등)의 유니코드 글리프 아이콘을 실제 아이콘 컴포넌트로 전면 교체
- [x] `frontend`: DesignSync `tokens/*.css`(colors/typography/spacing/radius/shadow)를 `get_file`로 읽어 `frontend/src/app/globals.css`의 `@theme` 토큰과 diff, 핸드오프 기준으로 갱신 (기존 `/style-guide`에서 컴포넌트가 깨지지 않는지 확인) — 메인 세션에서 DesignSync로 5개 토큰 파일 전부 직접 대조 완료: 색상/타이포/radius/shadow 값 전부 동일(변수명만 다름, `docs/design.md`에 매핑 기록됨), spacing은 핸드오프 값(4/8/12/16/24/32/48px)이 Tailwind 기본 스케일과 1:1 대응해 이미 커버됨, `radius-full`/`shadow-none`도 Tailwind 기본 유틸리티로 커버됨 — 변경 불필요 확인
- [x] `frontend`: `frontend/src/components/navigation/GlobalBottomNav.tsx`를 홈(`/`)/카테고리(`/category`)/검색(`/search`)/위시(`/wishlist`)/마이(`/mypage`) 5탭으로 재구성 (아이콘은 실제 아이콘 세트, `BottomNav.tsx` 자체 props 구조는 변경하지 않음)
- [x] `frontend`: `frontend/src/lib/storage/`에 localStorage 기반 유틸 구현 — 위시리스트(상품 id 배열, 토글 함수), 장바구니(`{key,id,title,size,unitPrice,qty}[]`, 추가/수량변경/삭제), 최근 본 상품(최근 8개, 중복 제거 후 최상단 삽입), 최근검색어(추가/조회). 각 유틸은 이후 phase에서 그대로 import해서 쓸 수 있는 형태
- [x] 검증: `./gradlew test` 통과, `npm run build`/`npm run lint` 통과. Playwright 기반 브라우저 검증 완료 — BottomNav 5탭이 아이콘과 함께 표시되고 현재 경로 탭이 활성 스타일(핑크+굵게)로 강조됨을 홈/카테고리/검색/위시/마이 전 화면에서 확인. `GET /products?category=OUTER` 등 조합 정상 응답 확인 (검증 중 로컬 DB에 시드 데이터가 비어있는 문제 발견 → 볼륨 재생성으로 해결, 코드 문제 아님)

## Phase 1: 홈 (`/`)

이 phase가 끝나면, 홈 화면에 프로모 배너·인기 랭킹 가로스크롤·카테고리 칩 필터·최근 본 상품 가로스크롤이 스펙 순서대로 표시되고, 카테고리 칩 탭이 실제 그리드 필터링으로 이어진다.

- [x] 헤더 아래 핑크 그라디언트 프로모 배너(정적 카피) 추가
- [x] "지금 인기 있는" 가로스크롤 랭킹 섹션: 상위 4개 상품에 순위 배지 표시 (실 리뷰 집계 없음 — 목업 정렬 기준으로 상위 4개 선정하는 로직을 구현하고 그 기준을 코드 주석으로 명시)
- [x] 카테고리 칩 필터: 탭하면 Phase 0에서 추가된 `category` 파라미터로 `GET /products`를 재호출해 그리드가 해당 카테고리로 필터링됨
- [x] "최근 본 상품" 가로스크롤: Phase 0의 localStorage 유틸로 읽어와 렌더링, 비어 있으면 섹션 자체 미표시 (이 phase에서는 방문 시 기록 로직은 아직 없음 — Phase 3에서 추가)
- [x] 빈 상태(상품 0개)/이미지 로드 실패(`surface-strong` 배경 + 상품명 플레이스홀더) 기존 패턴 유지 확인
- [x] 검증: `npm run build`/`npm run lint` 통과. 브라우저 확인 완료 — 프로모 배너→인기랭킹(순위배지)→카테고리 칩→상품 그리드 순서 확인

## Phase 2: 카테고리 + 검색 (`/category`, `/search`)

이 phase가 끝나면, `/category`에서 4개 카테고리를 선택해 `/search`로 필터링된 검색 결과를 볼 수 있고, `/search` 자체도 자동완성·최근검색어·인기검색어·정렬 기능을 포함해 완전히 동작한다.

- [x] `frontend/src/app/(shell)/category/page.tsx` 신규: 아우터/니트/이너웨어/악세서리 4개 리스트 항목(설명 텍스트 포함) 세로 나열, 탭 시 `/search?category={값}`으로 이동
- [x] `frontend/src/app/(shell)/search/page.tsx` 신규: 진입 시 검색바 자동 포커스
- [x] 입력 중 미실행 상태: 클라이언트에 로드된 상품명과 매칭되는 자동완성 드롭다운(최대 5개), 매칭 0개면 드롭다운 미표시
- [x] 입력값 없음 상태: 최근검색어 칩(Phase 0 유틸, 탭하면 해당 검색어로 재검색) + 인기검색어 순위 리스트(1~3위 핑크 숫자, 하드코딩 목업)
- [x] `frontend/src/components/commerce/FilterSheet.tsx` 신규 이식 (DesignSync `components/commerce/FilterSheet.jsx`/`.d.ts`/`.prompt.md` 참고, 기존 컴포넌트와 동일한 TypeScript 패턴)
- [x] 검색 실행 시: 결과 개수 + 정렬 버튼(`FilterSheet` 오픈 — 인기순/신상순/낮은 가격순/높은 가격순, 인기순=신상순) + 2열 상품 그리드, `?category=` 쿼리가 있으면 결과가 미리 필터링됨
- [x] 검색 결과 0개 시 빈 상태 문구
- [x] 검증: `npm run build`/`npm run lint` 통과. 브라우저 확인 완료 — `/category`→항목 탭→`/search?category=OUTER`로 이동해 3개 필터링 결과 확인, 검색 결과 0개 시 빈 상태 문구 확인, 미입력 상태 인기검색어(1~3위 핑크 숫자) 확인

## Phase 3: 상품상세 (`/products/[id]`)

이 phase가 끝나면, 상품상세 화면에서 사이즈 선택·위시 토글·장바구니 담기·리뷰 조회가 전부 실제로 동작하고, 방문한 상품이 홈의 "최근 본 상품"에 실제로 누적된다.

- [x] 기존 `SizeSelector` 컴포넌트를 화면에 연결 (S/M/L/XL 공통 고정 세트), 품절 상품이면 셀렉터 비활성
- [x] "사이즈 가이드"(체중/등길이 정적 표) 토글과 "배송·교환/반품 안내" 아코디언(정적 콘텐츠)을 동일 state로 관리해 동시에 하나만 열리도록 구현
- [x] `frontend/src/components/commerce/ReviewCard.tsx` 신규 이식(DesignSync `components/commerce/ReviewCard.jsx`/`.d.ts`/`.prompt.md` 참고), 전 상품 공통 목업 리뷰 3~4건 렌더링
- [x] 헤더 위시 하트: Phase 0 위시리스트 유틸로 실제 토글 (담기/제거 즉시 하트 상태 반영)
- [x] 하단 고정 CTA "장바구니 담기": 사이즈 선택 필수, Phase 0 장바구니 유틸에 아이템 추가, 성공 시 Toast 1.8초 노출 후 자동 소멸. 품절 상품이면 버튼 비활성
- [x] 방문 시 Phase 0 최근 본 상품 유틸에 현재 상품 id 기록(최대 8개, 중복 제거 후 최상단)
- [x] 존재하지 않는 id는 기존과 동일하게 `notFound()` 404 유지 확인 (회귀 없음)
- [x] 검증: `npm run build`/`npm run lint` 통과. 브라우저 확인 완료 — 사이즈 선택(M) 후 장바구니 담기 시 "장바구니에 담았어요" 토스트 노출 확인, 위시 하트 토글 시 "위시 완료" + 채워진 하트로 즉시 반영 확인

## Phase 4: 장바구니 (`/cart`)

이 phase가 끝나면, 장바구니 화면에서 Phase 3에서 담은 아이템의 수량 변경/삭제, 무료배송 진행바, 쿠폰 할인, 금액 요약이 전부 실제로 동작한다.

- [x] `frontend/src/components/feedback/ShippingProgress.tsx` 신규 이식(DesignSync `components/feedback/ShippingProgress.jsx`/`.d.ts`/`.prompt.md` 참고), `remaining = max(0, 50000 - (subtotal - discount))` 계산 로직 적용, 0이면 "무료배송 조건 달성" 메시지로 전환
- [x] 아이템 리스트: 썸네일/사이즈/수량 스테퍼/가격/삭제, Phase 0 장바구니 유틸 기반으로 변경 사항이 localStorage에 즉시 반영
- [x] 쿠폰 적용 토글(고정 3천원 할인)이 금액 요약에 반영
- [x] 금액 요약(상품금액/할인/배송비/총액) 계산 로직
- [x] 하단 고정 "결제" 버튼: 탭해도 실제 결제로 진행되지 않고 무동작 또는 "준비중" 토스트만 노출
- [x] 장바구니가 비어 있으면 빈 상태 문구 + 홈 이동 유도 CTA
- [x] 검증: `npm run build`/`npm run lint` 통과. 브라우저 확인 완료 — 수량 증가 시 18,000→36,000원 반영, 쿠폰 토글 시 -3,000원 할인 및 총액 재계산(36,000원) 정상, 무료배송 진행바 문구가 담긴 금액에 따라 실시간 갱신됨, "결제하기" 클릭 시 URL이 `/cart`에 그대로 유지(실제 결제로 진행되지 않음) 확인

## Phase 5: 위시리스트 (`/wishlist`)

이 phase가 끝나면, `/wishlist`에서 Phase 3에서 토글한 위시 상품이 2열 그리드로 표시되고 하트 토글로 추가/제거가 가능하다.

- [x] `frontend/src/app/(shell)/wishlist/page.tsx` 신규: 헤더 + Phase 0 위시리스트 유틸로 읽어온 상품을 기존 `ProductCard`로 2열 그리드 렌더링
- [x] 하트 버튼 재탭 시 위시에서 제거되고 그리드에서 즉시 사라짐 (구현 중 `ProductCard`의 하트 버튼 클릭이 카드 클릭(라우팅)으로 버블링되는 문제 발견 → `ProductCard.tsx`에 `e.stopPropagation()` 추가로 수정, 기존 사용처 `ProductGridItem`은 `favorited={false}` 고정이라 회귀 없음)
- [x] 위시가 비어 있으면 빈 상태 문구
- [x] 검증: `npm run build`/`npm run lint` 통과. 브라우저 확인 완료 — 상품상세에서 위시 추가한 상품이 `/wishlist`에 2열 그리드로 표시됨, BottomNav "위시" 탭 활성 스타일 확인, `/category`·`/wishlist` 라우트가 정상 페이지로 뜨는 것 확인(Phase 0 시점 404였던 부분 해소)

## Phase 6: 마이 (`/mypage`) + 전체 회귀 검증

이 phase가 끝나면, `/mypage`가 완성되고 스펙(`docs/specs/2026-08-26-app-redesign.md`)의 수용 기준 전체가 최종적으로 재검증된 상태가 된다.

- [x] `frontend/src/app/(shell)/mypage/page.tsx` 신규: 프로필(하드코딩 아바타 이미지 + 닉네임) — 아바타는 실제 이미지 에셋이 없어 `surface-strong` 배경의 원형 placeholder로 처리
- [x] 주문/위시/장바구니 카운트 요약 바: 위시·장바구니는 Phase 0 유틸 기준 실카운트, 주문은 0 고정
- [x] 메뉴 리스트(배송조회/쿠폰함/적립금/반려견 프로필 관리/고객센터): UI만 배치, 탭해도 에러 없이 무동작
- [x] 검증: `npm run build`/`npm run lint` 통과. 브라우저 확인 완료 — 위시(1)/장바구니(2, qty 합산) 카운트가 localStorage 실데이터를 정확히 반영, 메뉴 5개 항목 표시 확인(무동작이므로 클릭 액션은 코드 레벨 확인)
- [x] 스펙의 수용 기준(공통 3개 + 화면별 항목, 총 7개 섹션) 전체를 처음부터 다시 훑으며 브라우저에서 재검증: Playwright로 홈/카테고리/검색/상품상세/장바구니/위시리스트/마이 전 화면 스크린샷 검증 완료 — BottomNav 5탭 강조, `category` 필터, 아이콘 세트(lucide-react), 프로모배너/랭킹/칩/그리드 순서, 카테고리→검색 필터링 이동, 인기검색어, 사이즈선택→장바구니담기→토스트, 위시 토글, 장바구니 수량변경/쿠폰/무료배송진행바/결제무동작, 위시리스트 표시, 마이 카운트 전부 확인. (검증 중 로컬 개발 DB에 시드 데이터가 비어있던 문제 발견 → docker volume 재생성으로 해결, 코드 회귀 아님. 콘솔에는 시드 데이터의 의도된 broken-image URL로 인한 `ERR_NAME_NOT_RESOLVED`만 있었고 그 외 에러 없음)
- [x] 백엔드/프론트 최종 `./gradlew test`, `npm run build`, `npm run lint` 일괄 재실행해 전부 통과 확인 (`./gradlew test` 9/9, `npm run build`/`npm run lint` 통과)
