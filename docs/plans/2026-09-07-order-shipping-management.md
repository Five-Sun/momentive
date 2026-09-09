---
date: 2026-09-07
feature: order-shipping-management
spec: 2026-09-07-order-shipping-management.md
status: done
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

- [x] `order/domain/ShippingStatus` enum(`PREPARING`, `SHIPPING`, `DELIVERED`) 추가
- [x] `Order`(`backend/src/main/java/com/momentive/backend/order/domain/Order.java`)에 `shippingStatus`(`@Enumerated(EnumType.STRING)`, nullable), `courier`(nullable `String`), `trackingNumber`(nullable `String`) 필드를 추가한다. `markAsPaid(String tossPaymentKey)`가 `shippingStatus = ShippingStatus.PREPARING`도 함께 설정하도록 확장한다
- [x] `Order.updateShipping(ShippingStatus newStatus, String courier, String trackingNumber)` 도메인 메서드를 추가한다 — ① 현재 `status`가 `PAID`가 아니면 `CustomException(ErrorCode.ORDER_SHIPPING_NOT_APPLICABLE)`, ② `newStatus`가 `SHIPPING` 또는 `DELIVERED`인데 `courier`·`trackingNumber` 중 하나라도 비어있으면(blank) `CustomException(ErrorCode.SHIPPING_INFO_REQUIRED)`, ③ `newStatus`가 `PREPARING`이면 `courier`/`trackingNumber` 인자를 무시하고 기존 값을 그대로 유지(재입력 없이 다시 SHIPPING으로 돌아갈 수 있게), ④ 현재 `shippingStatus`와 무관하게 3개 상태 중 아무거나로 전환 허용(순서 제약 없음)
- [x] `ErrorCode`(`backend/src/main/java/com/momentive/backend/common/exception/ErrorCode.java`)에 `SHIPPING_INFO_REQUIRED`(400, "배송중/배송완료로 변경하려면 택배사와 송장번호를 입력해야 합니다."), `ORDER_SHIPPING_NOT_APPLICABLE`(400, "결제 완료된 주문만 배송상태를 관리할 수 있습니다.") 추가
- [x] `V17__add_shipping_fields_to_orders.sql` 작성 — `orders` 테이블에 `shipping_status VARCHAR(20) NULL`, `courier VARCHAR(50) NULL`, `tracking_number VARCHAR(100) NULL` 컬럼 추가 후 `UPDATE orders SET shipping_status = 'PREPARING' WHERE status = 'PAID'`로 백필
- [x] `OrderTest`(신규, `backend/src/test/java/com/momentive/backend/order/domain/OrderTest.java`) 도메인 유닛 테스트 추가 — `markAsPaid` 호출 시 `shippingStatus`가 `PREPARING`이 되는지, `updateShipping`이 (a) `PAID`가 아닌 주문에서 `ORDER_SHIPPING_NOT_APPLICABLE`을 던지는지, (b) `SHIPPING`/`DELIVERED`로 정보 없이 전환 시 `SHIPPING_INFO_REQUIRED`를 던지는지, (c) `DELIVERED`에서 `SHIPPING`으로(또는 그 반대로) 순서 제약 없이 전환되는지, (d) `PREPARING`으로 되돌릴 때 기존 `courier`/`trackingNumber`가 유지되는지 검증
- [x] 검증 — `./gradlew build`, `./gradlew test` 통과
- [ ] 검증(수동, 로컬 DB) — `V17` 적용 후 마이그레이션 전 이미 `PAID`였던 주문의 `shipping_status`가 `'PREPARING'`으로 백필됐는지 SQL로 직접 확인한다(spec AC 마지막 항목). **2026-09-09 기준 미검증** — Phase 5 E2E 준비 중 로컬 DB가 `docker compose down -v`로 초기화되며 V17이 처음부터 전체 마이그레이션 체인의 일부로 실행돼, 백필 대상이 될 "V17 이전에 이미 PAID였던 주문"이 로컬에 존재하지 않게 됐다(백필 UPDATE는 대상 0건으로 통과). 실제 운영 배포 시 `admin-product-management`의 `order_item.size` 항목과 동일하게 배포 직후 한 번 더 확인 권장

## Phase 2: 관리자 주문 API + 고객 응답 확장

관리자가 화면 없이 curl만으로 주문 목록 조회·상세 조회·배송상태 변경·취소를 수행할 수 있고, 고객용 주문상세 API도 배송정보를 함께 내려준다.

> 참고(2026-09-09): 이 phase의 `./gradlew build`/`test` 검증은 이번 세션 환경에서 `backend-reviewer` 서브에이전트에게 Bash 도구가 지급되지 않는 구조적 문제로 reviewer가 직접 실행하지 못했다. 사용자가 최상위 세션에서 직접 `./gradlew build test --rerun`을 실행해 BUILD SUCCESSFUL, 전체 테스트 실패/에러 0건을 확인하고 체크했다.

- [x] `OrderRepository`(`backend/src/main/java/com/momentive/backend/order/repository/OrderRepository.java`)에 `findForAdmin(Collection<OrderStatus> statuses, Pageable pageable)` 쿼리를 추가한다 — `ProductRepository.findForAdmin`과 동일한 패턴(`@Query` + `status IN :statuses`), 목록 응답에 주문자 정보가 필요하므로 `JOIN FETCH o.user`로 N+1을 피하고 `createdAt DESC`로 정렬한다
- [x] `order/dto/admin` 패키지에 관리자 주문 DTO 4종을 추가한다 — `AdminOrderSummaryResponse`(orderId, 주문자 email/nickname, totalAmount, status, shippingStatus(nullable), createdAt), `AdminOrderListResponse`(`AdminProductListResponse`와 동일한 content/page/size/totalElements/totalPages 구조), `AdminOrderDetailResponse`(주문자 email/nickname, items, address, itemsSubtotal/shippingFee/discountAmount/couponName/totalAmount, shippingStatus/courier/trackingNumber, createdAt), `AdminShippingUpdateRequest`(shippingStatus `@NotNull`, courier/trackingNumber는 nullable `String` — 조건부 필수 규칙은 Bean Validation이 아니라 Phase 1의 `Order.updateShipping`이 처리하므로 DTO에는 형식 검증만 둔다)
- [x] `AdminOrderService`(신규, `backend/src/main/java/com/momentive/backend/order/service/AdminOrderService.java`)를 추가한다 — `getOrders(page, size, statuses)`(statuses가 비어있으면 기본값 `[OrderStatus.PAID]`), `getOrder(orderId)`(없으면 `ORDER_NOT_FOUND`), `updateShipping(orderId, request)`(주문 조회 → `Order.updateShipping` 위임 → 갱신된 `AdminOrderDetailResponse` 반환)
- [x] `OrderPaymentTransactionSupport`(`backend/src/main/java/com/momentive/backend/payment/service/OrderPaymentTransactionSupport.java`)를 리팩터한다 — 기존 `cancelOrder(userId, orderId)`의 취소 처리·재고복원·쿠폰복원 로직을 `private Order doCancelOrder(Order order)`로 추출하고, 소유자 검증 없이 `orderRepository.findById`로만 주문을 조회해 `doCancelOrder`를 호출하는 `cancelOrderAsAdmin(Long orderId)` 패키지 메서드를 추가한다(`isCancellable()` 기준은 동일하게 적용, 대상이 아니면 `ORDER_NOT_CANCELLABLE`)
- [x] `PaymentService`(`backend/src/main/java/com/momentive/backend/payment/service/PaymentService.java`)에 `cancelOrderAsAdmin(Long orderId)`를 추가한다 — `transactionSupport.cancelOrderAsAdmin`에 위임 후 `OrderStatusResponse.from`으로 반환(고객 취소 API와 동일 응답 타입)
- [x] `AdminOrderController`(신규, `backend/src/main/java/com/momentive/backend/order/controller/AdminOrderController.java`)를 추가한다 — `GET /admin/orders`(page 기본 0·size 기본 20·status는 콤마 구분 복수 지정 가능, 기본값 `PAID`. `AdminProductController.getProducts`와 동일한 컨벤션), `GET /admin/orders/{orderId}`, `PATCH /admin/orders/{orderId}/shipping`, `POST /admin/orders/{orderId}/cancel`(`PaymentService.cancelOrderAsAdmin` 위임 — `OrderController`가 `OrderService`와 `PaymentService`를 함께 주입받는 것과 동일하게 `AdminOrderService`와 `PaymentService`를 함께 주입). 모든 메서드에 `@Operation`, DTO 필드에 `@Schema`, 클래스에 `@SecurityRequirement` 부여
- [x] 고객 `OrderResponse`(`backend/src/main/java/com/momentive/backend/order/dto/OrderResponse.java`)에 `shippingStatus`/`courier`/`trackingNumber`(모두 nullable) 필드를 추가하고 `OrderResponse.from`이 `Order`에서 그대로 매핑하도록 갱신한다
- [x] `AdminOrderServiceTest`(신규)를 추가한다 — 목록: 기본 호출 시 `PAID`만 반환, `status`에 4개 상태 모두 지정 시 전체 반환 및 페이지네이션 정상 동작. 상세: 정상 조회 + 존재하지 않는 orderId 404. 배송상태 변경: `SHIPPING`/`DELIVERED`를 정보 없이 요청 시 400(각각 `SHIPPING_INFO_REQUIRED`)이고 상태가 바뀌지 않음, `PAID`가 아닌 주문에 요청 시 400(`ORDER_SHIPPING_NOT_APPLICABLE`), `DELIVERED`→`SHIPPING`(역방향) 전환 성공. 취소: `PAID` 주문 취소 성공 시 재고가 고객 취소와 동일하게 복원되고 `CANCELLED` 전이, `PAID`가 아닌 주문 취소 시 409(`ORDER_NOT_CANCELLABLE`)
- [x] `PaymentServiceTest`(`backend/src/test/java/com/momentive/backend/payment/PaymentServiceTest.java`)에 `cancelOrderAsAdmin` 회귀 케이스를 추가한다 — 주문 소유자가 아닌 관리자 컨텍스트(소유자 검증을 거치지 않는 호출 경로)에서도 취소가 성공하는지 확인
- [x] 검증 — `./gradlew build`, `./gradlew test` 통과
- [ ] 검증(수동, curl) — 관리자 토큰으로 목록(기본 필터·전체보기)/상세/`PATCH .../shipping`(성공·실패 케이스)/`POST .../cancel` 흐름을 확인하고, 일반 회원 토큰으로 `/admin/orders`를 호출해 403(`ErrorResponse`의 `FORBIDDEN`)이 나는지 확인한다(`/admin/**`는 이미 `SecurityConfig`에서 보호되므로 신규 컨트롤러 테스트 대신 `docs/plans/2026-09-04-admin-product-management.md` Phase 3과 동일하게 수동 확인으로 처리)

## Phase 3: 관리자 화면

관리자가 브라우저에서 `/admin/orders` 목록을 보고 특정 주문의 배송상태·송장을 관리하거나 취소할 수 있다.

- [x] `frontend/src/lib/api/adminOrders.ts`(신규) — `AdminOrderSummary`/`AdminOrderListResponse`/`AdminOrderDetail`/`AdminShippingUpdateRequest` 타입과 `getAdminOrders`/`getAdminOrder`/`updateAdminOrderShipping`/`cancelAdminOrder` 함수. 반드시 공통 `apiFetch`(`src/lib/api/client.ts`)를 거쳐 호출하고 `ApiError`를 그대로 던진다
- [x] `frontend/src/lib/api/orders.ts`의 `OrderResponse` 타입에 `shippingStatus`/`courier`/`trackingNumber`(nullable) 필드를 추가한다(Phase 4에서 사용)
- [x] `frontend/src/lib/shippingStatus.ts`(신규) — `SHIPPING_STATUS_LABEL`(PREPARING/SHIPPING/DELIVERED → "배송준비중"/"배송중"/"배송완료"), `SHIPPING_STATUS_TONE`(`Badge` tone 매핑). 관리자·고객 화면이 공용으로 import한다(`src/lib/categories.ts`와 동일한 위치 컨벤션)
- [x] `frontend/src/app/admin/orders/courierOptions.ts`(신규) — 택배사 목록(CJ대한통운/우체국택배/한진택배/로젠택배/롯데택배/기타)
- [x] `frontend/src/app/admin/orders/page.tsx`(신규) — 주문 목록 테이블(주문ID/주문자/총액/주문상태/배송상태/생성일시), 상단 상태 필터(기본 `PAID`만 체크) + "전체보기" 토글, 페이지네이션(`frontend/src/app/admin/page.tsx`의 로딩/에러/빈 상태 유니온 패턴 재사용)
- [x] `frontend/src/app/admin/orders/AdminOrderTable.tsx`(신규) — `AdminProductTable.tsx`와 동일하게 `/admin` 안의 로컬 표 컴포넌트로 둔다
- [x] `frontend/src/app/admin/orders/[orderId]/page.tsx`(신규) — 주문 정보(주문자/배송지/상품목록/금액), 배송 관리 폼(React Hook Form + Zod: shippingStatus select, courier select+"기타" 선택 시 자유 텍스트 노출, trackingNumber `TextField`, 저장 버튼), 서버 에러(`SHIPPING_INFO_REQUIRED`/`ORDER_SHIPPING_NOT_APPLICABLE`)를 폼 에러로 인라인 표시, 대상 주문이 `PAID`가 아니면 폼 전체 disabled + 안내 문구, 취소 버튼은 `PAID`일 때만 노출/활성화되고 `window.confirm` 다이얼로그 후 `cancelAdminOrder` 호출
- [x] 검증 — `npm run build`, `npm run lint`(`frontend/`) 통과 (2026-09-09, 사용자가 직접 실행 확인: `/admin/orders`·`/admin/orders/[orderId]` 라우트 정상 생성, lint 에러 0건)
- [x] 검증(수동, 브라우저) — 2026-09-09, `docs/e2e/2026-09-07-order-shipping-management.md`로 확인. 기본 필터(PAID)·전체보기 토글, 정보 없이 "배송중" 저장 시 인라인 에러, "기타" 택배사 자유입력 저장 반영, `PAID`가 아닌 주문(취소 후)에서 폼·취소버튼 비활성화, 저장·취소 버튼 클릭 정상 동작을 모두 확인. **페이지네이션은 미확인** — 로컬 시드 데이터가 주문 1건뿐이라 여러 페이지 상태를 재현하지 못했다(컨트롤 자체는 `admin/page.tsx`와 동일한 기존 패턴을 그대로 재사용해 리스크는 낮다고 판단)

## Phase 4: 고객 화면 확장

고객이 `마이페이지 > 주문내역 > 주문상세`에서 배송상태와 송장번호를 확인할 수 있다.

- [x] `frontend/src/app/(shell)/mypage/orders/[orderId]/page.tsx`를 확장한다 — `order.shippingStatus`가 있을 때만 배송상태 `Badge`(Phase 3의 `shippingStatus.ts` 라벨/톤 재사용)를 노출하고, `order.trackingNumber`가 있으면 `"${courier} ${trackingNumber}"` 형식으로 택배사명과 함께 표시한다. 값이 없는 주문(`PENDING`/`FAILED`/`CANCELLED`)에는 뱃지 자체를 렌더링하지 않는다
- [x] 검증 — `npm run build`, `npm run lint`(`frontend/`) 통과 (2026-09-09, 사용자가 직접 실행 확인: `/admin/orders`·`/admin/orders/[orderId]` 라우트 정상 생성, lint 에러 0건)
- [x] 검증(수동, 브라우저) — 2026-09-09, `docs/e2e/2026-09-07-order-shipping-management.md` 시나리오 4로 확인. `PAID`(배송중) 주문 상세에 배송상태 뱃지와 "CJ대한통운 {송장번호}"가 함께 표시됨. **추가 수정(2026-09-09)**: 취소된 주문에도 마지막 배송상태 뱃지가 남는 사각지대를 발견해 그 자리에서 수정 — `mypage/orders/[orderId]`의 뱃지 노출 조건에 `order.status === "PAID"`를 추가하고, 실제 취소 주문(#1)으로 뱃지가 사라지는지 dev-browser로 재확인함(`npm run build`/`lint` 재통과 확인)

## Phase 5: E2E 검증

관리자 발송 처리부터 고객 배송 확인까지 한 유저 플로우로 이어 실행해 통과를 확인한다.

- [x] `docs/e2e/2026-09-07-order-shipping-management.md`를 `.claude/rules/e2e-format.md` 규격으로 작성했다(이번 세션 환경에서 `e2e-tester` 서브에이전트에게 Bash가 지급되지 않아, 사용자가 직접 최상위 세션에서 작성·실행). 시나리오 9개: (1) 관리자 로그인+기본 PAID 필터, (2) 주문 생성(PENDING)→SQL로 PAID 승격→필터 차이 확인, (3) 정보 없이 "배송중" 저장 실패, (4) 정보 입력 후 저장 성공+고객 화면 반영, (5) "기타" 택배사 자유입력+배송완료, (6) 배송완료→배송중 되돌리기, (7) 관리자 취소+폼 비활성화, (8) 취소된 주문 재취소 409(API), (9) 비관리자 403(API)
- [x] 셀렉터는 role/text 로케이터를 우선했고, 관리자 주문 목록 행은 `a[href="/admin/orders/{orderId}"]`의 조상 `tr`로 컨테이너를 먼저 스코프했다(`docs/backlog/2026-08-31-product-review-phase4-02.md` 재발 방지 반영)
- [x] 검증(수동, 브라우저 자동화) — 2026-09-09, dev-browser로 실행. **9개 시나리오 전부 PASS**(Toss 실결제 불가로 시나리오 2 중간에 SQL로 `PAID` 승격하는 수동 개입이 필요해 스크립트를 2단계로 나눠 실행 — 상세는 e2e 문서의 "실행 결과" 절 참고). 실패 없어 backlog 항목 없음
