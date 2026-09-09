---
date: 2026-09-09
feature: delivery-tracking
spec: 2026-09-09-delivery-tracking.md
status: done
---

# 배송조회 플랜

## 개요

`docs/specs/2026-09-09-delivery-tracking.md` 기반. 마이페이지 "배송조회" 메뉴를 기존 `/mypage/orders` 화면에 연결하고, 그 화면이 배송상태를 실제로 보여줄 수 있도록 백엔드 요약 API에 필드를 추가한다. Frontend가 Backend의 API 변경에 의존하므로 Backend를 먼저 완료한다.

## Phase 1: 백엔드 — 주문 요약 API에 배송상태 추가

`GET /orders`가 반환하는 `OrderSummaryResponse`에 `shippingStatus`를 추가해, 프론트가 목록에서 배송상태를 표시할 수 있게 한다.

- [x] `OrderSummaryResponse`(또는 대응 DTO)에 `shippingStatus` 필드 추가, `Order` 엔티티 값을 그대로 매핑(취소 등으로 상태값이 남아 있어도 그대로 내려주고, 노출 여부 판단은 프론트 책임)
- [x] 주문 목록 조회 서비스/쿼리에서 해당 필드가 채워지는지 확인
- [x] 백엔드 테스트: 배송상태가 있는 주문과 없는 주문(PENDING 등) 각각 목록 응답에 올바른 `shippingStatus` 값이 담기는지 검증
- [x] `./gradlew build`, `./gradlew test` 통과

## Phase 2: 프론트엔드 — 목록 화면 연동 + 메뉴 연결

- [x] `frontend/src/app/(shell)/mypage/orders/page.tsx`의 `OrderSummaryResponse` 타입에 `shippingStatus` 반영
- [x] 주문 카드에 배송상태 뱃지 추가 — 표시 조건 `order.shippingStatus && order.status === "PAID"`, 라벨/톤은 `src/lib/shippingStatus.ts`의 `SHIPPING_STATUS_LABEL`/`SHIPPING_STATUS_TONE` 재사용
- [x] `frontend/src/app/(shell)/mypage/page.tsx`의 "배송조회" 메뉴 `onClick`을 `() => router.push("/mypage/orders")`로 변경
- [x] `npm run build`, `npm run lint` 통과

## Phase 3: 검증

- [x] `./dev.sh`로 전체 스택 기동, 배송중/배송준비중/배송완료 각 상태의 주문을 가진 계정으로 `/mypage/orders` 진입해 뱃지 노출 확인(수동 브라우저 검증)
- [x] 결제대기/결제실패/취소 주문에는 배송상태 뱃지가 뜨지 않는지 확인
- [x] 주문 카드 클릭 → 상세 화면 배송정보가 목록과 일치하는지 확인
- [x] 검증 후 테스트 계정/데이터 정리
