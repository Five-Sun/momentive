---
date: 2026-09-07
feature: order-shipping-management
spec: 2026-09-07-order-shipping-management.md
status: planned
---

# 주문 배송상태·송장 관리 플랜

## 개요

`docs/specs/2026-09-07-order-shipping-management.md`를 기반으로 한다. 이 spec은 `admin-product-management`(PR #18, `develop` 병합 완료)로 세운 관리자 인가 기반(`SecurityConfig`의 `/admin/**` → `hasRole("ADMIN")`, `JwtAuthenticationFilter`의 role 기반 authority) 위에 **관리자 최초의 전체 주문 조회/처리 화면**과 **주문별 배송상태·송장 관리**를 얹는다. 관리자 인가 기반은 이미 `feat/order-shipping-management` 브랜치에 실제로 존재하므로, 이 플랜은 그 전제를 다시 만드는 phase 없이 바로 도메인부터 시작한다.

Phase는 `admin-product-management` 플랜과 동일한 원칙 — **영향 범위가 넓은 것부터 아래에서 위로** — 으로 나눴다. 배송 도메인/스키마가 먼저 확정돼야 그 위의 관리자 API가 응답 계약을 확정할 수 있고, API가 있어야 관리자 화면과 고객 화면 확장을 붙일 수 있다.

- **Phase 1 (도메인/마이그레이션)** — `Order`에 배송 관련 불변식(PAID 상태에서만 배송정보 변경 가능, SHIPPING/DELIVERED 전환 시 택배사·송장번호 필수)을 도메인 메서드로 못박는다. 이후 모든 phase가 이 규칙에 의존한다.
- **Phase 2 (관리자 API + 고객 응답 확장)** — Phase 1의 도메인 위에 관리자 CRUD·취소 API를 올리고, 기존 고객 `GET /orders/{orderId}` 응답도 같은 필드로 확장한다. 여기까지 오면 화면 없이 curl만으로 배송상태 관리가 가능해진다.
- **Phase 3 (관리자 화면)** — Phase 2 API의 소비자. `/admin`은 `(shell)` 밖 데스크톱 레이아웃이라 기존 고객 화면과 독립적으로 만들 수 있다.
- **Phase 4 (고객 화면 확장)** — 이미 존재하는 `mypage/orders/[orderId]`에 배송 뱃지·송장번호만 얹는 좁은 범위라 마지막에 가볍게 처리한다.
- **Phase 5 (E2E 검증)** — 관리자 발송 처리 → 고객 배송 확인까지 한 유저 플로우로 통과하는지 확인.

### 과거 실패 이력에서 가져온 주의점 (`docs/backlog/`)

- `2026-09-04-admin-product-management-phase3-01`(backend): 트랜잭션 내에서 새로 만든 자식/응답 DTO를 flush 전에 조립해 `id`가 `null`로 나간 결함. 이번 플랜은 `Order` 자체에 새 자식 엔티티를 추가하지 않고 스칼라 필드만 갱신하므로 직접 해당하진 않지만, `AdminOrderService.updateShipping` 응답 조립 시점(저장 후인지)을 동일한 기준으로 점검한다.
- `2026-08-29-cart-order-payment-phase6-01`(frontend): 공통 레이아웃의 `fixed` 하단 네비가 페이지 자체 CTA를 가려 클릭 불가. `/admin` 레이아웃(`frontend/src/app/admin/layout.tsx`)은 애초에 고정 하단 UI를 두지 않는다는 설계 원칙이 이미 주석으로 명시돼 있어(`docs/backlog/2026-08-29-cart-order-payment-phase6-01.md` 재발 방지 반영 결과) Phase 3에서 이 문제가 재발할 여지는 낮지만, Phase 3 수동 검증에서 저장·취소 버튼이 실제로 클릭되는지 다시 확인한다.
- `2026-08-30-cart-order-payment-phase4-01`(infra): 이 플랜은 외부 PG/서드파티 연동이 없어(택배사 API 미연동, spec Out of Scope) 해당 유형의 리스크는 없다.
- `2026-08-31-product-review-phase4-02`(test): 같은 텍스트가 여러 위치에 중복 렌더링될 때 `getByText().first()`가 엉뚱한 요소를 잡는 문제. Phase 5 e2e 셀렉터(관리자 주문 목록의 반복 행 등)는 컨테이너를 먼저 스코프한다.

## Phase 1: 배송 도메인 모델 및 마이그레이션

`Order`가 배송상태·택배사·송장번호를 갖고, 상태 전이 규칙(불변식)이 도메인 메서드로 강제된다. 이 phase가 끝나면 이 규칙을 검증하는 도메인 유닛 테스트가 통과하고, 로컬 DB에 `V17`이 적용되어 기존 `PAID` 주문의 `shipping_status`가 `PREPARING`으로 백필돼 있다.

- [ ] `order/domain/ShippingStatus` enum(`PREPARING`, `SHIPPING`, `DELIVERED`) 추가
- [ ] `Order`(`backend/src/main/java/com/momentive/backend/order/domain/Order.java`)에 `shippingStatus`(`@Enumerated(EnumType.STRING)`, nullable), `courier`(nullable `String`), `trackingNumber`(nullable `String`) 필드를 추가한다. `markAsPaid(String tossPaymentKey)`가 `shippingStatus = ShippingStatus.PREPARING`도 함께 설정하도록 확장한다
- [ ] `Order.updateShipping(ShippingStatus newStatus, String courier, String trackingNumber)` 도메인 메서드를 추가한다 — ① 현재 `status`가 `PAID`가 아니면 `CustomException(ErrorCode.ORDER_SHIPPING_NOT_APPLICABLE)`, ② `newStatus`가 `SHIPPING` 또는 `DELIVERED`인데 `courier`·`trackingNumber` 중 하나라도 비어있으면(blank) `CustomException(ErrorCode.SHIPPING_INFO_REQUIRED)`, ③ `newStatus`가 `PREPARING`이면 `courier`/`trackingNumber` 인자를 무시하고 기존 값을 그대로 유지(재입력 없이 다시 SHIPPING으로 돌아갈 수 있게), ④ 현재 `shippingStatus`와 무관하게 3개 상태 중 아무거나로 전환 허용(순서 제약 없음)
- [ ] `ErrorCode`(`backend/src/main/java/com/momentive/backend/common/exception/ErrorCode.java`)에 `SHIPPING_INFO_REQUIRED`(400, "배송중/배송완료로 변경하려면 택배사와 송장번호를 입력해야 합니다."), `ORDER_SHIPPING_NOT_APPLICABLE`(400, "결제 완료된 주문만 배송상태를 관리할 수 있습니다.") 추가
- [ ] `V17__add_shipping_fields_to_orders.sql` 작성 — `orders` 테이블에 `shipping_status VARCHAR(20) NULL`, `courier VARCHAR(50) NULL`, `tracking_number VARCHAR(100) NULL` 컬럼 추가 후 `UPDATE orders SET shipping_status = 'PREPARING' WHERE status = 'PAID'`로 백필
- [ ] `OrderTest`(신규, `backend/src/test/java/com/momentive/backend/order/domain/OrderTest.java`) 도메인 유닛 테스트 추가 — `markAsPaid` 호출 시 `shippingStatus`가 `PREPARING`이 되는지, `updateShipping`이 (a) `PAID`가 아닌 주문에서 `ORDER_SHIPPING_NOT_APPLICABLE`을 던지는지, (b) `SHIPPING`/`DELIVERED`로 정보 없이 전환 시 `SHIPPING_INFO_REQUIRED`를 던지는지, (c) `DELIVERED`에서 `SHIPPING`으로(또는 그 반대로) 순서 제약 없이 전환되는지, (d) `PREPARING`으로 되돌릴 때 기존 `courier`/`trackingNumber`가 유지되는지 검증
- [ ] 검증 — `./gradlew build`, `./gradlew test` 통과
- [ ] 검증(수동, 로컬 DB) — `V17` 적용 후 마이그레이션 전 이미 `PAID`였던 주문의 `shipping_status`가 `'PREPARING'`으로 백필됐는지 SQL로 직접 확인한다(spec AC 마지막 항목)

## Phase 2: 관리자 주문 API + 고객 응답 확장

관리자가 화면 없이 curl만으로 주문 목록 조회·상세 조회·배송상태 변경·취소를 수행할 수 있고, 고객용 주문상세 API도 배송정보를 함께 내려준다.

- [ ] `OrderRepository`(`backend/src/main/java/com/momentive/backend/order/repository/OrderRepository.java`)에 `findForAdmin(Collection<OrderStatus> statuses, Pageable pageable)` 쿼리를 추가한다 — `ProductRepository.findForAdmin`과 동일한 패턴(`@Query` + `status IN :statuses`), 목록 응답에 주문자 정보가 필요하므로 `JOIN FETCH o.user`로 N+1을 피하고 `createdAt DESC`로 정렬한다
- [ ] `order/dto/admin` 패키지에 관리자 주문 DTO 4종을 추가한다 — `AdminOrderSummaryResponse`(orderId, 주문자 email/nickname, totalAmount, status, shippingStatus(nullable), createdAt), `AdminOrderListResponse`(`AdminProductListResponse`와 동일한 content/page/size/totalElements/totalPages 구조), `AdminOrderDetailResponse`(주문자 email/nickname, items, address, itemsSubtotal/shippingFee/discountAmount/couponName/totalAmount, shippingStatus/courier/trackingNumber, createdAt), `AdminShippingUpdateRequest`(shippingStatus `@NotNull`, courier/trackingNumber는 nullable `String` — 조건부 필수 규칙은 Bean Validation이 아니라 Phase 1의 `Order.updateShipping`이 처리하므로 DTO에는 형식 검증만 둔다)
- [ ] `AdminOrderService`(신규, `backend/src/main/java/com/momentive/backend/order/service/AdminOrderService.java`)를 추가한다 — `getOrders(page, size, statuses)`(statuses가 비어있으면 기본값 `[OrderStatus.PAID]`), `getOrder(orderId)`(없으면 `ORDER_NOT_FOUND`), `updateShipping(orderId, request)`(주문 조회 → `Order.updateShipping` 위임 → 갱신된 `AdminOrderDetailResponse` 반환)
- [ ] `OrderPaymentTransactionSupport`(`backend/src/main/java/com/momentive/backend/payment/service/OrderPaymentTransactionSupport.java`)를 리팩터한다 — 기존 `cancelOrder(userId, orderId)`의 취소 처리·재고복원·쿠폰복원 로직을 `private Order doCancelOrder(Order order)`로 추출하고, 소유자 검증 없이 `orderRepository.findById`로만 주문을 조회해 `doCancelOrder`를 호출하는 `cancelOrderAsAdmin(Long orderId)` 패키지 메서드를 추가한다(`isCancellable()` 기준은 동일하게 적용, 대상이 아니면 `ORDER_NOT_CANCELLABLE`)
- [ ] `PaymentService`(`backend/src/main/java/com/momentive/backend/payment/service/PaymentService.java`)에 `cancelOrderAsAdmin(Long orderId)`를 추가한다 — `transactionSupport.cancelOrderAsAdmin`에 위임 후 `OrderStatusResponse.from`으로 반환(고객 취소 API와 동일 응답 타입)
- [ ] `AdminOrderController`(신규, `backend/src/main/java/com/momentive/backend/order/controller/AdminOrderController.java`)를 추가한다 — `GET /admin/orders`(page 기본 0·size 기본 20·status는 콤마 구분 복수 지정 가능, 기본값 `PAID`. `AdminProductController.getProducts`와 동일한 컨벤션), `GET /admin/orders/{orderId}`, `PATCH /admin/orders/{orderId}/shipping`, `POST /admin/orders/{orderId}/cancel`(`PaymentService.cancelOrderAsAdmin` 위임 — `OrderController`가 `OrderService`와 `PaymentService`를 함께 주입받는 것과 동일하게 `AdminOrderService`와 `PaymentService`를 함께 주입). 모든 메서드에 `@Operation`, DTO 필드에 `@Schema`, 클래스에 `@SecurityRequirement` 부여
- [ ] 고객 `OrderResponse`(`backend/src/main/java/com/momentive/backend/order/dto/OrderResponse.java`)에 `shippingStatus`/`courier`/`trackingNumber`(모두 nullable) 필드를 추가하고 `OrderResponse.from`이 `Order`에서 그대로 매핑하도록 갱신한다
- [ ] `AdminOrderServiceTest`(신규)를 추가한다 — 목록: 기본 호출 시 `PAID`만 반환, `status`에 4개 상태 모두 지정 시 전체 반환 및 페이지네이션 정상 동작. 상세: 정상 조회 + 존재하지 않는 orderId 404. 배송상태 변경: `SHIPPING`/`DELIVERED`를 정보 없이 요청 시 400(각각 `SHIPPING_INFO_REQUIRED`)이고 상태가 바뀌지 않음, `PAID`가 아닌 주문에 요청 시 400(`ORDER_SHIPPING_NOT_APPLICABLE`), `DELIVERED`→`SHIPPING`(역방향) 전환 성공. 취소: `PAID` 주문 취소 성공 시 재고가 고객 취소와 동일하게 복원되고 `CANCELLED` 전이, `PAID`가 아닌 주문 취소 시 409(`ORDER_NOT_CANCELLABLE`)
- [ ] `PaymentServiceTest`(`backend/src/test/java/com/momentive/backend/payment/PaymentServiceTest.java`)에 `cancelOrderAsAdmin` 회귀 케이스를 추가한다 — 주문 소유자가 아닌 관리자 컨텍스트(소유자 검증을 거치지 않는 호출 경로)에서도 취소가 성공하는지 확인
- [ ] 검증 — `./gradlew build`, `./gradlew test` 통과
- [ ] 검증(수동, curl) — 관리자 토큰으로 목록(기본 필터·전체보기)/상세/`PATCH .../shipping`(성공·실패 케이스)/`POST .../cancel` 흐름을 확인하고, 일반 회원 토큰으로 `/admin/orders`를 호출해 403(`ErrorResponse`의 `FORBIDDEN`)이 나는지 확인한다(`/admin/**`는 이미 `SecurityConfig`에서 보호되므로 신규 컨트롤러 테스트 대신 `docs/plans/2026-09-04-admin-product-management.md` Phase 3과 동일하게 수동 확인으로 처리)

## Phase 3: 관리자 화면

관리자가 브라우저에서 `/admin/orders` 목록을 보고 특정 주문의 배송상태·송장을 관리하거나 취소할 수 있다.

- [ ] `frontend/src/lib/api/adminOrders.ts`(신규) — `AdminOrderSummary`/`AdminOrderListResponse`/`AdminOrderDetail`/`AdminShippingUpdateRequest` 타입과 `getAdminOrders`/`getAdminOrder`/`updateAdminOrderShipping`/`cancelAdminOrder` 함수. 반드시 공통 `apiFetch`(`src/lib/api/client.ts`)를 거쳐 호출하고 `ApiError`를 그대로 던진다
- [ ] `frontend/src/lib/api/orders.ts`의 `OrderResponse` 타입에 `shippingStatus`/`courier`/`trackingNumber`(nullable) 필드를 추가한다(Phase 4에서 사용)
- [ ] `frontend/src/lib/shippingStatus.ts`(신규) — `SHIPPING_STATUS_LABEL`(PREPARING/SHIPPING/DELIVERED → "배송준비중"/"배송중"/"배송완료"), `SHIPPING_STATUS_TONE`(`Badge` tone 매핑). 관리자·고객 화면이 공용으로 import한다(`src/lib/categories.ts`와 동일한 위치 컨벤션)
- [ ] `frontend/src/app/admin/orders/courierOptions.ts`(신규) — 택배사 목록(CJ대한통운/우체국택배/한진택배/로젠택배/롯데택배/기타)
- [ ] `frontend/src/app/admin/orders/page.tsx`(신규) — 주문 목록 테이블(주문ID/주문자/총액/주문상태/배송상태/생성일시), 상단 상태 필터(기본 `PAID`만 체크) + "전체보기" 토글, 페이지네이션(`frontend/src/app/admin/page.tsx`의 로딩/에러/빈 상태 유니온 패턴 재사용)
- [ ] `frontend/src/app/admin/orders/AdminOrderTable.tsx`(신규) — `AdminProductTable.tsx`와 동일하게 `/admin` 안의 로컬 표 컴포넌트로 둔다
- [ ] `frontend/src/app/admin/orders/[orderId]/page.tsx`(신규) — 주문 정보(주문자/배송지/상품목록/금액), 배송 관리 폼(React Hook Form + Zod: shippingStatus select, courier select+"기타" 선택 시 자유 텍스트 노출, trackingNumber `TextField`, 저장 버튼), 서버 에러(`SHIPPING_INFO_REQUIRED`/`ORDER_SHIPPING_NOT_APPLICABLE`)를 폼 에러로 인라인 표시, 대상 주문이 `PAID`가 아니면 폼 전체 disabled + 안내 문구, 취소 버튼은 `PAID`일 때만 노출/활성화되고 `window.confirm` 다이얼로그 후 `cancelAdminOrder` 호출
- [ ] 검증 — `npm run build`, `npm run lint`(`frontend/`) 통과
- [ ] 검증(수동, 브라우저) — 목록 기본 필터·전체보기 토글·페이지네이션 동작 확인, 상세에서 정보 없이 `SHIPPING` 저장 시 에러 메시지 노출 확인, "기타" 택배사 자유 텍스트 입력 저장 후 그 값이 `courier`로 반영되는지 확인, `PAID`가 아닌 주문에서 폼·취소버튼이 비활성화되는지 확인, 저장·취소 버튼이 실제로 클릭되는지 확인(`/admin` 레이아웃에 고정 하단 UI가 없어 `docs/backlog/2026-08-29-cart-order-payment-phase6-01.md` 유형 문제 재발 가능성은 낮지만 재확인)

## Phase 4: 고객 화면 확장

고객이 `마이페이지 > 주문내역 > 주문상세`에서 배송상태와 송장번호를 확인할 수 있다.

- [ ] `frontend/src/app/(shell)/mypage/orders/[orderId]/page.tsx`를 확장한다 — `order.shippingStatus`가 있을 때만 배송상태 `Badge`(Phase 3의 `shippingStatus.ts` 라벨/톤 재사용)를 노출하고, `order.trackingNumber`가 있으면 `"${courier} ${trackingNumber}"` 형식으로 택배사명과 함께 표시한다. 값이 없는 주문(`PENDING`/`FAILED`/`CANCELLED`)에는 뱃지 자체를 렌더링하지 않는다
- [ ] 검증 — `npm run build`, `npm run lint`(`frontend/`) 통과
- [ ] 검증(수동, 브라우저) — `PAID` 이후 주문 상세에서 배송상태 뱃지가 보이는지, `PENDING`/`FAILED`/`CANCELLED` 주문에는 뱃지가 없는지, Phase 3에서 관리자가 입력한 송장번호가 택배사명과 함께 표시되는지 확인

## Phase 5: E2E 검증

관리자 발송 처리부터 고객 배송 확인까지 한 유저 플로우로 이어 실행해 통과를 확인한다.

- [ ] `e2e-tester` 에이전트가 `.claude/rules/e2e-format.md` 규격으로 `docs/e2e/2026-09-07-order-shipping-management.md`를 생성한다 — 시나리오 축은 (1) 관리자 로그인 후 `/admin/orders` 진입(기본 `PAID` 필터 확인), (2) 주문 상세 진입 후 택배사·송장번호 없이 "배송중"으로 저장 시 실패 확인, (3) "기타" 택배사 자유입력 + 송장번호 입력 후 "배송중" 저장 성공, (4) 고객 계정으로 전환해 `mypage/orders/[orderId]`에서 배송 뱃지·송장번호 확인, (5) 관리자 계정으로 돌아가 해당 주문 취소 처리(단, 배송정보 입력을 마친 주문은 이미 배송중이므로 재고복원·취소 시나리오는 별도 `PAID` 주문으로 진행하거나 순서를 조정 — 실제 작성 시점에 상태 충돌 여부를 e2e-format.md 기준으로 판단)까지 단일 탭 흐름으로 설계
- [ ] 셀렉터는 role/text 로케이터를 우선하고, 관리자 주문 목록처럼 반복 렌더링되는 행은 컨테이너를 먼저 스코프한 뒤 텍스트를 찾는다(`docs/backlog/2026-08-31-product-review-phase4-02.md` 재발 방지)
- [ ] 검증(수동, 브라우저 자동화) — dev-browser로 전체 스크립트를 1회 실행해 모든 시나리오가 PASS하는지 확인한다. 실패 시 `.claude/rules/backlog-format.md` 규격으로 `docs/backlog/2026-09-07-order-shipping-management-phase5-01.md`를 남긴다
