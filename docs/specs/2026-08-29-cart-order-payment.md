---
date: 2026-08-29
feature: cart-order-payment
status: implemented
---

# 장바구니→주문→결제 (토스페이먼츠)

## 목적 (Why)

현재 `/cart`의 "결제하기" 버튼은 토스트만 띄우는 무동작 상태이고, 백엔드에는 주문(Order) 개념 자체가 없다. Auth 도입으로 로그인 사용자를 신뢰성 있게 식별할 수 있게 됐으니, 이제 실제 주문을 생성하고 토스페이먼츠로 결제를 완료할 수 있는 흐름을 만든다. 이 spec은 개인사업자 실 운영 서비스로서 실제 매출이 발생하는 첫 기능이다.

## 범위 (Scope)

### In Scope
- 장바구니(`localStorage` 유지)에서 항목별 체크박스 + 전체선택으로 부분/전체 결제 대상 선택
- `Product`에 정수 재고 필드(`stock`) 도입, 기존 상품은 마이그레이션 시 기본값 부여(`soldOut=true`인 상품은 `stock=0`)
- 사이즈는 `OrderItem`에 문자열로만 저장(옵션별 재고 없음)
- `User`에 연결된 다중 주소록(`Address`, 1:N), 기본배송지 플래그, 최초 주문 시점에 배송지 없으면 입력받아 저장
- Toss 결제위젯(Payment Widget) SDK 연동
- 주문서 제출 시 재고 선점(차감)과 `Order`(`PENDING`) 생성을 같은 트랜잭션으로 처리
- `Order` 상태: `PENDING` / `PAID` / `FAILED` / `CANCELLED` 4종
- 결제 실패 또는 `PENDING` 만료 시 `Order`는 `FAILED`로 종료, 재고 복원, 재시도 없이 사용자는 장바구니에서 새로 시작
- `PAID` 상태에서 사용자가 직접 취소 가능(시간 제한 없음, 재고 복원)
- 마이페이지 주문내역 목록 + 상세 화면
- 배송비 없음(상품가만 결제)

### Out of Scope
- 장바구니의 서버(Cart 엔티티) 이전 — localStorage 유지, 필요해지면 별도 spec
- 사이즈별 옵션/재고 관리(`ProductOption`) — 별도 spec
- 재고 관리 admin API/화면 — admin 화면 자체가 범위 밖(Auth spec에서 결정된 원칙 유지)
- 쿠폰 시스템(발급/사용/만료) — 마이페이지 쿠폰함은 계속 placeholder로 둠
- 배송비 정책(무료배송 기준, 지역별 추가요금 등) — 별도 spec
- 부분환불/교환, 배송 시작 후 취소, 배송 상태 추적(택배사 연동) — 별도 spec
- 결제 실패 시 같은 주문으로 재결제(재시도) — `confirm 재시도 없음` 컨벤션과 일치, 실패하면 항상 새 주문

## 사용자 시나리오

### 1. 장바구니에서 결제할 항목 선택
1. `/cart`에서 각 상품 항목에 체크박스가 있고, 상단에 "전체선택"이 있다.
2. 선택된 항목만 금액 요약(상품가 합계)에 반영된다.
3. "구매하기" 버튼은 선택된 항목이 1개 이상일 때만 활성화된다.

### 2. 주문서 작성 (배송지 입력/선택)
1. "구매하기"를 누르면 주문서 작성 화면으로 이동한다.
2. 저장된 배송지가 있으면 기본배송지가 자동 선택되고, 다른 저장된 주소로 변경하거나 새 주소를 추가할 수 있다.
3. 저장된 배송지가 없으면(최초 주문) 배송지 입력 폼이 바로 보이고, 입력 후 저장된다(기본배송지로 지정).
4. 주문 내용(선택된 상품, 수량, 사이즈, 합계 금액)을 최종 확인한다.

### 3. 결제 (Toss 결제위젯)
1. 주문서를 제출하면 서버가 재고를 선점(차감)하고 `Order`를 `PENDING` 상태로 생성한 뒤, Toss 결제위젯이 렌더링된다.
2. 사용자가 결제수단을 선택하고 결제를 완료하면 Toss가 콜백/리다이렉트로 결과를 알려주고, 서버가 confirm을 호출한다.
3. confirm 성공: `Order`가 `PAID`로 전환되고 주문 완료 화면(또는 주문내역 상세)으로 이동한다.
4. confirm 실패(또는 사용자가 결제창에서 이탈): `Order`가 `FAILED`로 전환되고 재고가 복원된다. 사용자는 실패 안내를 보고 장바구니로 돌아가 처음부터 다시 시도해야 한다(같은 주문 재결제 없음).
5. 결제창을 띄운 채로 사용자가 아무 조치 없이 오래 방치하면, 스케줄러가 만료된 `PENDING` 주문을 `FAILED`로 전환하고 재고를 복원한다.

### 4. 마이페이지 주문내역 조회
1. 마이페이지에서 주문내역 목록(상태/금액/일시)을 볼 수 있다.
2. 목록에서 항목을 선택하면 주문 상세(상품 구성, 배송지, 결제 상태)를 볼 수 있다.

### 5. 주문 취소
1. `PAID` 상태인 주문은 상세 화면에서 취소 버튼을 볼 수 있다(시간 제한 없음 — 배송 상태 추적이 범위 밖이라 별도 배송 시작 전이가 없는 한 계속 취소 가능).
2. 취소하면 `Order`가 `CANCELLED`로 전환되고 재고가 복원된다.
3. `FAILED`/`CANCELLED` 상태인 주문에는 취소 버튼이 노출되지 않는다.

## 인터페이스

### API

**`POST /orders`** (주문서 제출 — 재고 선점 + `PENDING` 생성)
- Request: `{ items: [{ productId, quantity, size }], addressId (기존 주소 선택 시) | address (신규 입력 시 { recipient, phone, zipcode, address1, address2 }) }`
- Response 201: `{ orderId, status: "PENDING", totalAmount, items: [...] }`
- 에러: `VALIDATION_FAILED`(400), `OUT_OF_STOCK`(409, 선점 시점에 재고 부족한 항목 포함), `PRODUCT_NOT_FOUND`(404)

**`POST /orders/{orderId}/confirm`** (Toss 결제 승인 콜백 후 서버가 호출)
- Request: `{ paymentKey, orderId(Toss 측 주문 식별자), amount }`
- Response 200: `{ orderId, status: "PAID" }`
- 에러: `PAYMENT_CONFIRM_FAILED`(402 또는 400 — Toss 응답 코드 매핑), `ORDER_NOT_PENDING`(409, 이미 처리된 주문)
- 실패 시 서버가 재고 복원 및 `Order` `FAILED` 전환까지 같은 트랜잭션/후속 처리로 수행

**`GET /orders`** (내 주문내역 목록)
- Response 200: `[{ orderId, status, totalAmount, createdAt, itemsSummary }]`

**`GET /orders/{orderId}`** (주문 상세)
- Response 200: `{ orderId, status, totalAmount, items: [...], address: {...}, createdAt }`
- 에러: `ORDER_NOT_FOUND`(404), `FORBIDDEN`(403, 타인 주문 조회 시도)

**`POST /orders/{orderId}/cancel`**
- Response 200: `{ orderId, status: "CANCELLED" }`
- 에러: `ORDER_NOT_CANCELLABLE`(409, `PAID`가 아닌 상태), `ORDER_NOT_FOUND`(404), `FORBIDDEN`(403)

**`GET /addresses`** / **`POST /addresses`** / **`PATCH /addresses/{id}`** (배송지 CRUD, 기본배송지 지정 포함)
- `POST`/`PATCH` Request에 `isDefault: boolean` 포함, true로 설정 시 기존 기본배송지는 자동 해제

### 화면

**`/cart` 변경**
- 각 항목에 체크박스, 상단 "전체선택" 체크박스
- 금액 요약이 선택된 항목 기준으로만 계산
- "구매하기" 버튼: 선택 항목 0개면 비활성화

**`/checkout`(신규, 주문서 작성)**
- 배송지 섹션: 저장된 주소 목록에서 선택(기본배송지 우선 표시) 또는 "새 배송지 추가"
- 주문 상품 목록(선택된 항목 요약, 읽기 전용)
- 합계 금액
- "결제하기" 버튼 → 제출 시 `POST /orders` 호출 후 Toss 결제위젯 렌더링

**결제위젯 화면**
- Toss Payment Widget SDK 렌더링, 결제수단 선택 UI는 Toss가 제공

**주문 완료/실패 화면**
- 성공: 주문 요약 + "주문내역 보기" 링크
- 실패: 실패 안내 + "장바구니로 돌아가기" 링크

**마이페이지 주문내역 (`/mypage/orders`, 신규)**
- 목록: 상태 배지, 금액, 주문일시
- 상세(`/mypage/orders/[orderId]`): 상품구성, 배송지, 결제상태, `PAID`면 취소 버튼

**마이페이지 배송지 관리** (최초 주문 시 자동 생성되는 흐름 외에, 이후 조회/추가/기본지정 UI도 필요 — 체크아웃 화면 내에서 처리, 별도 마이페이지 메뉴는 이번 스펙에서 필수 아님)

### 데이터 모델

**`Product` 변경**
- `stock` (Integer, not null, default 마이그레이션 시 부여) 추가
- `@Version` 낙관적 락 필드 추가(선점/복원 동시성 제어)

**`Address`**
- `id` (PK)
- `user_id` (FK → User)
- `recipient`, `phone`, `zipcode`, `address1`, `address2`
- `is_default` (Boolean, default false)
- `created_at`

**`Order`**
- `id` (PK)
- `user_id` (FK → User)
- `status` (`PENDING` | `PAID` | `FAILED` | `CANCELLED`)
- `total_amount`
- `address_id` (FK → Address, 스냅샷이 아닌 참조 — 주문 시점 배송지 그대로 유지 가정)
- `toss_payment_key` (nullable, confirm 성공 시 기록)
- `created_at`, `updated_at`

**`OrderItem`**
- `id` (PK)
- `order_id` (FK → Order)
- `product_id` (FK → Product)
- `quantity`
- `size` (String, nullable)
- `unit_price` (주문 시점 가격 스냅샷)

## 수용 기준 (Acceptance Criteria)

- [x] 장바구니에서 항목을 체크/해제하면 금액 요약이 선택된 항목만 반영해 갱신된다
- [x] 선택된 항목이 0개면 "구매하기" 버튼이 비활성화된다
- [x] "구매하기" 진입 시 저장된 기본배송지가 있으면 자동 선택되고, 없으면 배송지 입력 폼이 즉시 보인다
- [x] 새 배송지를 입력해 저장하면 이후 주문에서 선택 가능한 주소록에 추가된다
- [x] 주문서를 제출하면 재고가 선점(차감)되고 `Order`가 `PENDING`으로 생성된 뒤 Toss 결제위젯이 뜬다
- [x] 재고가 부족한 상품이 포함되면 `OUT_OF_STOCK` 에러로 주문 생성이 거부되고 재고 차감이 일어나지 않는다
- [x] 결제 성공 시 `Order`가 `PAID`로 전환되고 주문 완료 화면이 보인다 (백엔드 `PaymentService` 테스트로 검증 — 브라우저 E2E는 Toss 샌드박스 실카드 결제가 필요해 스킵, `docs/e2e/2026-08-29-cart-order-payment.md` 시나리오 7)
- [x] 결제 실패(또는 사용자 이탈) 시 `Order`가 `FAILED`로 전환되고 재고가 복원되며, 같은 주문으로 재결제할 방법이 없다
- [x] `PENDING` 상태로 일정 시간 방치된 주문은 스케줄러에 의해 `FAILED`로 전환되고 재고가 복원된다
- [x] 마이페이지 주문내역 목록에서 상태/금액/일시가 보이고, 상세로 진입하면 상품구성/배송지/상태를 볼 수 있다
- [x] `PAID` 상태의 주문만 취소 버튼이 보이고, 취소하면 `CANCELLED`로 전환되며 재고가 복원된다 (백엔드 `PaymentService` 테스트로 검증 — 브라우저 E2E는 위와 동일한 사유로 스킵)
- [x] `FAILED`/`CANCELLED` 상태의 주문에는 취소 버튼이 보이지 않는다
- [x] 타인의 주문을 `orderId`로 직접 조회/취소 시도하면 `FORBIDDEN`으로 거부된다
- [x] 동시에 같은 상품에 대해 여러 주문이 재고 선점을 시도하면 낙관적 락으로 하나만 성공하고 나머지는 `OUT_OF_STOCK`으로 처리된다
