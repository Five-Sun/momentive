---
date: 2026-09-02
feature: responsive-design-handoff
spec: 2026-09-02-responsive-design-handoff.md
status: done
---

# 디자인 핸드오프 2차 이관 플랜

## 개요

`docs/specs/2026-09-02-responsive-design-handoff.md` 기반. 폰트(어비 세현체) 교체 / 모션 토큰 도입 / `(shell)` 하위 18개 라우트 데스크톱 반응형 3가지를 이관한다.

Phase는 **영향 범위가 넓은 것부터 좁은 것 순**으로 배치했다. 토큰(Phase 1) → 셸·네비(Phase 2) → 공통 컴포넌트(Phase 3) → 개별 화면(Phase 4~5) 순서라, 앞 phase가 끝나면 뒤 phase의 작업량이 줄어든다. 반대로 화면부터 손대면 토큰 변경 때 전부 다시 만져야 한다.

Phase 4와 5를 나눈 이유는 **근거의 성격이 다르기 때문**이다. Phase 4는 `design_handoff_momentive_app/ui_kits/web-app/index.html`이라는 확정 레퍼런스를 그대로 옮기는 작업이고, Phase 5는 레퍼런스가 없어 spec의 기본 규칙(폼 480px / 문서 720px / 목록 다단)으로 직접 설계하는 작업이다. 리뷰 기준이 달라 섞으면 판정이 어려워진다.

Phase 6(`/style-guide`)과 Phase 7(E2E)은 spec 합의 시점엔 하나였으나, reviewer가 "마지막 코드 phase 통과 시 `## Phase N+1: E2E 검증` 섹션을 감지해 `e2e-tester`로 체이닝"하는 절차를 쓰므로 분리했다.

핸드오프 번들은 리포지토리 루트 `design_handoff_momentive_app/`에 커밋되어 있다 — 구현 시 원본을 직접 대조한다.

사전 조사로 이미 확인된 사항(재조사 불필요):
- 색상·radius·shadow·spacing·타이포 스케일은 1차 이관에서 값까지 동일하게 반영 완료 — **이번에 건드리지 않는다**
- 어비 세현체 라이선스: 웹사이트·임베딩·상업사용 허용, `fsType=8`. self-host 가능
- 어비 세현체는 **Regular 1종만 존재**(Bold 미배포). weight 500~700은 synthetic bold로 렌더됨
- 글리프 커버리지 실측 결과 서비스 텍스트 397자 **전량 커버, 미커버 0건**

## Phase 1: 토큰 기반 (폰트 · 모션 · 디자인 문서)

폰트와 모션 토큰이 전역에 깔리고, 이후 모든 phase가 참조할 `docs/design.md`가 생긴다. 이 phase가 끝나면 화면 구조는 그대로인 채 **글꼴만 어비 세현체로 바뀌어 있어야** 한다.

- [x] 어비 세현체 woff2를 `frontend/public/fonts/UhBeeSehyun.woff2`로 추가 (출처 `github.com/fonts-archive/UhBeeSehyun`, 358KB). 외부 CDN 참조는 사용하지 않는다
- [x] `frontend/src/app/layout.tsx`에서 `Jua` import·`--font-jua` 변수를 제거하고, `next/font/local`로 어비 세현체를 로드해 CSS 변수(`--font-uhbee`)로 노출. `Noto_Sans_KR`은 fallback 용도로 유지
- [x] `frontend/src/app/globals.css`의 `@theme inline`에서 `--font-display`/`--font-body`가 모두 `var(--font-uhbee), var(--font-noto-sans-kr), sans-serif`를 가리키도록 변경. 타이포 스케일 10종의 크기·줄높이·weight 값은 변경하지 않는다
- [x] `globals.css`에 모션 토큰 추가 — `--ease-spring: cubic-bezier(.34,1.56,.64,1)`, `@keyframes paw-pop`(scale 1→1.28→.94→1), `@keyframes bump-up`(translateY 0→-3px & scale 1.15→0). Tailwind v4 `--animate-*` 테마 키로 노출해 `animate-paw-pop`/`animate-bump-up` 유틸이 생성되게 한다
- [x] `globals.css`에 `@media (prefers-reduced-motion: reduce)` 블록을 추가해 위 애니메이션을 비활성화
- [x] `docs/design.md` 신규 작성 — 토큰 원천(색상/타이포/radius/shadow/spacing/모션), 브레이크포인트 규칙 표(<1024 / ≥1024 / ≥1280), `frontend/src/components/` 카테고리별 컴포넌트 목록과 사용 지침, 반복 패턴(`AppHeader`·`BottomActionBar`·`SelectableCircle`·`SummaryRows`·`ProductGrid`) 정리. `frontend/CLAUDE.md`가 참조하는 대상이므로 reviewer가 검증 기준으로 쓸 수 있는 수준으로 작성
- [x] 검증 — `cd frontend && npm run build && npm run lint` 통과
- [x] 검증(수동, 브라우저) — **굵기 육안 확인 게이트**. 홈·상품상세·장바구니에서 `text-tag`(11px, weight 600)와 `text-caption`(12px, weight 500)의 synthetic bold 판독성을 확인한다. 획이 뭉개져 판독이 어려우면 **해당 토큰만 weight 400으로 평탄화**하고 그 결정을 `docs/design.md`에 근거와 함께 기록한다. 다른 서체로 대체하지 않는다 — **결과: 뭉개짐 없음, 평탄화 불필요.** 3배 확대 클로즈업으로 확인, `docs/design.md` "굵기 육안 확인 게이트" 섹션에 근거 기록

## Phase 2: 앱 셸 · 네비게이션 반응형

1024px 경계에서 상단 네비 ↔ 하단 탭바가 전환되고, 폰 목업 프레임이 사라진다. 전 화면에 영향이 가는 구간이라 단독 phase로 격리한다.

- [x] `frontend/src/app/(shell)/layout.tsx`에서 `bg-surface-strong` 배경 + `max-w-[480px]` 프레임 + `shadow-float`를 제거하고 반응형 컨테이너로 교체 — <1024px는 `max-w-[480px] mx-auto`, ≥1024px는 `max-w-[1400px] mx-auto` + 좌우 40px 패딩. 배경은 `canvas`
- [x] `frontend/src/components/navigation/TopNav.tsx` 신설 — ≥1024px에서만 렌더. 높이 80px, 하단 `border-hairline` 1px. 좌측 로고(38px 원형) + 링크 4개(홈 `/`, 카테고리 `/category`, 위시 `/wishlist`, 마이 `/mypage`), 우측 검색 입력창(h-42px, w-300px, `surface-soft`, pill, Enter 시 `/search?q=` 이동) + 장바구니 아이콘(`/cart`, 수량 배지 `brand-pink-active`). 활성 링크는 `brand-pink-active` + bold. 아이콘은 `lucide-react` 사용
- [x] `(shell)/layout.tsx`에 `TopNav`를 배치하고, `GlobalBottomNav`는 <1024px에서만 렌더되도록 분기
- [x] `frontend/src/components/navigation/GlobalBottomNav.tsx` — 5탭 구성과 `href`는 현행 유지. `HIDDEN_PREFIXES`(`/checkout`, `/mypage/orders/`) 우회가 <1024px에만 적용되도록 정리
- [x] 검증 — `npm run build && npm run lint` 통과
- [x] 검증(수동, 브라우저) — 1024px 경계 위아래로 폭을 조절해 상단 네비 ↔ 하단 탭바 전환, 활성 상태 표시, 장바구니 배지 수량, 검색창 Enter 라우팅을 확인. 전 화면 영향 구간이므로 홈·상품상세·장바구니·마이를 얕게 훑어 레이아웃 붕괴가 없는지 함께 확인 — 1024px/1280px 데스크톱 네비 전환, 검색창 Enter→`/search?q=` 라우팅, 장바구니 배지 수량 반영 전부 정상 확인

## Phase 3: 공통 컴포넌트 반응형 · 모션 적용

Phase 1에서 만든 모션 토큰이 실제 인터랙션에 붙고, 공통 컴포넌트가 데스크톱 폭에 대응한다. 이 phase가 끝나면 개별 화면을 만지지 않아도 컴포넌트 단위 동작이 완성된다.

- [x] `frontend/src/components/commerce/ProductCard.tsx` — 하트 토글에 `animate-paw-pop` 적용 (기존 `e.stopPropagation()` 동작 유지)
- [x] `frontend/src/components/core/Chip.tsx`, `frontend/src/components/core/Badge.tsx` — 선택/노출 시 `animate-paw-pop` 적용
- [x] `frontend/src/components/navigation/BottomNav.tsx` — 활성 탭 아이콘에 `animate-bump-up` 적용
- [x] `frontend/src/components/feedback/Toast.tsx` — 등장 트랜지션에 `--ease-spring` 적용 (translateY + scale), 1.8초 자동 소멸 동작 유지
- [x] 상품 그리드 공통화 — 홈·검색·위시리스트가 반복하는 2열 그리드를 `<1024px 2열 / ≥1024px 3열 / ≥1280px 4열` 규칙으로 통일. `docs/design.md`의 `ProductGrid` 항목과 일치시킨다
- [x] `frontend/src/components/feedback/ShippingProgress.tsx` 등 폭 의존 컴포넌트가 데스크톱 폭에서 늘어져 깨지지 않는지 점검·수정
- [x] 검증 — `npm run build && npm run lint` 통과
- [x] 검증(수동, 브라우저) — 하트·칩·탭·토스트 4개 모션이 실제로 재생되는지, OS "동작 줄이기" 활성화 시 모두 비활성화되는지 확인 — 위시 하트(`paw-pop`)·사이즈 칩(`paw-pop`)·BottomNav 탭(`bump-up`) 실제 발동 확인, Toast는 인라인 `--ease-spring` 적용 확인. `prefers-reduced-motion: reduce`에서 `animationDuration: 0s`, `transitionDuration: 1e-06s`로 전부 무력화됨을 computed style로 확인

## Phase 4: 핸드오프 레퍼런스가 있는 7개 화면 데스크톱

`ui_kits/web-app/index.html`을 근거로 데스크톱 레이아웃을 적용한다. **레퍼런스의 목업 비즈니스 로직(무료배송 5만원, 쿠폰 3,000원 토글, 하드코딩 평점)은 이관하지 않는다.**

- [x] `/` 홈 (`(shell)/page.tsx`) — 프로모 배너 전체폭(`.hero` 데스크톱 규칙), 카테고리 레일 가로 확장(`.cat-rail`), 상품 그리드 3~4열, 모바일 전용 검색 행은 데스크톱에서 숨김(`.mobile-search-row`)
- [x] `/category` — 카테고리 목록 2열 그리드 (`.cat-grid`)
- [x] `/search` — 결과 그리드 3~4열. 자동완성·최근검색어·인기검색어·정렬 바텀시트 기능은 **삭제하지 않고 유지**
- [x] `/products/[id]` (`components/commerce/ProductDetailView.tsx`) — 데스크톱 좌 이미지 sticky / 우 제목·평점·가격·사이즈·CTA 2단. 리뷰 목록과 사이즈가이드·배송/교환 아코디언은 하단 전체폭. 하단 고정 CTA 바는 <1024px에서만 `fixed`
- [x] `/cart` — 좌 아이템 목록 / 우 340px 요약 컬럼 2단(`.cart-layout`), 결제 CTA는 요약 컬럼 내 sticky. 무료배송 임계값 70,000원과 제주 할증 로직은 현행 유지
- [x] `/wishlist` — 그리드 3~4열
- [x] `/mypage` — 메뉴 3열 그리드 (`.my-menu`), 주문/위시/장바구니 카운트 요약 바 유지
- [x] 검증 — `npm run build && npm run lint` 통과
- [x] 검증(수동, 브라우저) — 7개 화면을 390px / 1024px / 1440px 3개 폭에서 확인. CTA가 네비게이션에 가려지지 않는지, 상품 카드가 데스크톱에서 과도하게 늘어나지 않는지 확인 — 홈/카테고리/검색/상품상세/장바구니/위시리스트/마이 전부 3폭 스크린샷으로 확인, 390px 하단 fixed CTA와 BottomNav 겹침 없음, 1024px 3열·1280px 4열 그리드 전환, 상품상세 좌우 2단 sticky, 장바구니 우측 340px 요약 컬럼 전부 정상

## Phase 5: 레퍼런스가 없는 11개 화면 데스크톱

핸드오프에 데스크톱 디자인이 없는 화면들. spec의 기본 규칙(폼 480px 중앙 / 문서 720px 가독폭 / 목록 다단·최대폭 제한)으로 직접 설계한다.

- [x] `/login`, `/signup` — 콘텐츠 480px 폭 중앙 배치. 폼 필드를 데스크톱 폭으로 늘리지 않는다
- [x] `/checkout` — 좌 배송지·쿠폰 선택 / 우 340px 요약 컬럼 2단, 결제 CTA는 요약 컬럼 내 sticky. 쿠폰 거부 3종 분기와 목록 재조회 복구 동작은 현행 유지
- [x] `/checkout/payment`, `/checkout/success`, `/checkout/fail` — 720px 폭 중앙 배치, CTA 인라인. Toss 결제위젯 컨테이너가 데스크톱 폭에서 깨지지 않는지 확인
- [x] `/mypage/orders`, `/mypage/orders/[orderId]` — 목록·상세 최대 폭 제한, 하단 고정 CTA는 데스크톱에서 인라인 전환
- [x] `/mypage/coupons`, `/mypage/pets` — 카드 목록 2열 그리드
- [x] `/mypage/support` — FAQ 720px 가독 폭 중앙 배치
- [x] 검증 — `npm run build && npm run lint` 통과
- [x] 검증(수동, 브라우저) — 11개 화면을 390px / 1024px / 1440px 3개 폭에서 확인. 긴 한국어 텍스트 줄바꿈, 폼 에러 메시지 인라인 표시 위치를 함께 확인 — 로그인/회원가입 480px 중앙 배치, 체크아웃 좌우 2단(우측 340px sticky 요약, 배송비 반영), 주문내역·쿠폰함·반려견·고객센터(720px) 로그인 상태로 실제 확인. 신규 계정 빈 상태(주문내역/쿠폰/반려견 없음) 렌더링도 정상

## Phase 6: `/style-guide` 갱신

디자인 문서(`docs/design.md`)의 살아있는 짝인 `/style-guide`가 현재 코드와 일치하게 된다.

- [x] `frontend/src/app/style-guide/page.tsx` — BottomNav 예시를 4탭에서 실제와 같은 5탭(홈/카테고리/검색/위시/마이)으로 정정
- [x] 어비 세현체 타이포 스케일 10종 샘플 추가 (각 토큰의 실제 렌더 모습, Phase 1에서 평탄화한 토큰이 있으면 그 상태로 표시)
- [x] 모션 3종(`paw-pop`/`bump-up`/`--ease-spring`) 인터랙티브 샘플 추가 — 눌러서 재생을 확인할 수 있는 형태
- [x] 브레이크포인트 규칙 샘플 추가 (<1024 / ≥1024 / ≥1280 그리드 열 수)
- [x] `docs/design.md`와 `/style-guide` 내용이 어긋나는 항목이 없는지 대조·정리
- [x] 검증 — `npm run build && npm run lint` 통과

## Phase 7: E2E 검증

`e2e-tester`가 핵심 플로우 회귀를 확인한다. 로컬 서버는 사용자가 `./dev.sh`로 미리 기동해 둔 상태여야 한다.

- [x] `docs/e2e/2026-09-02-responsive-design-handoff.md` 생성 — `e2e-format.md` 규격에 따라 단일 탭 순차 실행 스크립트로 작성
- [x] 핵심 3플로우 회귀 — ① 장바구니 → 주문 → 결제(배송비 70,000원 임계값·제주 할증 표시 포함) ② 쿠폰 등록·선택·할인 반영 ③ 리뷰 작성·수정. 모바일 폭(390px)과 데스크톱 폭(1440px) 양쪽에서 실행 (리뷰 작성/수정 자체는 `PAID` 주문 생성이 Toss 샌드박스 실카드 결제를 요구해 사전조건 미충족으로 스킵 — 실패 아님, 상세는 `docs/e2e/2026-09-02-responsive-design-handoff.md` 참고)
- [x] 반응형 전환 시나리오 — 1024px 경계에서 상단 네비 ↔ 하단 탭바 전환, 데스크톱에서 `fixed` CTA 잔존 여부, CTA와 네비게이션 겹침 없음 확인
- [x] 검증 실패 시 `docs/backlog/` 항목을 `backlog-format.md` 규격으로 기록하고 plan은 수정하지 않는다 (이번 검증은 전부 PASS해 해당 없음)
