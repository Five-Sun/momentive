---
date: 2026-08-31
feature: shipping-fee-policy
spec: 2026-08-31-shipping-fee-policy.md
status: done
---

# 배송비 정책 반영 플랜

## 개요

`docs/specs/2026-08-31-shipping-fee-policy.md` 기반. 실제 사업자 배송비 정책(기본 3,400원 / 상품금액 70,000원 이상 무료배송 / 제주 우편번호(63000~63644)면 상품금액과 무관하게 4,000원 항상 추가)을 주문 생성 로직에 반영한다.

계산의 단일 소스는 백엔드다 — `OrderResponse.totalAmount`가 이미 결제위젯 렌더링과 confirm 검증의 authoritative 값으로 쓰이고 있으므로(`OrderPaymentTransactionSupport.assertPendingAndOwned`), 백엔드에서 계산 로직과 DTO를 먼저 확정한 뒤(Phase 1) 이를 소비하는 화면들을 순서대로 반영한다: 체크아웃/주문상세 breakdown(Phase 2) → 장바구니 무료배송 진행 안내(Phase 3, 서버 응답과 무관한 독립적인 클라이언트 표시라 별도 phase로 분리) → E2E 검증(Phase 4). 이 순서는 이전 `docs/plans/2026-08-29-cart-order-payment.md`, `docs/plans/2026-08-31-mypage-menu-cleanup.md`의 "백엔드 → 프론트 → E2E" 관례를 따른다.

## Phase 1: 백엔드 — 배송비 계산 로직 + Order/OrderResponse 확장 + 마이그레이션

이 phase가 끝나면 `POST /orders`, `GET /orders/{orderId}`가 정책대로 계산된 `shippingFee`/`itemsSubtotal`을 포함한 `totalAmount`를 반환한다.

- [x] `backend/src/main/resources/db/migration/V8__add_shipping_fee_to_orders.sql` — `orders.shipping_fee` 컬럼 추가(기존 행은 `0`으로 backfill 후 `NOT NULL` 제약)
- [x] 정책 상수 + 계산 로직을 담은 `ShippingFeePolicy`(패키지 위치는 `order` 도메인 하위, 예: `backend/src/main/java/com/momentive/backend/order/domain/ShippingFeePolicy.java`) — 기본 배송비 3,400 / 무료배송 임계값 70,000(상품금액 기준) / 제주 할증 4,000 / 제주 판정 우편번호 범위 63000~63644. `calculate(int itemsSubtotal, String zipcode)` 형태로 두고, `zipcode`를 정수로 파싱해 범위 판정하되 파싱 실패 시 안전하게 "제주 아님"으로 처리
- [x] `backend/src/main/java/com/momentive/backend/order/domain/Order.java`에 `shippingFee`(Integer, not null) 필드 추가. 기존 `confirmTotalAmount(int)` 메서드를 `confirmAmounts(int itemsSubtotal, int shippingFee)`로 교체해 `totalAmount = itemsSubtotal + shippingFee`를 내부에서 계산하도록 캡슐화(setter 없이 도메인 메서드로 상태 변경하는 기존 컨벤션 유지)
- [x] `backend/src/main/java/com/momentive/backend/order/service/OrderService.java`의 `createOrder`에서 `resolveAddress`로 확정된 배송지의 `zipcode`와 items 루프로 누적된 `itemsSubtotal`을 `ShippingFeePolicy`에 넘겨 `shippingFee`를 계산하고, `order.confirmAmounts(itemsSubtotal, shippingFee)` 호출로 교체
- [x] `backend/src/main/java/com/momentive/backend/order/dto/OrderResponse.java`에 `itemsSubtotal`(Integer), `shippingFee`(Integer) 필드 추가 및 `@Schema` 작성(Swagger 컨벤션), `OrderResponse.from(Order)`에 반영. `OrderSummaryResponse`는 변경하지 않음(목록은 기존처럼 `totalAmount` 총액만 노출)
- [x] 단위 테스트 — `ShippingFeePolicy`(또는 동등 계산 로직)에 대해 4개 조합(70,000 미만/이상 × 제주/비제주) + 경계값(우편번호 62999/63000/63644/63645) + 우편번호 파싱 실패 케이스 검증. `OrderService.createOrder` 통합 테스트로 실제 주문 생성 시 `totalAmount`에 배송비가 반영되는지 확인
- [x] 검증(자동): `./gradlew build`, `./gradlew test` 통과

## Phase 2: 프론트 — 체크아웃/마이페이지 주문상세 배송비 breakdown

이 phase가 끝나면 체크아웃 화면과 마이페이지 주문상세 화면 모두 "상품금액/배송비/총 결제금액" 3줄을 보여준다.

- [x] `frontend/src/lib/api/orders.ts`(또는 동등 타입 정의 파일)의 `OrderResponse` 타입에 `itemsSubtotal`, `shippingFee` 필드 추가
- [x] `frontend/src/lib/shipping.ts` 신규 — 백엔드 `ShippingFeePolicy`와 동일한 정책 상수(3,400/70,000/4,000/63000~63644) 및 계산 함수를 클라이언트에도 mirror. 체크아웃에서 배송지를 고르거나 새로 입력할 때 서버 왕복 없이 즉시 미리보기를 보여주는 용도로만 쓰고, 실제 결제 금액 확정은 항상 서버 응답(`POST /orders`의 `OrderResponse.totalAmount`)을 따른다는 점을 컴포넌트 주석 또는 리뷰 시 확인 가능하게 남긴다
- [x] `frontend/src/app/(shell)/checkout/page.tsx` — 기존 "총 결제금액" 1줄을 "상품금액 / 배송비 / 총 결제금액" 3줄로 확장. 저장된 배송지 선택 변경 또는 신규 배송지 폼의 우편번호 입력이 바뀔 때마다 `frontend/src/lib/shipping.ts`로 재계산해 즉시 갱신(제주 ↔ 비제주 전환 케이스 포함). 배송비가 0원이면 "무료"로 표시
- [x] `frontend/src/app/(shell)/mypage/orders/[orderId]/page.tsx` — 기존 "총 결제금액" 1줄을 동일하게 3줄로 확장하되, 이미 확정된 서버 응답(`OrderResponse.itemsSubtotal`/`shippingFee`/`totalAmount`)을 그대로 표시(클라이언트 재계산 없음)
- [x] 정적 확인 — `frontend/src/app/(shell)/checkout/payment/page.tsx`의 결제위젯 렌더링과 `backend/.../OrderPaymentTransactionSupport.assertPendingAndOwned`의 confirm 금액 검증이 이미 `order.totalAmount`(Phase 1에서 배송비가 반영된 값)를 그대로 참조하는 구조인지 코드로 재확인 — 수정 불필요, 리뷰 시 결제 계약이 깨지지 않았음을 확인하는 체크박스
- [x] 검증(자동): `npm run build`, `npm run lint` 통과
- [x] 검증(수동, 브라우저): 체크아웃에서 배송지를 제주 ↔ 비제주로 바꿔가며 배송비/총액이 즉시 갱신되는지, 마이페이지 주문상세에서 3줄 breakdown이 정확히 표시되는지 확인 — `e2e-tester`가 dev-browser로 대체 검증(`docs/e2e/2026-08-31-shipping-fee-policy.md` 시나리오 3·4), 2026-08-31 PASS

## Phase 3: 프론트 — 장바구니 무료배송 진행 안내(ShippingProgress) 연결

이 phase가 끝나면 장바구니에서 선택한 상품 금액에 따라 무료배송까지 남은 금액 또는 달성 안내가 보인다.

- [x] `frontend/src/components/feedback/ShippingProgress.tsx`의 하드코딩된 기준금액 `50000`을 `70000`으로 갱신
- [x] `frontend/src/app/(shell)/cart/page.tsx`에 지금까지 어디서도 import되지 않던 `ShippingProgress`를 상품 목록과 금액 요약 사이에 배치. `remaining = Math.max(0, 70000 - selectedSubtotal)`을 계산해 전달(`selectedSubtotal`은 체크박스로 선택된 상품 기준, 기존 `subtotal` 변수 재사용)
- [x] 검증(자동): `npm run build`, `npm run lint` 통과
- [x] 검증(수동, 브라우저): 장바구니에서 상품 선택/해제로 선택 금액이 70,000원 기준을 넘나들 때 안내 문구와 진행바가 올바르게 전환되는지 확인 — `e2e-tester`가 dev-browser로 대체 검증(`docs/e2e/2026-08-31-shipping-fee-policy.md` 시나리오 1·5), 2026-08-31 PASS

## Phase 4: E2E 검증

- [x] `e2e-tester`가 spec의 사용자 시나리오 1~4, 수용 기준(AC) 9개를 근거로 `docs/e2e/YYYY-MM-DD-shipping-fee-policy.md` 케이스를 도출해 실행 — 장바구니 무료배송 안내 전환, 체크아웃 배송지 전환에 따른 배송비 재계산, 주문 생성 후 마이페이지 주문상세 breakdown 확인 포함. Toss 결제위젯 confirm 성공 경로는 상점(스토어) 미등록으로 자동화 검증이 불가능하므로(`docs/backlog/2026-08-30-cart-order-payment-phase4-01.md`와 동일 제약) 해당 범위는 사전조건 미충족으로 스킵 처리하고 나머지 시나리오만 검증. 시나리오 1·2는 2026-08-31 1차 실행에서 PASS, 시나리오 3은 1차 실행 시 로컬 DB 상품 데이터 없음으로 스킵됐다가 DB 재시딩 후 2차 실행에서 PASS
