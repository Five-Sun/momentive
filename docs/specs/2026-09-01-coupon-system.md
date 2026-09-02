---
date: 2026-09-01
feature: coupon-system
status: confirmed
---

# 쿠폰 시스템

## 목적 (Why)

마이페이지 "쿠폰함" 메뉴가 `onClick: () => {}`로 완전 무동작이고(`frontend/src/app/(shell)/mypage/page.tsx:17`), 장바구니의 쿠폰 토글은 `useState` 하나와 `COUPON_DISCOUNT = 3000` 하드코딩으로 "할인금액 −3,000원" 표시만 바꿀 뿐 실제 결제금액에는 반영되지 않는다(`frontend/src/app/(shell)/cart/page.tsx:80-83`). 백엔드에는 쿠폰 엔티티·테이블·API가 전혀 없다.

즉 **실 고객에게 이미 노출된 거짓 UI**다. 고객이 토글을 켜도 결제금액이 그대로여서 혼란을 주고, 쿠폰함을 눌러도 아무 일이 일어나지 않는다.

동시에, 인스타그램(`@momentive_official`)으로 프로모션 코드를 배포해 신규 구매를 유도할 수단이 현재 전혀 없다. 이 spec은 그 두 가지를 함께 해소한다 — 껍데기 UI를 실제 동작으로 채우고, 코드 기반 쿠폰 배포라는 운영 수단을 만든다.

## 범위 (Scope)

### In Scope

- 쿠폰 정의(`Coupon`)와 사용자별 발급분(`UserCoupon`) 도메인 신설
- 쿠폰 종류 2종: 정액 할인, 정률 할인(할인 상한액 필수)
- 쿠폰 사용 조건: 유효기간, 최소 주문금액, 1인 1회
- 쿠폰 코드 입력을 통한 사용자 자가 등록
- 쿠폰함 화면(`/mypage/coupons`) 신설 및 마이페이지 메뉴 연결
- 체크아웃 화면의 쿠폰 선택 및 할인 반영
- 장바구니의 가짜 쿠폰 토글 제거
- `Order`의 금액 필드 재구성 — `itemsSubtotal` 명시 컬럼 승격, `discountAmount` 추가
- 주문 생성 시 쿠폰 선점, 결제 실패·만료·주문 취소 시 복원
- 마이페이지 주문상세의 할인 내역 표시

### Out of Scope

- **적립금(포인트)** — 적립 시점·소멸 정책·취소 시 회수까지 별도 정책 결정이 필요하므로 분리한다. 마이페이지 "적립금" 메뉴는 계속 무동작으로 둔다
- **배송조회** — Order 도메인에 배송 상태 개념 자체가 없고 택배사 연동이 필요하다. 마이페이지 "배송조회" 메뉴는 계속 무동작으로 둔다
- **무료배송 쿠폰** — 제주 우편번호 할증(+4,000원)을 덮는지 여부가 별도 논쟁이고, 배송비 정책이 방금 도입된 참이라 함께 흔들지 않는다
- **관리자 쿠폰 발급 API/화면** — 관리자 도메인 자체가 존재하지 않는다(`User.role`에 `ADMIN` enum만 있고 부여 경로도 검사 지점도 없음). 쿠폰 정의는 상품과 동일하게 flyway 마이그레이션으로 시드한다
- **회원가입 시 자동 지급** — 발급 경로는 코드 입력 하나로 고정한다
- **특정 상품·카테고리 한정 쿠폰** — 시드 상품이 15개뿐이고 카테고리가 ACCESSORY로 쏠려 있어 지금 만들어도 쓸 데가 없다
- **전체 발급 수량 제한(선착순 소진)** — 관리자 화면 없이는 소진 수량을 조절할 수 없어 위험하다. 유효기간으로만 통제한다
- **한 주문에 쿠폰 여러 장 중복 사용** — 쿠폰 간 우선순위·중복 허용 플래그가 파생된다
- **쿠폰 사용 취소 후 유효기간 연장** — 복원은 하되 기간은 손대지 않는다

## 사용자 시나리오

### 1. 쿠폰 코드 등록

1. 로그인한 사용자가 마이페이지에서 "쿠폰함"을 누른다 → `/mypage/coupons`로 이동한다
2. 화면 상단의 코드 입력칸에 인스타그램에서 받은 코드(예: `WELCOME3000`)를 입력하고 등록한다
3. 성공하면 아래 "사용 가능한 쿠폰" 목록에 해당 쿠폰이 즉시 나타난다
4. 실패 시 입력칸 아래에 사유별로 다른 문구가 인라인으로 표시된다
   - 존재하지 않는 코드
   - 유효기간이 지난 쿠폰
   - 이미 등록한 쿠폰 (1인 1회)
5. 비로그인 상태로 `/mypage/coupons`에 접근하면 로그인 안내를 보여준다

### 2. 쿠폰 사용 (체크아웃)

1. 장바구니에서 결제할 상품을 선택하고 체크아웃으로 진입한다 (장바구니에는 쿠폰 관련 UI가 더 이상 없다)
2. 체크아웃 화면에서 배송지를 선택하거나 새로 입력한다
3. 쿠폰 선택 영역에 보유한 사용 가능 쿠폰이 **전부** 나열된다. 최소 주문금액에 미달하는 쿠폰은 선택할 수 없는 상태로 표시되고, 그 아래에 사유가 보인다 (예: "30,000원 이상 구매 시 사용 가능")
4. 쿠폰을 선택하면 금액 요약의 "쿠폰 할인" 줄과 최종 결제금액이 즉시 갱신된다. 선택을 해제하면 원래대로 돌아간다
5. 배송비는 **할인 전** 상품금액으로 판정되므로, 쿠폰을 적용해도 무료배송 여부는 바뀌지 않는다
6. "결제하기"를 누르면 주문이 `PENDING` 상태로 생성되면서 쿠폰이 선점된다(다른 주문에 다시 쓸 수 없다)
7. Toss 결제위젯이 **할인이 반영된 최종 금액**으로 렌더된다

### 3. 결제 결과에 따른 쿠폰 처리

- **결제 성공(`PAID`)**: 쿠폰이 사용 완료로 확정된다. 쿠폰함의 "사용 완료·만료" 구간으로 내려간다
- **결제 실패(`FAILED`)**: 쿠폰이 사용 가능 상태로 복원되어 다시 쓸 수 있다
- **`PENDING` 만료**: 만료 스케줄러가 재고를 복원할 때 쿠폰도 함께 복원한다
- **`PAID` 주문 취소(`CANCELLED`)**: 재고 복원과 함께 쿠폰도 사용 가능 상태로 복원한다. 단 복원 시점에 이미 유효기간이 지났다면 만료된 쿠폰으로 보일 뿐 사용할 수 없다

### 4. 주문 내역 확인

1. 마이페이지 주문내역 **목록**은 현행대로 총 결제금액만 보여준다
2. 주문 **상세**로 들어가면 금액 내역에 "쿠폰 할인 −N원" 줄과 사용한 쿠폰 이름이 표시된다
3. 쿠폰을 쓰지 않은 주문에서는 할인 줄 자체가 보이지 않는다

### 예외 케이스

- 할인액이 상품금액보다 큰 경우(정액 쿠폰 + 최소 주문금액 미설정): 할인액을 상품금액까지만 적용해 상품금액이 음수가 되지 않게 한다. 이때 결제금액은 배송비만 남는다
- 쿠폰 선택 후 결제하기를 누르기 전에 쿠폰이 만료된 경우: 서버가 주문 생성 시 재검증하여 거부한다
- 프론트에 미러링된 할인 계산과 서버 계산이 어긋나는 경우: 서버 계산이 항상 정답이다. Toss confirm은 `order.totalAmount`를 단일 소스로 검증하므로 금액 위변조로 이어지지 않는다

## 인터페이스

### API

모두 인증 필요(`@SecurityRequirement`). `@Operation` summary와 DTO `@Schema`는 `backend/CLAUDE.md` 컨벤션대로 붙인다.

#### `POST /coupons/register` — 쿠폰 코드 등록

Request
```json
{ "code": "WELCOME3000" }
```

Response `201`
```json
{
  "id": 12,
  "couponName": "웰컴 3,000원 할인",
  "discountType": "FIXED",
  "discountValue": 3000,
  "maxDiscountAmount": null,
  "minOrderAmount": 30000,
  "expiresAt": "2026-12-31T23:59:59",
  "status": "AVAILABLE"
}
```

에러 — 모두 `400`, `fieldErrors.code`에 사유별 문구
| 상황 | 문구 |
|---|---|
| 존재하지 않는 코드 | 존재하지 않는 쿠폰 코드입니다 |
| 유효기간 만료 | 유효기간이 지난 쿠폰입니다 |
| 이미 등록함 | 이미 등록한 쿠폰입니다 |

#### `GET /coupons` — 보유 쿠폰 목록

Response `200` — 사용 가능한 쿠폰이 먼저, 사용 완료·만료가 뒤에 오도록 정렬한다. 각 항목은 `POST /coupons/register` 응답과 동일한 형태에 `usedOrderId`를 더한다. 만료 여부는 `expiresAt`으로 판정되며 별도 상태값을 두지 않는다.

#### `POST /orders` — 주문 생성 (기존 API 확장)

Request에 선택 필드 `userCouponId` 추가. 값이 있으면 서버가 다음을 검증한 뒤 할인을 적용하고 쿠폰을 선점한다.
- 요청자 본인의 `UserCoupon`인가
- 상태가 `AVAILABLE`인가
- `expiresAt`이 지나지 않았는가
- 상품금액이 `minOrderAmount` 이상인가

검증 실패 시 `400`. 기존 재고 부족 응답(`OutOfStockItem`) 구조는 변경하지 않는다.

#### `OrderResponse` 확장

`itemsSubtotal`, `shippingFee`, `totalAmount`에 더해 `discountAmount`(Integer)와 `couponName`(String, 미사용 시 `null`)을 추가한다.

#### 변경하지 않는 것

`POST /payments/confirm`의 금액 검증(`OrderPaymentTransactionSupport.assertPendingAndOwned`)은 `order.getTotalAmount()`를 단일 소스로 비교하므로 **손대지 않는다**. 할인은 주문 생성 시점에 이미 `totalAmount`에 반영되어 있다.

### 화면

#### `/mypage/coupons` — 쿠폰함 (신설)

- 상단: 쿠폰 코드 입력 폼(입력칸 + 등록 버튼). React Hook Form + Zod, 서버 `fieldErrors`를 입력칸 아래 인라인 매핑 (`frontend/CLAUDE.md` 컨벤션)
- 하단: 보유 쿠폰 목록. 탭 없이 한 화면에서 두 구간으로 나눈다
  - "사용 가능한 쿠폰" — 쿠폰명, 할인 내용, 최소 주문금액, 유효기간
  - "사용 완료·만료" — 같은 정보를 흐리게(비활성 스타일) 표시
- 보유 쿠폰이 하나도 없을 때 빈 상태 안내를 보여준다
- 비로그인 시 로그인 안내를 보여준다

#### 마이페이지 메뉴

"쿠폰함" 항목의 `onClick: () => {}`를 `/mypage/coupons` 라우팅으로 교체한다. "배송조회"와 "적립금"은 무동작 그대로 둔다.

#### 체크아웃 (`/checkout`)

- 배송지 선택 영역과 금액 요약 사이에 쿠폰 선택 영역을 추가한다
- 보유한 사용 가능 쿠폰을 전부 나열한다. 최소 주문금액 미달 쿠폰은 선택 불가 상태 + 사유 문구를 함께 표시한다
- 쿠폰 선택/해제 시 금액 요약이 즉시 갱신된다. 계산 로직은 `calculateShippingFee`와 같은 방식으로 프론트에 미러링한다
- 금액 요약 구성: 상품금액 / **쿠폰 할인(−N원, 쿠폰 선택 시에만 노출)** / 배송비 / 총 결제금액
- 보유 쿠폰이 없으면 쿠폰 영역 자체를 감춘다

#### 장바구니 (`/cart`)

- 쿠폰 토글 UI와 `COUPON_DISCOUNT = 3000` 상수, 관련 `useState`를 **제거**한다
- 기존 `ShippingProgress`(무료배송 진행바)와 금액 요약은 그대로 유지한다

#### 마이페이지 주문상세

금액 내역에 "쿠폰 할인 −N원" 줄과 쿠폰명을 추가한다. `discountAmount`가 0이거나 `couponName`이 `null`이면 줄 자체를 렌더하지 않는다. 주문내역 **목록** 화면은 변경하지 않는다.

### 데이터 모델

#### `Coupon` (쿠폰 정의, 신규)

| 필드 | 타입 | 제약 |
|---|---|---|
| `id` | Long | PK |
| `code` | String | unique, not null. 등록 시 대문자로 정규화해 비교 |
| `name` | String | not null. 화면 표시용 (예: "웰컴 3,000원 할인") |
| `discountType` | enum | `FIXED` \| `PERCENT`, not null |
| `discountValue` | Integer | not null. `FIXED`면 원 단위, `PERCENT`면 퍼센트 |
| `maxDiscountAmount` | Integer | `PERCENT`일 때 필수, `FIXED`일 때 null |
| `minOrderAmount` | Integer | not null, 기본 0 |
| `expiresAt` | LocalDateTime | not null |
| `createdAt` | LocalDateTime | not null |

#### `UserCoupon` (사용자별 발급분, 신규)

| 필드 | 타입 | 제약 |
|---|---|---|
| `id` | Long | PK |
| `user` | User | ManyToOne, not null |
| `coupon` | Coupon | ManyToOne, not null |
| `status` | enum | `AVAILABLE` \| `USED`, not null. **만료는 상태가 아니라 `coupon.expiresAt`으로 파생 판정** |
| `usedOrderId` | Long | nullable. 어느 주문에 썼는지 추적 |
| `registeredAt` | LocalDateTime | not null |
| `usedAt` | LocalDateTime | nullable |

- `unique(user_id, coupon_id)` — 1인 1회 제약을 DB 레벨에서 보장

#### `Order` 변경

| 필드 | 변경 |
|---|---|
| `itemsSubtotal` | **신규 컬럼**. 기존에는 `totalAmount - shippingFee`로 역산하던 값을 명시 저장으로 승격하고 `getItemsSubtotal()` 역산 메서드를 제거한다 |
| `discountAmount` | **신규 컬럼**, not null, 기본 0 |
| `userCoupon` | **신규 연관관계**, ManyToOne nullable. 쿠폰명 표시와 취소 시 복원에 사용 |
| `totalAmount` | 계산식 변경: `itemsSubtotal - discountAmount + shippingFee` |

`Order.confirmAmounts(itemsSubtotal, shippingFee)`를 할인액까지 받도록 확장한다.

#### 마이그레이션

- `coupon`, `user_coupon` 테이블 생성
- `orders`에 `items_subtotal`, `discount_amount`, `user_coupon_id` 컬럼 추가. 기존 행은 `items_subtotal = total_amount - shipping_fee`, `discount_amount = 0`, `user_coupon_id = NULL`로 백필
- 초기 쿠폰 시드 (코드 배포용 쿠폰 정의)

## 수용 기준 (Acceptance Criteria)

- [x] 마이페이지 "쿠폰함"을 누르면 `/mypage/coupons`로 이동한다
- [x] 유효한 쿠폰 코드를 입력하면 "사용 가능한 쿠폰" 목록에 즉시 추가된다
- [x] 존재하지 않는 코드 / 만료된 쿠폰 / 이미 등록한 코드가 각각 다른 문구로 입력칸 아래 인라인 표시된다 (2026-09-02 QA에서 만료 쿠폰을 로컬 DB에 직접 시드해 3종 전부 확인)
- [x] 같은 코드를 같은 계정으로 두 번 등록할 수 없다 (DB unique 제약으로도 보장)
- [x] 쿠폰함에서 사용 완료·만료 쿠폰이 사용 가능 쿠폰과 구분되어 아래쪽에 표시된다
- [x] 장바구니 화면에 쿠폰 관련 UI가 더 이상 존재하지 않고 `COUPON_DISCOUNT` 상수가 코드에서 제거되었다
- [x] 체크아웃에서 보유 쿠폰이 전부 나열되고, 최소 주문금액 미달 쿠폰은 선택 불가 상태로 사유와 함께 표시된다
- [x] 체크아웃에서 쿠폰을 선택/해제하면 "쿠폰 할인" 줄과 총 결제금액이 즉시 갱신된다
- [x] 정률 쿠폰의 할인액이 `maxDiscountAmount`를 넘지 않는다 (2026-09-02 QA: 상품금액 72,000원에 10% 쿠폰 적용 시 7,200원이 아니라 상한 5,000원으로 제한됨을 화면에서 확인)
- [ ] 할인액이 상품금액보다 큰 경우 상품금액까지만 적용되어 결제금액이 배송비 이상으로 유지된다 (E2E 미검증, Phase 2 백엔드 테스트로 커버)
- [x] 무료배송 임계값(70,000원) 판정이 **할인 전** 상품금액 기준으로 이루어진다 — 쿠폰 적용 후 금액이 임계값 아래로 내려가도 배송비가 붙지 않는다
- [ ] 주문 생성 시 서버가 쿠폰 소유·상태·유효기간·최소 주문금액을 재검증하고, 위반 시 400으로 거부한다 (E2E 미검증, Phase 2 백엔드 테스트로 커버)
- [x] 주문 생성 후 해당 쿠폰이 선점되어 다른 주문에 다시 사용할 수 없다
- [ ] 결제 실패 시 쿠폰이 사용 가능 상태로 복원된다 (E2E 미검증, Phase 2 백엔드 테스트로 커버)
- [ ] `PENDING` 주문이 만료되면 재고와 함께 쿠폰도 복원된다 (E2E 미검증, Phase 2 백엔드 테스트로 커버)
- [ ] `PAID` 주문을 취소하면 재고와 함께 쿠폰도 복원된다 (E2E 미검증, Phase 2 백엔드 테스트로 커버)
- [ ] Toss 결제위젯이 할인이 반영된 최종 금액으로 렌더되고, confirm 금액 검증 로직은 변경 없이 통과한다 (위젯 렌더 금액은 E2E로 검증됨. confirm 성공 경로는 상점 미등록 제약으로 검증 못함)
- [x] 마이페이지 주문상세에 "쿠폰 할인 −N원"과 쿠폰명이 표시되고, 쿠폰 미사용 주문에서는 해당 줄이 렌더되지 않는다
- [ ] 기존 주문(마이그레이션 백필 대상)의 주문상세 금액 내역이 깨지지 않고 그대로 표시된다 (재시딩 직후라 V11 이전 orders 로우를 재현할 수 없어 E2E로 검증 못함, Phase 2 마이그레이션 작성 시점 리뷰로 커버)
- [x] `./gradlew build` / `./gradlew test` / `npm run build` / `npm run lint` 전부 통과한다 (2026-09-02 QA에서 Windows 머신 기준으로 4개 전부 재실행 — 백엔드 84 tests 실패 0)
