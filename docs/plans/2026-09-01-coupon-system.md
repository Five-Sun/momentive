---
date: 2026-09-01
feature: coupon-system
spec: 2026-09-01-coupon-system.md
status: done
---

# 쿠폰 시스템 플랜

## 개요

`docs/specs/2026-09-01-coupon-system.md`를 기반으로 한다. 쿠폰 도메인을 신설하고, 기존 주문·결제 흐름에 할인을 끼워 넣은 뒤, 쿠폰함과 체크아웃 화면을 붙이는 순서다.

Phase를 이렇게 나눈 이유는 **위험 구간을 뒤로 미루지 않고 격리하기 위해서**다. 이 작업에서 유일하게 이미 배포된 데이터에 영향을 주는 부분은 `Order`의 금액 필드 재구성(`getItemsSubtotal()` 역산 제거 + 기존 주문 백필)인데, 이것을 쿠폰 도메인 신설과 한 phase에 섞으면 실패 시 원인 절단면이 흐려진다. 그래서 Phase 1은 주문과 완전히 무관한 쿠폰 도메인만 세우고(등록·조회가 독립적으로 동작하는 상태), Phase 2에서 비로소 주문과 결합한다.

프론트는 백엔드가 전부 선 뒤에 붙인다. Phase 3(쿠폰함)은 체크아웃과 무관하게 단독 검증이 가능하고, Phase 4는 결제 흐름 전체(체크아웃 적용 → 장바구니 정리 → 주문상세 표시)를 한 덩어리로 묶어 금액 표시의 일관성을 한 번에 확인한다.

Phase 5는 `e2e-tester`가 브라우저로 유저 플로우를 검증한다. **Toss confirm 성공 경로는 상점 미등록 제약(`docs/backlog/2026-08-30-cart-order-payment-phase4-01.md`)으로 이번에도 검증 불가**이며, 위젯 렌더 금액까지만 확인 대상이다.

## Phase 1: 백엔드 쿠폰 도메인

쿠폰 등록·조회가 주문 도메인과 무관하게 독립적으로 동작하는 상태가 된다. 이 phase가 끝나면 Swagger UI에서 `POST /coupons/register`로 시드 쿠폰을 등록하고 `GET /coupons`로 조회할 수 있다.

- [x] `coupon` 도메인 패키지 신설 — `Coupon` 엔티티(`code`/`name`/`discountType`/`discountValue`/`maxDiscountAmount`/`minOrderAmount`/`expiresAt`), `DiscountType` enum(`FIXED`/`PERCENT`)
- [x] `UserCoupon` 엔티티 — `user`/`coupon` ManyToOne, `UserCouponStatus` enum(`AVAILABLE`/`USED`), `usedOrderId`, `registeredAt`, `usedAt`. 만료는 상태값이 아니라 `coupon.expiresAt` 기준 파생 판정으로 구현
- [x] flyway 마이그레이션 — `coupon`/`user_coupon` 테이블 생성, `user_coupon`에 `unique(user_id, coupon_id)` 제약 포함
- [x] flyway 마이그레이션 — 초기 쿠폰 시드(정액 쿠폰 1개 이상 + 정률 쿠폰 1개 이상, 최소 주문금액 있는 것과 없는 것을 모두 포함해 이후 phase의 검증 데이터로 쓸 수 있게 구성)
- [x] `CouponRepository`, `UserCouponRepository` — 코드 조회(대문자 정규화 비교), 사용자별 보유 목록 조회
- [x] `CouponService` — `register(userId, code)`(존재하지 않는 코드/만료/중복 등록 3종 분기), `findMyCoupons(userId)`(사용 가능 먼저, 사용 완료·만료 뒤 정렬)
- [x] `CouponController` — `POST /coupons/register`(201), `GET /coupons`(200). `backend/CLAUDE.md` 컨벤션대로 `@Operation` summary, `@SecurityRequirement`, `@CurrentUser` 파라미터에 `@Parameter(hidden = true)` 적용
- [x] 요청/응답 DTO 전 필드에 `@Schema` — `CouponRegisterRequest`, `UserCouponResponse`
- [x] 등록 실패 3종이 각각 다른 문구로 `fieldErrors.code`에 담기도록 예외 처리 연결
- [x] `CouponService` 단위/통합 테스트 — 등록 성공, 실패 3종, 목록 정렬 순서
- [x] 검증 — `./gradlew build`와 `./gradlew test` 통과

## Phase 2: 백엔드 주문 연동

쿠폰을 적용한 주문이 올바른 금액으로 생성되고, 상태 전이(성공/실패/만료/취소)마다 쿠폰이 정확히 선점·복원된다. **기존 주문이 회귀 없이 그대로 조회되는 것이 이 phase의 핵심 검증 포인트다.**

- [x] `Order`에 `itemsSubtotal`, `discountAmount`, `userCoupon`(ManyToOne nullable) 추가하고 **`getItemsSubtotal()` 역산 메서드 제거** (`backend/src/main/java/com/momentive/backend/order/domain/Order.java`)
- [x] `Order.confirmAmounts()`를 할인액까지 받도록 확장 — `totalAmount = itemsSubtotal - discountAmount + shippingFee`
- [x] flyway 마이그레이션 — `orders`에 `items_subtotal`/`discount_amount`/`user_coupon_id` 추가, 기존 행을 `items_subtotal = total_amount - shipping_fee`, `discount_amount = 0`, `user_coupon_id = NULL`로 백필
- [x] 할인 계산 로직을 도메인으로 분리 — `CouponDiscountPolicy`(또는 동등 위치). `FIXED`는 `min(discountValue, itemsSubtotal)`, `PERCENT`는 `min(itemsSubtotal × 비율, maxDiscountAmount, itemsSubtotal)`
- [x] `OrderCreateRequest`에 선택 필드 `userCouponId` 추가(`@Schema` 포함)
- [x] `OrderService.create()`에 쿠폰 처리 — 소유자 일치·`AVAILABLE` 상태·`expiresAt` 미경과·`minOrderAmount` 충족 4종 재검증 후 할인 적용 및 `UserCoupon` 선점(`USED` + `usedOrderId`). 위반 시 400
- [x] **배송비는 할인 전 `itemsSubtotal`로 계산** — `ShippingFeePolicy.calculate()`에 넘기는 인자를 할인액으로 바꾸지 않는다
- [x] 쿠폰 복원 3개 경로 — 결제 실패(`PaymentService`), `PENDING` 만료(`OrderExpirationService`/`OrderExpirationScheduler`), `PAID` 주문 취소. 재고 복원과 같은 트랜잭션에서 `UserCoupon`을 `AVAILABLE`로 되돌리고 `usedOrderId`/`usedAt` 초기화
- [x] `OrderResponse`에 `discountAmount`, `couponName`(미사용 시 `null`) 추가 + `@Schema`
- [x] 테스트 — 쿠폰 적용 주문 금액 계산(정액/정률/상한/상품금액 초과 방어), 무료배송 임계값이 할인 전 기준인지, 복원 3경로, 검증 실패 4종
- [x] 검증 — `./gradlew build`와 `./gradlew test` 통과, 기존 주문 관련 테스트 회귀 없음

## Phase 3: 프론트 쿠폰함

`/mypage/coupons`가 독립적으로 동작한다. 코드를 등록하고 보유 쿠폰을 확인할 수 있으며, 체크아웃과는 아직 무관하다.

- [x] `frontend/src/lib/api/coupon.ts` — `registerCoupon(code)`, `fetchMyCoupons()`. 기존 `apiFetch` 래퍼와 `ApiError` 타입 사용
- [x] `/mypage/coupons` 페이지 신설 — 코드 입력 폼(React Hook Form + Zod), 서버 `fieldErrors.code`를 입력칸 아래 인라인 매핑 (`frontend/CLAUDE.md` 에러 처리 컨벤션)
- [x] 보유 쿠폰 목록 — 탭 없이 "사용 가능한 쿠폰"(상단)과 "사용 완료·만료"(하단, 비활성 스타일) 두 구간. 쿠폰명/할인 내용/최소 주문금액/유효기간 표시
- [x] 빈 상태(보유 쿠폰 0개) 안내와 비로그인 안내 처리 — 기존 마이페이지 하위 화면의 조건부 렌더링 패턴 준용
- [x] 마이페이지 메뉴의 "쿠폰함" `onClick: () => {}`를 `/mypage/coupons` 라우팅으로 교체 (`frontend/src/app/(shell)/mypage/page.tsx`). "배송조회"/"적립금"은 무동작 유지
- [x] `docs/design.md` 토큰·컴포넌트 일관성 확인 — 기존 `/mypage/pets`, `/mypage/support` 화면과 같은 레이아웃 언어 사용
- [x] 검증 — `npm run build`와 `npm run lint` 통과

## Phase 4: 프론트 결제 흐름 반영

체크아웃에서 쿠폰을 적용해 결제할 수 있고, 장바구니의 가짜 UI가 사라지며, 주문상세에 할인 내역이 보인다. 금액 표시가 세 화면에서 일관된다.

- [x] 할인 계산 프론트 미러 — `calculateShippingFee`와 같은 방식으로 `calculateCouponDiscount()` 구현. Phase 2의 `CouponDiscountPolicy`와 동일한 계산식
- [x] 체크아웃에 쿠폰 선택 영역 추가 (`frontend/src/app/(shell)/checkout/page.tsx`) — 배송지 영역과 금액 요약 사이에 배치. 보유 사용 가능 쿠폰 전부 나열, 최소 주문금액 미달 쿠폰은 선택 불가 + 사유 문구 표시
- [x] 쿠폰 선택/해제 시 금액 요약 즉시 갱신 — 상품금액 / 쿠폰 할인(선택 시에만 노출) / 배송비 / 총 결제금액. 보유 쿠폰이 없으면 쿠폰 영역 자체를 감춤
- [x] 주문 생성 요청에 `userCouponId` 전달, Toss 결제위젯이 서버가 확정한 `totalAmount`로 렌더되는지 확인
- [x] **장바구니에서 쿠폰 토글 UI, `COUPON_DISCOUNT = 3000` 상수, 관련 `useState` 제거** (`frontend/src/app/(shell)/cart/page.tsx`). `ShippingProgress`와 금액 요약은 유지
- [x] 마이페이지 주문상세 금액 내역에 "쿠폰 할인 −N원" + 쿠폰명 추가. `discountAmount`가 0이거나 `couponName`이 `null`이면 줄 자체를 렌더하지 않음. 주문내역 목록 화면은 변경하지 않음
- [x] 검증 — `npm run build`와 `npm run lint` 통과

## Phase 5: E2E 검증

`e2e-tester`가 `.claude/rules/e2e-format.md` 규격으로 케이스를 작성하고 dev-browser로 실행한다. 로컬 서버(`./dev.sh`)가 기동된 상태여야 한다.

- [x] 쿠폰 코드 등록 — 성공, 그리고 실패 3종(없는 코드/만료/중복 등록)의 인라인 에러 문구 확인 (만료 쿠폰 케이스는 시드에 만료 쿠폰이 없고 관리자 발급 API도 없어 사전조건 미충족으로 스킵, 나머지 성공/없는 코드/중복 등록은 PASS)
- [x] 쿠폰함 목록 — 사용 가능/사용 완료·만료 구간 분리 표시 확인
- [x] 체크아웃 쿠폰 적용 — 선택/해제 시 금액 요약 즉시 갱신, 최소 주문금액 미달 쿠폰의 비활성 상태와 사유 문구 확인
- [x] 무료배송 임계값이 **할인 전** 상품금액 기준인지 확인 — 쿠폰 적용 후 금액이 70,000원 아래로 내려가도 배송비가 붙지 않을 것
- [x] 쿠폰 적용 주문 생성 → 쿠폰이 선점되어 쿠폰함에서 사용 불가로 바뀌는지 확인
- [x] 주문상세에 할인 줄과 쿠폰명이 표시되고, 쿠폰 미사용 주문에서는 렌더되지 않는지 확인
- [x] 기존 주문(마이그레이션 백필 대상)의 주문상세 금액 내역 회귀 없음 확인 (재시딩 직후라 V11 이전 orders 로우를 재현할 방법이 없어 사전조건 미충족으로 스킵 — 이 로직 자체는 Phase 2 마이그레이션 작성 시점의 리뷰/테스트로 이미 커버됨)
- [x] 장바구니에 쿠폰 관련 UI가 더 이상 없는지 확인
- [x] Toss confirm 성공 경로는 상점 미등록 제약으로 스킵 — 위젯 렌더 금액이 할인 반영 금액과 일치하는지까지만 확인하고 스킵 사유를 보고에 명시
