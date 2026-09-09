---
date: 2026-09-07
feature: order-shipping-management
status: confirmed
---

# 주문 배송상태·송장 관리

## 목적 (Why)

`Order`에는 배송 관련 정보가 전혀 없다(`OrderStatus`는 `PENDING/PAID/FAILED/CANCELLED`뿐). 고객은 결제 후 배송이 어느 단계인지 알 방법이 없고, 관리자도 발송 처리를 할 화면 자체가 없어 지금은 DB를 직접 건드리지 않는 한 아무것도 할 수 없다.

이번 spec은 `admin-product-management`로 막 세운 관리자 인가 기반 위에, **관리자 최초의 전체 주문 조회/처리 화면**과 **주문별 배송상태·송장 관리**를 추가한다. `docs/specs/2026-09-04-admin-product-management.md`의 후속(spec B)이며, 원래 같은 spec B에 묶여 있던 쿠폰 발급 기능은 이번 라운드에서 범위 제외했다(Out of Scope 참고).

## 범위 (Scope)

### In Scope
- `Order`에 배송상태(`shippingStatus`: 배송준비중/배송중/배송완료), 택배사명(`courier`), 송장번호(`trackingNumber`) 필드 추가
- 주문이 `PAID`로 전환되는 순간 `shippingStatus`를 "배송준비중"으로 자동 초기화
- 관리자 주문 목록/상세 API 및 화면 (첫 관리자용 전체 주문 조회)
- 관리자의 배송상태 변경 (수동, 순방향/역방향 제약 없음), "배송중"으로 전환 시 택배사·송장번호 필수 검증
- 관리자의 주문 취소 (기존 고객 취소와 동일한 재고 복원 로직 재사용, `PAID` 주문만 대상)
- 고객용 주문상세(`mypage/orders/[orderId]`)에 배송상태 뱃지 + 송장번호 노출

### Out of Scope
- 쿠폰 발급 API/화면 — 2026-09-07 그릴링에서 완전히 범위 제외. 관리자 도메인이 막 생긴 시점에 미리 만들 필요가 없다고 판단했고, 실제로 필요해지면 별도 세션에서 다룬다
- 실제 택배사 API 연동(실시간 배송 조회, 배송완료 자동 감지) — 송장 정보는 관리자가 수동으로 입력하는 텍스트일 뿐이다
- 배송상태 변경에 대한 알림(이메일/푸시) — 관련 인프라 자체가 없다
- 배송상태 자동/스케줄 전환 — 모든 전환은 관리자 수동 조작
- 관리자 주문 목록의 날짜 범위 검색, 고객 이메일 검색
- 3단계 "배송조회"(마이페이지 메뉴의 실시간 조회 경험) — 이번 spec은 상세 화면에 뱃지·송장번호를 노출하는 선에서 그친다

## 사용자 시나리오

**관리자 — 발송 처리**
1. 관리자가 `/admin/orders`에 접속한다. 기본 필터는 `PAID` 상태만 보여준다(페이지네이션 적용).
2. 관리자가 주문 하나를 선택해 상세로 들어간다. 배송상태는 "배송준비중"으로 표시돼 있다.
3. 관리자가 택배사(드롭다운, 목록에 없으면 "기타" 선택 후 직접 입력)와 송장번호를 입력하고 상태를 "배송중"으로 바꾼다.
4. 택배사·송장번호를 비운 채 "배송중"으로 바꾸려 하면 저장이 거부되고 안내 메시지가 뜬다.
5. 상품이 실제로 도착하면 관리자가 상태를 "배송완료"로 바꾼다.

**관리자 — 실수 정정**
6. 관리자가 실수로 "배송완료"를 눌렀다. 상세 화면에서 다시 "배송중"으로 되돌린다 — 순서 제약 없이 3단계 중 아무 상태나 선택할 수 있다.

**관리자 — 대신 취소**
7. 고객이 전화로 취소를 요청했다. 관리자가 해당 주문(`PAID` 상태)의 상세 화면에서 취소 버튼을 눌러 처리한다. 재고가 복원되고 주문은 `CANCELLED`로 전이된다. 이미 `PAID`가 아닌 주문에는 취소 버튼이 비활성화돼 있다.

**관리자 — 전체 보기**
8. 관리자가 "전체보기" 옵션을 켜면 `PENDING/FAILED/CANCELLED` 주문도 목록에 나타난다. 다만 이 주문들은 상세에서 배송상태 변경 액션이 비활성화돼 있다(애초에 배송상태 자체가 없음).

**고객 — 배송 확인**
9. 고객이 `마이페이지 > 주문내역 > 주문상세`로 들어간다. 배송상태 뱃지("배송준비중"/"배송중"/"배송완료")가 보이고, 송장번호가 입력된 경우 택배사명과 함께 표시된다. 아직 `PAID`가 안 됐거나(`PENDING`) 취소/실패된 주문에는 배송상태 뱃지가 없다.

## 인터페이스

### API

모든 `/admin/orders/**` 엔드포인트는 `SecurityConfig`의 `hasRole("ADMIN")`으로 보호되며, 권한 없는 요청은 컨트롤러 도달 전 403(`ErrorCode.FORBIDDEN`)으로 끊긴다.

**`GET /admin/orders`** — 관리자 주문 목록
- Query: `page`(기본 0), `size`(기본 20), `status`(복수 지정 가능한 `OrderStatus` 목록, 기본값 `PAID`. 전체보기는 프론트에서 `PENDING,PAID,FAILED,CANCELLED`를 모두 넘기는 방식으로 구현 — `AdminProductController.getProducts`와 동일한 컨벤션)
- Response: 페이지네이션된 주문 요약 목록(주문ID, 주문자, 총액, `OrderStatus`, `shippingStatus`(nullable), 생성일시)

**`GET /admin/orders/{orderId}`** — 관리자 주문 상세
- Response: `OrderResponse`에 상당하는 정보 + 주문자 이메일/이름 + `shippingStatus`, `courier`, `trackingNumber`
- 404: `ErrorCode.ORDER_NOT_FOUND`

**`PATCH /admin/orders/{orderId}/shipping`** — 배송상태·송장 변경
- Request body: `shippingStatus`(필수, `PREPARING|SHIPPING|DELIVERED`), `courier`(조건부 필수), `trackingNumber`(조건부 필수)
- 검증 규칙:
  - 대상 주문이 `PAID` 상태가 아니면 400 `ErrorCode.ORDER_SHIPPING_NOT_APPLICABLE`(신규 에러코드)
  - `shippingStatus`가 `SHIPPING` 또는 `DELIVERED`이면 `courier`·`trackingNumber`가 모두 비어있지 않아야 함 — 아니면 400 `ErrorCode.SHIPPING_INFO_REQUIRED`(신규 에러코드)
  - `PREPARING`으로 되돌릴 때는 `courier`/`trackingNumber` 값이 있어도 그대로 유지(초기화하지 않음) — 재입력 없이 다시 "배송중"으로 돌아갈 수 있게
- 순서 제약 없음: 현재 상태와 무관하게 3개 값 중 아무거나로 전환 가능
- Response: 갱신된 관리자 주문 상세

**`POST /admin/orders/{orderId}/cancel`** — 관리자 주문 취소
- 기존 `PaymentService.cancelOrder` / `OrderPaymentTransactionSupport`의 취소·재고복원 로직을 재사용하되, 소유자(`userId`) 검증만 건너뛴다 (관리자는 주문 소유자가 아니므로)
- `Order.isCancellable()`(= `status == PAID`) 기준은 고객 취소와 동일하게 적용 — 대상이 아니면 409 `ErrorCode.ORDER_NOT_CANCELLABLE`(기존 코드 재사용)
- Response: `OrderStatusResponse`(기존 고객 취소 API와 동일 타입)

**기존 고객용 `GET /orders/{orderId}` 응답 확장**
- `OrderResponse`에 `shippingStatus`(nullable), `courier`(nullable), `trackingNumber`(nullable) 필드 추가

### 화면

**`/admin/orders`** (신규)
- 주문 목록 테이블: 주문ID, 주문자, 총액, 주문상태, 배송상태, 생성일시
- 상단 필터: 상태 선택(기본 `PAID`만 체크된 상태) + "전체보기" 토글
- 페이지네이션 컨트롤

**`/admin/orders/[orderId]`** (신규)
- 주문 정보(주문자, 배송지, 상품 목록, 금액) — 기존 고객용 주문상세와 유사한 레이아웃 재사용
- 배송 관리 폼: 배송상태 선택(라디오/셀렉트 3단계), 택배사 드롭다운(CJ대한통운/우체국택배/한진택배/로젠택배/롯데택배/기타 — "기타" 선택 시 자유 텍스트 입력 노출), 송장번호 텍스트 입력, 저장 버튼
  - 대상 주문이 `PAID`가 아니면 이 폼 전체가 비활성화되고 안내 문구 표시
- 취소 버튼: `PAID` 상태일 때만 노출/활성화, 클릭 시 확인 다이얼로그 후 취소 처리

**`mypage/orders/[orderId]`** (기존 화면 확장)
- 배송상태 뱃지 추가 (배송상태가 없는 주문에는 뱃지 미노출)
- 송장번호가 있으면 택배사명과 함께 표시(예: "CJ대한통운 123456789012")

### 데이터 모델

**`Order` 엔티티에 필드 추가** (`backend/src/main/java/com/momentive/backend/order/domain/Order.java`)
- `shippingStatus`: `ShippingStatus` enum(`PREPARING`, `SHIPPING`, `DELIVERED`), nullable — `PAID` 이전 주문 및 `PENDING/FAILED/CANCELLED`로 끝난 주문은 `null`
- `courier`: `String`, nullable
- `trackingNumber`: `String`, nullable
- `markAsPaid(...)`에서 `shippingStatus = ShippingStatus.PREPARING`으로 자동 설정하는 로직 추가
- 신규 도메인 메서드(예: `updateShipping(ShippingStatus, String courier, String trackingNumber)`)로 상태 변경 — 필드 직접 대입 대신 도메인 메서드를 통해 불변식(‘PAID 상태에서만 가능’ 등)을 지키게 함

**마이그레이션** (`V17` 예정, 다음 미사용 번호 확인 후 배정)
- `orders` 테이블에 `shipping_status VARCHAR(20) NULL`, `courier VARCHAR(50) NULL`, `tracking_number VARCHAR(100) NULL` 컬럼 추가
- 백필: 이미 `PAID`인 기존 주문의 `shipping_status`를 `'PREPARING'`으로 일괄 설정(`UPDATE orders SET shipping_status = 'PREPARING' WHERE status = 'PAID'`) — 실제 송장 정보는 없으므로 관리자가 이후 수동으로 채워 넣어야 함을 배포 노트에 남긴다

**신규 `ErrorCode`**
- `SHIPPING_INFO_REQUIRED` (400) — "배송중/배송완료로 변경하려면 택배사와 송장번호를 입력해야 합니다."
- `ORDER_SHIPPING_NOT_APPLICABLE` (400) — "결제 완료된 주문만 배송상태를 관리할 수 있습니다."

## 수용 기준 (Acceptance Criteria)

- [x] 주문이 `PAID`로 전환되면 `shippingStatus`가 자동으로 `PREPARING`("배송준비중")이 된다 — `OrderTest`(단위테스트) + E2E
- [x] `GET /admin/orders`가 파라미터 없이 호출되면 `PAID` 상태 주문만 반환한다 — E2E 시나리오 1·2
- [x] `GET /admin/orders`에 `status=PENDING,PAID,FAILED,CANCELLED`를 넘기면 전체 상태 주문이 반환된다 — E2E 시나리오 2·`AdminOrderServiceTest`. **페이지네이션 자체(여러 페이지 이동)는 미확인** — 로컬 시드가 주문 1건뿐이라 재현 못함, 컨트롤은 `AdminProductController`와 동일 패턴 재사용이라 리스크는 낮다고 판단
- [x] `GET /admin/orders/{orderId}`가 배송상태·택배사·송장번호·주문자 정보를 포함해 반환한다. 존재하지 않는 주문ID는 404(`ORDER_NOT_FOUND`) — `AdminOrderServiceTest` + E2E
- [x] `PATCH /admin/orders/{orderId}/shipping`으로 `shippingStatus=SHIPPING`을 택배사·송장번호 없이 요청하면 400(`SHIPPING_INFO_REQUIRED`)이 반환되고 상태가 바뀌지 않는다 — `AdminOrderServiceTest` + E2E 시나리오 3
- [x] `PATCH /admin/orders/{orderId}/shipping`으로 `shippingStatus=DELIVERED`를 택배사·송장번호 없이 요청해도 동일하게 400(`SHIPPING_INFO_REQUIRED`)이 반환된다 — `AdminOrderServiceTest`(E2E에서는 SHIPPING 경로만 직접 재현, DELIVERED 경로는 단위테스트로 커버)
- [x] `PAID`가 아닌 주문(`PENDING`/`FAILED`/`CANCELLED`)에 `PATCH .../shipping`을 요청하면 400(`ORDER_SHIPPING_NOT_APPLICABLE`)이 반환된다 — `OrderTest`/`AdminOrderServiceTest`(E2E는 화면 비활성화로 간접 확인, API 400 자체는 단위테스트로 커버)
- [x] 이미 `DELIVERED`인 주문을 `SHIPPING`으로(또는 그 반대로) 되돌리는 요청이 성공한다 — 순서 제약이 없다 — `OrderTest` + E2E 시나리오 6
- [x] `POST /admin/orders/{orderId}/cancel`이 `PAID` 주문에 대해 성공하고, 재고가 고객 취소와 동일하게 복원되며 주문이 `CANCELLED`로 전이된다 — `PaymentServiceTest` + E2E 시나리오 7(재고 100 복원 SQL로 직접 확인)
- [x] `POST /admin/orders/{orderId}/cancel`이 `PAID`가 아닌 주문에 대해 409(`ORDER_NOT_CANCELLABLE`)를 반환한다 — `PaymentServiceTest` + E2E 시나리오 8
- [x] 관리자가 아닌 로그인 사용자가 `/admin/orders/**` 엔드포인트를 호출하면 403(`FORBIDDEN`)이 반환된다 — E2E 시나리오 9
- [x] 고객용 `GET /orders/{orderId}` 응답에 `shippingStatus`/`courier`/`trackingNumber`가 포함되고, `mypage/orders/[orderId]` 화면에 배송상태 뱃지가 표시된다(값이 없으면 뱃지 미노출) — E2E 시나리오 4. **2026-09-09 수정**: E2E 중 발견한 사각지대(이미 배송상태가 설정된 주문을 취소해도 `shippingStatus`가 초기화되지 않아 `CANCELLED` 주문에도 마지막 배송상태 뱃지가 남던 문제)를 고쳤다 — `mypage/orders/[orderId]`의 뱃지 노출 조건에 `order.status === "PAID"`를 추가해, 취소된 주문에는 뱃지가 더 이상 뜨지 않는다(수정 후 실제 취소 주문으로 재확인 완료). 관리자 목록/상세는 취소 전 마지막 배송단계를 계속 보여주는 편이 운영에 유용해 그대로 뒀다
- [x] `mypage/orders/[orderId]`에 송장번호가 있으면 택배사명과 함께 표시된다 — E2E 시나리오 4
- [x] `/admin/orders` 화면에서 기본 필터가 `PAID`만 보여주고, "전체보기" 토글로 전체 상태 주문을 볼 수 있다 — E2E 시나리오 1·2
- [x] `/admin/orders/[orderId]`에서 택배사 드롭다운의 "기타" 선택 시 자유 텍스트 입력이 노출되고, 저장 시 그 값이 `courier`로 전송된다 — E2E 시나리오 5
- [x] `/admin/orders/[orderId]`에서 대상 주문이 `PAID`가 아니면 배송 관리 폼과 취소 버튼이 비활성화된다 — E2E 시나리오 7
- [ ] `V17` 마이그레이션 적용 후 기존 `PAID` 주문의 `shipping_status`가 `PREPARING`으로 백필돼 있다(로컬 DB에서 SQL로 직접 확인). **2026-09-09 기준 미검증** — E2E 준비 중 로컬 DB를 `docker compose down -v`로 초기화해 V17이 처음부터 전체 마이그레이션 체인의 일부로 실행됐고, 백필 대상이 될 "V17 이전에 이미 PAID였던 주문"이 로컬에 없어 백필 UPDATE가 대상 0건으로 통과함. 운영 배포 직후 한 번 더 확인 권장(`admin-product-management`의 `order_item.size` 항목과 동일한 성격)
