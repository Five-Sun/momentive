---
date: 2026-08-27
feature: product-catalog-home
spec: 2026-08-18-product-catalog.md
status: done
relates_to: 2026-08-23-product-catalog-home.md
---

# 상품 목록 조회 → 홈 화면 E2E 검증 (fix-1)

## 개요

원본 플랜(`plans/2026-08-23-product-catalog-home.md`, status: done)은 plan-runner/e2e-tester 체계(`specs/2026-08-26-plan-runner-cycle.md`, `plans/2026-08-26-plan-runner-cycle.md`)가 도입되기 전에 완료되어 `## Phase <N>: E2E 검증` phase가 없다. 오늘 `e2e-tester` 에이전트를 처음 실사용 검증하기 위해, 이미 완료된 이 기능에 대해 dev-browser 기반 E2E 검증 phase를 사후에 추가한다. 원본 plan의 Phase 1~3(백엔드 API, 홈 화면, 상품 상세)은 이미 done이므로 새 코드 phase는 필요 없고, 이 fix plan은 E2E 검증 phase 하나만 담는다.

1차 근거 spec은 `specs/2026-08-18-product-catalog.md`(목록→클릭→상세→404 핵심 흐름의 원본)이며, `specs/2026-08-23-home-screen.md`(그리드/무한스크롤 등 시각·인터랙션 디테일)도 함께 참고한다.

## Phase 1: E2E 검증

이 phase가 끝나면 홈 화면의 상품 목록 조회부터 상세 페이지 이동까지의 핵심 사용자 플로우가 실제 브라우저(dev-browser)로 검증된 상태가 된다.

- [x] 시나리오 1 — 홈(`/`) 접속 시 상품 그리드가 실제 API 데이터로 렌더링된다 (각 카드의 대표이미지/이름/가격/품절뱃지 노출) — product-catalog AC, home-screen AC 근거
- [x] 시나리오 2 — 품절 상품이 목록에서 숨겨지지 않고 "품절" 뱃지와 함께 노출된다 — product-catalog 사용자 시나리오 4 / AC 근거
- [x] 시나리오 3 — 상품 카드를 클릭하면 `/products/{id}` 상세 페이지로 이동하고, 이미지 갤러리(등록 순서대로)/이름/설명/가격(할인가 병기)/품절 여부가 표시되며 장바구니·구매 등 액션 버튼이 없다 — product-catalog 사용자 시나리오 5~6 / AC, home-screen 마지막 AC 근거
- [x] 시나리오 4 — 그리드 하단까지 스크롤하면 다음 페이지(20개)가 자동으로 이어붙는다 (무한스크롤) — home-screen 사용자 시나리오 5 / AC 근거
- [x] 시나리오 5 — 존재하지 않는 상품 id로 상세 페이지에 접근하면 404로 처리된다 — product-catalog 사용자 시나리오 7 / AC 근거
- [x] 시나리오 6 — 상품이 0개인 상태에서는 그리드 대신 빈 상태 문구가 노출된다 — product-catalog 사용자 시나리오 7 / home-screen AC 근거. 이 시나리오는 시나리오 1~4(상품 존재 전제)와 DB 상태가 충돌하므로, `.claude/rules/e2e-format.md`의 "데이터 사전조건 충돌" 규칙에 따라 실행 시점에 시드 데이터를 비울 방법이 없으면 실패로 집계하지 않고 "사전조건 미충족으로 스킵"이라고 보고한다
