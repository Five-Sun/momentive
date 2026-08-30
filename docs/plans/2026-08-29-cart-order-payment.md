---
date: 2026-08-29
feature: cart-order-payment
spec: 2026-08-29-cart-order-payment.md
status: in_progress
---

# 장바구니→주문→결제 (토스페이먼츠) 플랜

## 개요

`docs/specs/2026-08-29-cart-order-payment.md`를 기반으로 장바구니 부분/전체 선택 → 주문서 작성(배송지) → Toss 결제위젯 결제 → 주문내역 조회/취소 흐름을 도입한다.

Auth 플랜(`docs/plans/2026-08-27-auth.md`)과 마찬가지로 백엔드 API 계약을 먼저 확정한 뒤 프론트를 쌓는 순서를 따르되, 이번 도메인은 (1) 재고/주문/배송지 골격과 (2) Toss 결제 연동·취소·만료 스케줄러가 서로 다른 성격의 작업(전자는 트랜잭션/낙관적 락 중심, 후자는 외부 API 연동/비동기 상태 전이 중심)이라 백엔드를 2개 phase로 나눈다. 프론트도 장바구니 선택 UI(가벼움) → 체크아웃+결제위젯(외부 SDK 연동으로 무거움) → 마이페이지 주문내역(조회+취소, 상태 회귀 위주)로 3개 phase로 나눠 각 phase가 독립적으로 브라우저에서 확인 가능한 단위가 되게 한다.

- **Phase 1 (백엔드: 재고·주문·배송지 도메인 골격)**: `Product.stock`+`@Version`, `Address` CRUD, `Order`/`OrderItem`, `POST /orders`(재고 선점+낙관적 락 재시도+`PENDING` 생성), `GET /orders`, `GET /orders/{id}`. Toss 연동 없이 `PENDING` 생성까지 완결시켜 이후 phase가 이 위에 결제 상태 전이만 얹으면 되게 한다.
- **Phase 2 (백엔드: Toss 결제 연동 + 취소 + 만료 스케줄러)**: `PaymentGatewayClient` 추상화, `confirm`/`cancel` API, `PENDING` 만료 배치. Phase 1의 `PENDING` 주문을 입력으로 받아 상태 전이(`PAID`/`FAILED`/`CANCELLED`)를 완성한다.
- **Phase 3 (프론트: 장바구니 선택 UI)**: `/cart`에 체크박스+전체선택, 선택 기준 금액 재계산, "구매하기" 버튼 활성화 조건. 화면 단위로 가장 가볍고 독립적으로 검증 가능해 먼저 진행한다.
- **Phase 4 (프론트: 체크아웃 + Toss 결제위젯 + 완료/실패 화면)**: 배송지 입력/선택 폼, `POST /orders` 연동, Toss Payment Widget SDK, confirm 콜백, 성공/실패 화면. Phase 1/2 API와 Phase 3 장바구니 선택 결과를 소비한다.
- **Phase 5 (프론트: 마이페이지 주문내역 + 취소 + 회귀)**: `/mypage/orders` 목록/상세, 취소 버튼(상태별 배타 분기), 기존 상품/장바구니/위시리스트 회귀 확인.
- **Phase 6 (E2E 검증)**: Phase 5 통과 직후 `e2e-tester`가 spec 사용자 시나리오 기반 케이스를 도출·실행한다.

`docs/backlog/2026-08-26-app-redesign-phase2-01.md`(상태 분기 누락 사례)를 참고해, Phase 5에서 `PAID`/`FAILED`/`CANCELLED` 상태별 취소 버튼 노출 여부가 서로 배타적으로 조건화됐는지 명시적으로 짚는다.

## Phase 1: 백엔드 재고·주문·배송지 도메인 골격

이 phase가 끝나면 Toss 연동 없이도 curl/Postman으로 주문서 제출 → 재고 선점(차감) → `Order`(`PENDING`) 생성 → 목록/상세 조회까지 전체 흐름을 검증할 수 있는 상태가 된다.

- [x] `Product`에 `stock`(Integer, not null) 컬럼과 `@Version` 낙관적 락 필드 추가. 마이그레이션 시 기존 상품은 `soldOut=true`면 `stock=0`, 그 외 기본값 부여(Flyway/JPA DDL 방식은 기존 프로젝트 마이그레이션 관례를 따름).
- [x] `Address` 엔티티(`id`, `user_id` FK, `recipient`, `phone`tae, `zipcode`, `address1`, `address2`, `is_default`, `created_at`) 및 Repository. `@NoArgsConstructor(access = PROTECTED)` + 정적 팩토리.
- [x] `AddressService`/`AddressController`: `GET /addresses`, `POST /addresses`, `PATCH /addresses/{id}` — `isDefault=true` 설정 시 기존 기본배송지 자동 해제(같은 트랜잭션). `@CurrentUser`로 소유자 식별, 타 사용자 주소 접근은 `FORBIDDEN`.
- [x] `Order`(`id`, `user_id` FK, `status` enum `PENDING/PAID/FAILED/CANCELLED`, `total_amount`, `address_id` FK, `toss_payment_key` nullable, `created_at`, `updated_at`) 및 `OrderItem`(`id`, `order_id` FK, `product_id` FK, `quantity`, `size` nullable, `unit_price`) 엔티티 + Repository.
- [x] `OrderService.createOrder`: 요청 항목별 `Product` 조회(`PRODUCT_NOT_FOUND`), 재고 검증 후 차감을 `@Version` 낙관적 락 하에 for-loop 최대 2회 재시도, 2회 실패 시 `STOCK_CONFLICT`. 재고 부족 항목이 하나라도 있으면 `OUT_OF_STOCK`(부족 항목 목록 포함)으로 전체 거부하고 어떤 재고도 차감하지 않음(원자적). `addressId` 또는 신규 `address` 중 하나로 배송지 확정(신규면 `Address` 저장 후 연결, 기본배송지로 지정). `Order`/`OrderItem`을 `PENDING`으로 저장. `@Transactional` 경계는 Service.
- [x] `OrderController`: `POST /orders`(201), `GET /orders`(200, `itemsSummary` 포함 DTO), `GET /orders/{orderId}`(200, `ORDER_NOT_FOUND` 404, `FORBIDDEN` 403 — 타인 주문 접근 시).
- [x] 신규 `ErrorCode`: `OUT_OF_STOCK`(409), `PRODUCT_NOT_FOUND`(404), `STOCK_CONFLICT`, `ORDER_NOT_FOUND`(404), `FORBIDDEN`(403, 기존 재사용 가능 시 재사용) — `GlobalExceptionHandler` 매핑 확인.
- [x] Request/Response DTO는 Entity 직접 노출 없이 별도 클래스로 분리 (`OrderCreateRequest`, `OrderResponse`, `OrderSummaryResponse`, `AddressRequest`, `AddressResponse` 등).
- [x] `OrderService`/`AddressService` 단위/통합 테스트: 정상 주문 생성(재고 차감 확인), 재고 부족 시 `OUT_OF_STOCK`(재고 변화 없음 검증), 동시 주문 낙관적 락 충돌 시 하나만 성공·나머지 `OUT_OF_STOCK` 또는 `STOCK_CONFLICT`, 배송지 없이 신규 입력 시 `Address` 생성+기본배송지 지정, 타인 주문 상세 조회 시 `FORBIDDEN`.
- [x] 검증(자동): `./gradlew build`, `./gradlew test` 통과.

## Phase 2: 백엔드 Toss 결제 연동 + 취소 + 만료 스케줄러

이 phase가 끝나면 백엔드만으로 `PENDING` 주문에 대해 confirm 성공/실패, 사용자 취소, 방치 시 자동 만료까지 전체 상태 전이가 서비스 테스트로 검증된 상태가 된다.

- [x] `payment.client.PaymentGatewayClient` 인터페이스 정의(confirm 요청/응답 최소 계약). `TossPaymentGatewayClient` 구현체: Spring `RestClient`로 Toss confirm API 호출, 타임아웃 5초, 재시도 없음. 시크릿은 `application-{profile}.yml` + 환경변수로 주입(하드코딩 금지).
- [x] `PaymentService`(또는 `OrderService` 내 confirm 로직)는 `PaymentGatewayClient` 인터페이스에만 의존 — 테스트에서 fake/mock 구현체로 대체 가능하게.
- [x] `POST /orders/{orderId}/confirm`: `paymentKey`/`orderId`(Toss 측)/`amount` 검증(요청 온 금액과 `Order.total_amount` 일치 확인, 불일치 시 위변조 방지 목적의 실패 처리) 후 Toss confirm 호출. 성공 시 `Order.PAID` 전환 + `toss_payment_key` 기록. 실패/타임아웃 시 `Order.FAILED` 전환 + 재고 복원(트랜잭션/후속 처리로). 이미 `PENDING`이 아닌 주문에 대한 재호출은 `ORDER_NOT_PENDING`(409)으로 거부, confirm 재시도 자체를 허용하지 않음.
- [x] Toss 원본 에러코드를 1:1 매핑하지 않고 `PAYMENT_CONFIRM_FAILED` 등 단순화된 `ErrorCode`로 응답, 원본 코드는 로그/`ErrorResponse` 상세 필드에만 기록.
- [x] `POST /orders/{orderId}/cancel`: `PAID` 상태만 허용, 아니면 `ORDER_NOT_CANCELLABLE`(409). 취소 시 `Order.CANCELLED` 전환 + 재고 복원(재고 원복도 `@Version` 낙관적 락 대상이므로 동일한 재시도 정책 적용). 소유자 검증(`FORBIDDEN`), `ORDER_NOT_FOUND` 처리.
- [x] `@Scheduled` 배치: 일정 시간(spec에 구체 수치 없음 — 결제위젯 세션 통상 유효시간을 고려해 구현 시 상수로 정의하고 주석에 근거 명시) 지난 `PENDING` 주문을 주기적으로 `FAILED` 전환 + 재고 복원. 배치 주기 사이 공백을 보완하기 위해 `GET /orders/{orderId}` 조회 시점에도 만료 대상이면 lazy하게 `FAILED` 처리 후 응답(컨벤션 명시 사항).
- [x] `PaymentService`/`OrderService` 테스트: confirm 성공(`PAID` 전환+`toss_payment_key` 기록), confirm 실패(`FAILED` 전환+재고 복원), 이미 처리된 주문 재confirm 시 `ORDER_NOT_PENDING`, 취소 성공(재고 복원), `PAID`가 아닌 주문 취소 시도 시 `ORDER_NOT_CANCELLABLE`, 만료 스케줄러 대상 주문의 상태 전이 및 재고 복원 — `PaymentGatewayClient`는 fake 구현체로 대체.
- [x] 검증(자동): `./gradlew build`, `./gradlew test` 통과.

## Phase 3: 프론트 장바구니 선택 UI

이 phase가 끝나면 `/cart`에서 항목별 체크박스와 전체선택으로 결제 대상을 고를 수 있고, 선택 상태에 따라 금액 요약과 "구매하기" 버튼 활성화가 정확히 반영되는 상태가 된다.

- [x] `src/app/(shell)/cart/page.tsx`에 항목별 체크박스 + 상단 "전체선택" 체크박스 UI 추가. 체크 상태는 컴포넌트 로컬 상태(선택은 세션성 UI 상태이므로 `localStorage` 영속화 불필요, `cart.ts`의 `CartItem` 자체는 변경하지 않음).
- [x] 금액 요약(상품가 합계)을 선택된 항목만 반영하도록 계산 로직 수정. spec 범위(배송비 없음, 쿠폰은 placeholder 유지)에 맞춰 기존 `SHIPPING_FEE`/`FREE_SHIPPING_THRESHOLD` 기반 배송비 계산과 `ShippingProgress` 노출을 제거하고, 쿠폰 토글 UI는 유지하되 실제 결제 금액 계산에는 반영하지 않음(placeholder임을 코드 주석으로 명시).
- [x] "구매하기" 버튼: 선택된 항목이 1개 이상일 때만 활성화(`Button`의 `disabled` prop 활용), 클릭 시 `/checkout`으로 라우팅하며 선택된 항목 정보를 다음 phase가 소비할 수 있는 형태로 전달(예: 선택된 `key` 목록을 세션 저장소 또는 쿼리 파라미터로 — 구현 시 확정, `src/lib/storage/` 컨벤션 우선 검토).
- [x] 타입: 선택 상태 관리에 `any` 없이 명시 타입 사용.
- [x] 검증(자동): `npm run build`, `npm run lint` 통과.
- [x] 검증(수동, 브라우저): 개별 체크/해제 시 합계 갱신, 전체선택 토글 동작, 선택 0개일 때 버튼 비활성화 확인. (`docs/e2e/2026-08-29-cart-order-payment.md` 시나리오 1 PASS로 확인)

## Phase 4: 프론트 체크아웃 + Toss 결제위젯 + 완료/실패 화면

이 phase가 끝나면 브라우저에서 장바구니 선택 → 체크아웃(배송지 입력/선택) → `POST /orders` 호출 → Toss 결제위젯 노출 → confirm 결과에 따른 성공/실패 화면 전환까지 끝까지 수행할 수 있는 상태가 된다.

- [x] `src/lib/api/orders.ts` 신규: `createOrder`, `confirmOrder`, `getOrders`, `getOrder`, `cancelOrder` — 모두 `apiFetch` 경유, `Order`/`OrderItem` 관련 타입 명시.
- [x] `src/lib/api/addresses.ts` 신규: `getAddresses`, `createAddress`, `updateAddress` — `apiFetch` 경유.
- [x] `/checkout` 페이지(`src/app/(shell)/checkout/page.tsx`): 배송지 섹션(저장된 주소 목록 조회 후 기본배송지 자동 선택, 다른 주소 선택 또는 "새 배송지 추가" 폼 전환), 주문 상품 목록(Phase 3에서 전달된 선택 항목 읽기 전용 요약), 합계 금액, "결제하기" 버튼.
- [x] 배송지 신규 입력 폼은 React Hook Form + Zod, `src/components/forms/`의 필드 컴포넌트(`TextField` 등) 사용, 없는 필드(우편번호 등)는 신규 컴포넌트 추가. 서버 `fieldErrors`는 `setError`로 매핑.
- [x] "결제하기" 제출 시 `POST /orders` 호출(성공 시 `orderId` 확보) 후 Toss Payment Widget SDK를 로드해 결제수단 선택 UI 렌더링. SDK 클라이언트 키는 환경변수(`NEXT_PUBLIC_TOSS_CLIENT_KEY` 등)로 주입, 하드코딩 금지.
- [x] Toss 결제 승인 콜백/리다이렉트 처리 라우트(성공 리다이렉트 URL): 콜백에서 받은 `paymentKey`/`orderId`/`amount`로 `POST /orders/{orderId}/confirm` 호출.
- [x] 주문 완료 화면: confirm 성공 시 주문 요약 + "주문내역 보기"(→ `/mypage/orders/[orderId]`) 링크.
- [x] 주문 실패 화면: confirm 실패 또는 사용자 결제창 이탈 시 실패 안내 + "장바구니로 돌아가기"(→ `/cart`) 링크. 같은 주문으로 재결제 진입 경로를 만들지 않음(라우팅 계약 확인 대상).
- [x] 에러 처리: `POST /orders`의 `OUT_OF_STOCK`/`VALIDATION_FAILED`/`PRODUCT_NOT_FOUND`는 `ApiError.errorCode` 분기해 사용자에게 적절히 안내(재고 부족 항목 표시 등), `fieldErrors` 있으면 인라인.
- [x] 검증(자동): `npm run build`, `npm run lint` 통과.
- [ ] 검증(수동, 브라우저, 토스 샌드박스): 체크아웃 진입 시 기본배송지 자동 선택/미보유 시 입력 폼 즉시 노출, 실패 화면 전환은 확인됨(`docs/e2e/2026-08-29-cart-order-payment.md` 시나리오 2/4/5 PASS). **결제위젯 렌더링~confirm 성공 경로는 미검증** — Toss 상점(스토어) 미등록으로 클라이언트 키가 결제위젯 API에서 401, 결제수단 UI 자체가 뜨지 않음. 상점 등록 후 재검증 필요(시나리오 7)

## Phase 5: 프론트 마이페이지 주문내역 + 취소 + 회귀

이 phase가 끝나면 마이페이지에서 주문내역 목록/상세 조회와 `PAID` 주문 취소가 브라우저에서 완결되고, 이번 변경이 기존 상품/장바구니/위시리스트 기능을 깨지 않았음이 확인된 상태가 된다.

- [x] `/mypage/orders` 페이지(`src/app/(shell)/mypage/orders/page.tsx`): `GET /orders` 연동, 상태 배지/금액/주문일시 목록 표시.
- [x] `/mypage/orders/[orderId]` 페이지: `GET /orders/{orderId}` 연동, 상품구성/배송지/결제상태 표시. `PAID` 상태에서만 취소 버튼 노출 — `PENDING`/`FAILED`/`CANCELLED`와 배타적으로 조건화됐는지 상태별 노출 여부를 표로 정리해 확인(`docs/backlog/2026-08-26-app-redesign-phase2-01.md` 사례 참고).
- [x] 취소 버튼 클릭 시 `POST /orders/{orderId}/cancel` 호출 → 성공 시 상태 갱신(`CANCELLED`로 재렌더링, 취소 버튼 사라짐), 실패(`ORDER_NOT_CANCELLABLE` 등) 시 `Toast`로 안내.
- [x] 마이페이지(`src/app/(shell)/mypage/page.tsx`)에 주문내역 진입 링크 추가(기존 메뉴 리스트 패턴 유지, 최소 변경).
- [x] 회귀 확인: 비로그인/로그인 상태에서 상품 목록/상세 조회, 장바구니 담기(`src/lib/storage/cart.ts`), 위시리스트 토글이 이번 변경으로 깨지지 않았는지 코드 경로상 확인. `/cart`의 기존 쿠폰 placeholder UI가 Phase 3 변경 이후에도 정상 렌더링되는지 포함.
- [x] 검증(자동): `npm run build`, `npm run lint`, 백엔드 `./gradlew test` 통과(Phase 1~5 통합 상태 재확인).
- [ ] 검증(수동, 브라우저): `FAILED`/`CANCELLED` 주문 상세에서 취소 버튼 미노출 확인됨(`docs/e2e/2026-08-29-cart-order-payment.md` 시나리오 5/6 PASS). **`PAID` 상태 도달·취소 플로우는 미검증** — Phase 4와 동일 사유(상점 미등록으로 결제위젯 진입 불가). 상점 등록 후 시나리오 7로 재검증 필요

## Phase 6: E2E 검증

Phase 5의 backend/frontend reviewer 승인 직후, `e2e-tester`가 spec `docs/specs/2026-08-29-cart-order-payment.md`의 사용자 시나리오(장바구니 항목 선택, 주문서 작성, 결제 성공/실패, 방치 시 만료, 주문내역 조회, 주문 취소)를 근거로 `docs/e2e/` 규격(`.claude/rules/e2e-format.md`)에 맞춰 케이스를 그 시점에 도출·작성하고 dev-browser로 실행한다.

- [x] `docs/e2e/YYYY-MM-DD-cart-order-payment.md` 작성 및 각 시나리오 실행, 전체 pass 확인 후 이 phase의 체크박스를 체크한다. Toss 실결제 연동을 브라우저 자동화로 완결하기 어려운 시나리오(결제위젯 내부 UI)는 사전조건/판정 기준란에 한계를 명시하고 가능한 범위(주문 생성까지, 실패/취소/조회 흐름)에서 검증한다. 실패 시나리오가 있으면 `docs/backlog/` 규격대로 실패를 기록하고 이 phase는 미완료로 남긴다.
