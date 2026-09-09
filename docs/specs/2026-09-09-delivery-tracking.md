---
date: 2026-09-09
feature: delivery-tracking
status: confirmed
---

# 배송조회

## 목적 (Why)

마이페이지 메뉴 5개 중 "배송조회"가 유일하게 무동작 상태(`onClick`이 빈 함수)로 남아 있다. 고객이 자신의 배송 상태를 확인하려면 주문상세를 하나하나 클릭해서 들어가야 하고, 목록 화면에서는 결제상태만 보이고 배송상태는 전혀 알 수 없다. "배송조회"라는 메뉴가 존재함에도 실제로 배송 상태를 조회할 방법이 없는 상태를 해소한다.

## 범위 (Scope)

### In Scope
- 마이페이지 "배송조회" 메뉴를 `/mypage/orders`로 연결
- `GET /orders` 응답(`OrderSummaryResponse`)에 `shippingStatus` 필드 추가
- 주문 목록 카드에 배송상태 뱃지 추가(결제상태 뱃지와 함께 노출)

### Out of Scope
- 신규 화면 개발(기존 `/mypage/orders` 재사용, 별도 배송조회 전용 화면 없음)
- 택배사 실시간 조회 API 연동 및 외부 조회 페이지 딥링크
- 목록 정렬 로직 변경(기존 생성일 최신순 유지)
- 배송상태별 필터링 기능

## 사용자 시나리오

1. 로그인한 사용자가 마이페이지에서 "배송조회" 메뉴를 클릭하면 `/mypage/orders`(주문내역)로 이동한다. 마이페이지 상단의 기존 "주문내역" 버튼과 동일한 목적지다.
2. 목록에서 결제완료(`PAID`) 상태이고 배송정보가 있는 주문 카드는 결제상태 뱃지 옆에 배송상태(배송준비중/배송중/배송완료) 뱃지가 함께 표시된다.
3. 결제대기/결제실패/취소된 주문 카드는 배송상태 뱃지 없이 결제상태 뱃지만 표시된다(기존과 동일 — 취소된 주문에 배송 단계 이전 상태가 남아 보이는 것을 방지).
4. 주문 카드를 클릭하면 기존과 동일하게 주문상세로 이동한다. 상세 화면의 배송상태·송장번호 노출은 이번 spec에서 변경하지 않는다.
5. 주문이 없거나 목록 로드에 실패하면 기존 빈 상태/에러 문구를 그대로 사용한다.

## 인터페이스

### API

- `GET /orders` 응답 `OrderSummaryResponse`에 `shippingStatus: ShippingStatus | null` 필드를 추가한다. 값은 `OrderResponse.shippingStatus`와 동일한 소스(`Order` 엔티티)에서 가져오며, 취소된 주문에 옛 배송상태가 남아 있어도 필드 값 자체는 그대로 내려준다 — 화면에서 노출 여부만 `status === "PAID"` 조건으로 제어한다(주문상세와 동일 패턴).

### 화면

- `frontend/src/app/(shell)/mypage/orders/page.tsx`: 주문 카드에 배송상태 뱃지 추가. 표시 조건(`order.shippingStatus && order.status === "PAID"`), 라벨/톤은 `src/lib/shippingStatus.ts`의 `SHIPPING_STATUS_LABEL`/`SHIPPING_STATUS_TONE`을 그대로 재사용한다.
- `frontend/src/app/(shell)/mypage/page.tsx`: "배송조회" 메뉴 항목의 `onClick`을 `() => router.push("/mypage/orders")`로 변경한다.

## 수용 기준 (Acceptance Criteria)

- [x] 마이페이지 "배송조회" 메뉴 클릭 시 `/mypage/orders`로 이동한다
- [x] `GET /orders` 응답에 `shippingStatus` 필드가 포함된다
- [x] 결제완료(PAID) 상태이며 `shippingStatus`가 있는 주문 카드에는 결제상태 뱃지 옆에 배송상태 뱃지가 표시된다
- [x] 결제대기/결제실패/취소 주문 카드에는 배송상태 뱃지가 표시되지 않는다
- [x] 배송상태 뱃지의 라벨/색상이 주문상세 화면과 동일하다(`배송준비중`/`배송중`/`배송완료`)
- [x] 목록 정렬 순서는 기존과 동일(생성일 최신순)하게 유지된다
- [x] `./gradlew build` / `./gradlew test`, `npm run build` / `npm run lint` 통과
- [x] E2E: 배송중 상태의 주문이 있는 계정으로 배송조회 진입 → 목록에서 배송상태 뱃지 확인 → 상세 진입 시 기존 배송정보와 일치하는지 확인
