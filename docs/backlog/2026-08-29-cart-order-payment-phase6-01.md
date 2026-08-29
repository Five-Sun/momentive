---
date: 2026-08-29
feature: cart-order-payment
phase: 6
category: frontend
---

# cart-order-payment / Phase 6 — 2026-08-29

## 실패

`docs/e2e/2026-08-29-cart-order-payment.md` 시나리오 4(주문 생성 → Toss 결제위젯 렌더링 확인)를 dev-browser로 실행한 결과, `/checkout` 페이지에서 "결제하기" 버튼을 클릭해도 `POST /orders` 요청 자체가 발생하지 않고 화면 전환도 일어나지 않았다.

Playwright의 일반 `click()`은 액션 대기(actionability wait) 상태로 멈춰 스크립트가 타임아웃(30s)됐고, `force: true`로 강제 클릭해도 백엔드 로그(`.dev-logs/backend.log`)에 `POST /orders` 호출 흔적이 전혀 남지 않았다.

`page.evaluate`로 "결제하기" 버튼의 bounding box 중심 좌표에서 `document.elementFromPoint(cx, cy)`를 호출해 실제로 클릭을 받는 최상위 DOM 요소를 확인한 결과, 버튼이 아니라 전역 하단 네비게이션(`GlobalBottomNav`)의 `svg` 아이콘이었다(`isSameAsButton: false`). 즉 "결제하기" 버튼은 시각적으로도 완전히 가려져 있고(스크린샷: `~/.dev-browser/tmp/checkout-bottombar-check`, `~/.dev-browser/tmp/checkout-bottombar-after-scroll`) 클릭 자체를 받을 수 없는 상태다.

같은 `fixed bottom-0` 패턴을 쓰는 `/checkout/payment`(Toss 결제 버튼)와 `/mypage/orders/[orderId]`(주문 취소 버튼)도 동일한 원인으로 동일하게 가려질 것으로 코드 구조상 추정된다(`grep`으로 세 파일 모두 `fixed bottom-0 left-1/2 w-full max-w-[480px] -translate-x-1/2` 패턴을 그대로 사용하는 것을 확인).

이로 인해 시나리오 4는 fail 처리했고, 이 시나리오 4의 성공을 전제로 하는 시나리오 5(confirm 실패 흐름), 시나리오 6(마이페이지 목록 확인)은 시나리오 4 실패로 인해 실행하지 못했다(연쇄적으로 스킵). Phase 6은 미완료로 남긴다.

## 원인

`frontend/src/app/(shell)/layout.tsx`가 모든 `(shell)` 하위 페이지 공통으로 `GlobalBottomNav`를 `<div className="sticky bottom-0">`로 `children` 바로 다음 위치에 항상 렌더링한다(line 33-36). 반면 `checkout/page.tsx`, `checkout/payment/page.tsx`, `mypage/orders/[orderId]/page.tsx`는 각자 자신의 CTA 버튼을 `fixed bottom-0 left-1/2 ... -translate-x-1/2` 로 뷰포트 최하단에 직접 고정 배치한다.

두 요소 모두 명시적 `z-index`가 없고, 브라우저 기본 stacking 규칙상 DOM 트리에서 나중에 등장하는 요소(`GlobalBottomNav`)가 위에 그려진다. `GlobalBottomNav`는 `sticky` 컨테이너 안에 있어 `layout.tsx`가 렌더링하는 `children`(각 페이지의 `fixed` CTA 바 포함) 바로 뒤에 위치하므로, `fixed` CTA 바 위를 완전히 덮어버린다.

plan Phase 4/5의 "검증(수동, 브라우저)" step이 여전히 미체크(`- [ ]`)로 남아 있는 것으로 보아, 실제 브라우저에서 CTA 버튼을 클릭해보는 수동 검증이 이번 phase까지 한 번도 수행되지 않았다. reviewer의 정적 리뷰(코드 diff)만으로는 두 개의 서로 다른 파일(`layout.tsx`와 각 페이지)에 흩어진 `fixed`/`sticky` 배치가 실제 화면에서 겹치는지 판단하기 어려워 이 결함이 지금까지 감지되지 않았다.

## 조치

다음 중 하나로 겹침을 해소해야 한다.

1. (권장) `checkout`, `checkout/payment`, `mypage/orders/[orderId]`처럼 자체 CTA 바를 갖는 페이지에서는 `GlobalBottomNav`를 렌더링하지 않는다. `(shell)/layout.tsx`가 모든 하위 페이지에 무조건 `GlobalBottomNav`를 붙이는 대신, route group을 분리(예: 이 3개 페이지를 `GlobalBottomNav`가 없는 별도 레이아웃 그룹으로 이동)하거나, `usePathname()` 기준으로 특정 경로에서는 `GlobalBottomNav`를 숨기는 조건을 `GlobalBottomNav` 또는 `ShellLayout`에 추가한다.
2. 위 방식이 어렵다면 각 페이지의 CTA 바를 `GlobalBottomNav`보다 위에 오도록 `GlobalBottomNav` 자체를 CTA 바 노출 시 대체(replace)하는 구조로 바꾸고, 두 요소가 동시에 화면에 존재하지 않게 한다.
3. z-index만 조정해 CTA 바를 위로 올리는 방식은 지양한다 — 그러면 이번엔 반대로 `GlobalBottomNav`가 완전히 가려져 탭 이동 자체가 막히므로 근본 해결이 아니다(두 네비게이션 요소가 동시에 같은 화면에 존재하면 안 되는 문제).

수정 후 `/checkout`, `/checkout/payment`, `/mypage/orders/[orderId]` 3개 페이지 모두에서 CTA 버튼이 시각적으로 완전히 노출되고 정상 클릭되는지 dev-browser로 재검증해야 한다(이번에 사용한 `document.elementFromPoint` 기법을 재사용 가능).

### 실제 조치 (2026-08-29)

옵션 1을 적용하되, route group 분리 대신 `GlobalBottomNav`(`frontend/src/components/navigation/GlobalBottomNav.tsx`) 자체에 경로 기반 숨김 조건을 추가하는 방식으로 구현했다. 이 컴포넌트가 이미 `"use client"` + `usePathname()`을 쓰고 있어서 `(shell)/layout.tsx`(서버 컴포넌트, 쿠키 기반 `/auth/me` SSR 페치를 담당)를 건드리지 않고 최소 변경으로 해소할 수 있었다.

- `HIDDEN_PREFIXES = ["/checkout", "/mypage/orders/"]`를 정의하고, `pathname`이 이 중 하나로 시작하면 `GlobalBottomNav`가 `null`을 반환하도록 수정.
  - `/checkout`, `/checkout/payment`, `/checkout/success`, `/checkout/fail`을 모두 포괄 (뒤 두 화면은 자체 fixed CTA는 없지만 같은 그룹으로 묶어도 부작용 없음을 확인).
  - `/mypage/orders/[orderId]`는 `/mypage/orders/`(끝에 슬래시)로 매칭해 목록 페이지 `/mypage/orders`(CTA 바 없음, 네비게이션 유지 필요)는 제외되도록 함.
- `(shell)/layout.tsx`는 수정하지 않음 — `GlobalBottomNav`가 `null`을 반환해도 감싸는 `<div className="sticky bottom-0">`는 빈 채로 남지만 시각적으로 영향 없음.
- `npm run build`, `npm run lint` 모두 통과 확인 (`frontend/` 기준).
- dev-browser를 이용한 실제 클릭 재검증(시나리오 4~6)은 이 작업에서 수행하지 않았음 — e2e-tester 재실행 필요.

## 재발 방지

fixed/sticky로 화면 하단(또는 상단)에 고정 배치되는 UI를 페이지에 새로 추가할 때는, 그 페이지가 속한 공통 레이아웃(`layout.tsx`)이 이미 같은 위치에 고정 UI(전역 네비게이션 등)를 렌더링하고 있는지 먼저 확인하고, 정적 리뷰뿐 아니라 실제 브라우저에서 `document.elementFromPoint(cx, cy)`로 대상 버튼의 중심 좌표가 실제로 그 버튼 자신을 가리키는지 클릭 전에 검증하는 것을 이런 화면의 "검증(수동, 브라우저)" step 체크리스트에 포함한다.
