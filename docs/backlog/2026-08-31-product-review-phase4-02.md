---
date: 2026-08-31
feature: product-review
phase: 4
category: test
---

# product-review / Phase 4 — 2026-08-31 (재검증 2회차)

## 실패

`docs/e2e/2026-08-30-product-review.md` 시나리오 5("목록 화면(홈)에서 실제 평점 반영")에서 판정 실패. 홈(`/`)에서 `page.getByText("강아지 무릎담요").first()`로 찾은 카드 컨테이너의 텍스트에 "3.0"이 포함되어 있어야 하는데, 실제로는 `"강아지 무릎담요\n1\n강아지 무릎담요\n18,000원"`가 반환되어 평점 텍스트가 없었다(에러 메시지에 카드 내용 포함). 이 실패로 시나리오 6("리뷰 삭제 후 재작성 가능")은 실행되지 않았다(시나리오 5 실패로 미실행). 시나리오 1~4는 모두 pass.

스크린샷: `~/.dev-browser/tmp/product-review-scenario-5-wrong-rating` — 실제로 열어보면 화면 상단 "지금 인기 있는" 랭킹 캐러셀의 1번 카드("강아지 무릎담요", 랭크 뱃지만 있고 평점 없음)와, 그 아래 메인 상품 그리드의 "강아지 무릎담요" 카드(★ 3.0 (1) 정상 표시)가 함께 보인다.

## 원인

이번 실패는 제품 코드 결함이 아니라 e2e 스크립트의 셀렉터 모호성 때문이다.

- 홈 화면에는 같은 상품명 "강아지 무릎담요"가 두 군데 렌더링된다: (1) `frontend/src/app/(shell)/page.tsx`의 "지금 인기 있는" 랭킹 캐러셀 — `ProductMiniCard`(`frontend/src/components/commerce/ProductMiniCard.tsx`) 사용, 이 컴포넌트는 랭크 뱃지·이름·가격만 렌더링하고 평점을 표시하지 않는다(원래 설계가 그러함 — Phase 3 plan의 대상은 `ProductGridItem`이지 `ProductMiniCard`가 아님). (2) 그 아래 실제 상품 그리드 — `ProductGridItem`(`frontend/src/components/commerce/ProductGridItem.tsx` 56행) 사용, `product.averageRating`을 `Rating` 컴포넌트로 정상 표시.
- DOM 순서상 랭킹 캐러셀이 그리드보다 먼저 렌더링되므로, `page.getByText("강아지 무릎담요").first()`는 랭킹 캐러셀 쪽 텍스트 노드를 먼저 매칭한다. 그 조상 `<a>`(`ancestor::a[1]`)는 `ProductMiniCard`의 링크이므로 평점 텍스트가 없어 assertion이 실패했다.
- 즉 스크린샷으로 직접 확인한 결과 AC("홈/카테고리/검색/상품상세/위시리스트 5개 화면의 상품 카드가 하드코딩된 4.5 대신 실제 averageRating을 표시")의 실제 대상인 메인 그리드 카드는 "3.0 (1)"을 정상적으로 표시하고 있어 기능 자체는 정상 동작한다. `ProductGridItem.tsx` 56행의 `rating={product.averageRating != null ? <Rating value={product.averageRating} count={product.reviewCount} /> : undefined}` 코드도 하드코딩 없이 실제 값을 쓰고 있음을 코드 리뷰로 재확인했다.

## 조치

`docs/e2e/2026-08-30-product-review.md` 시나리오 5 스크립트의 셀렉터를 랭킹 캐러셀을 배제하고 메인 상품 그리드 컨테이너로 명시적으로 스코프하도록 수정해야 한다(예: 그리드 컨테이너의 클래스 `grid grid-cols-2`를 기준으로 `page.locator(".grid.grid-cols-2").getByText("강아지 무릎담요")`로 범위를 좁히거나, `ProductMiniCard`가 렌더링되는 "지금 인기 있는" 섹션을 CSS 선택자로 제외). 이번 실행에서는 소스 코드는 물론 케이스 문서도 직접 수정하지 않았으므로, e2e-tester가 이 backlog를 참고해 셀렉터를 고친 뒤 시나리오 5·6을 재실행해 실제로 pass하는지 확인해야 한다.

## 재발 방지

같은 상품/텍스트가 서로 다른 위젯(추천·랭킹 캐러셀 vs 메인 목록 그리드)에 중복 렌더링될 수 있는 화면에서는 `getByText(...).first()`처럼 DOM 순서에 의존하는 셀렉터를 쓰지 말고, 검증 대상 컴포넌트가 속한 컨테이너(클래스/구조)를 먼저 스코프한 뒤 그 안에서 텍스트를 찾도록 e2e 스크립트를 작성한다.
