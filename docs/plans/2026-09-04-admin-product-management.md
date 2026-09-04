---
date: 2026-09-04
feature: admin-product-management
spec: 2026-09-04-admin-product-management.md
status: in_progress
---

# 관리자 기반 및 상품 관리 플랜

## 개요

`docs/specs/2026-09-04-admin-product-management.md`를 기반으로 한다. 스펙은 "관리자 인가 기반", "`ProductVariant` 도입", "관리자 API", "관리자 화면", "고객 화면 반영" 다섯 덩어리를 한 번에 다루는데, 이들은 서로 강하게 의존한다 — 관리자 API는 인가 없이는 노출할 수 없고, 상품 등록 폼은 variant 모델이 확정되어야 만들 수 있고, 고객 화면은 variant를 내려주는 API가 있어야 붙일 수 있다.

그래서 **영향 범위가 넓은 것부터 아래에서 위로** 쌓는 순서로 phase를 나눴다. 도메인/스키마를 먼저 확정한 뒤 그 위에 API, 그 위에 화면을 올리면 뒤 phase가 앞 phase를 되돌릴 일이 없다. 반대로 화면부터 만들면 variant 모델이 흔들릴 때마다 폼과 상세 화면을 다시 짜야 한다.

- **Phase 1 (인가 기반)** — 이후 모든 관리자 기능의 전제. 인가는 도메인과 독립적이라 가장 먼저 끝낼 수 있고, 끝나면 "`/admin/**`은 ADMIN만 통과한다"만 확인하면 된다.
- **Phase 2 (`ProductVariant` 도입 + 데이터 이관)** — **이 플랜에서 가장 위험한 구간이자, 이미 배포되어 실제 고객 주문이 쌓인 데이터를 건드리는 유일한 지점이다.** `product.stock`/`product.sold_out` 제거는 되돌리기 어렵고, 실패하면 기존 주문 이력 표시가 깨진다. 그래서 API/화면 작업과 섞지 않고 독립 phase로 격리해, 이 phase 안에서 이관 결과와 기존 주문 이력 보존을 끝까지 검증한 뒤에만 다음으로 넘어간다.
- **Phase 3 (관리자 API)** — Phase 1의 인가와 Phase 2의 도메인이 모두 서 있어야 성립한다. 여기까지 오면 화면 없이 curl만으로 상품 등록이 가능해진다(운영 병목이 실질적으로 해소되는 지점).
- **Phase 4 (관리자 화면)** — Phase 3 API의 소비자. `/admin`은 `(shell)` 밖 데스크톱 레이아웃이라 기존 고객 화면과 충돌 없이 독립적으로 만들 수 있다.
- **Phase 5 (고객 화면 반영)** — 서버가 이미 variant와 `q`를 내려주는 상태에서 붙이는 마지막 결선. 장바구니 스키마 변경(구 형식 폐기)이 포함되어 사용자 눈에 보이는 변화가 가장 큰 구간이다.
- **Phase 6 (E2E 검증)** — 관리자 등록 → 고객 구매까지 한 흐름으로 통과하는지 확인.

### 과거 실패 이력에서 가져온 주의점 (`docs/backlog/`)

- `2026-08-29-cart-order-payment-phase1-01`(backend): 낙관적 락 재시도 카운팅을 "총 시도 횟수"로 오독한 off-by-one. Phase 2에서 재고 로직을 variant로 옮길 때 **"최초 1회 + 재시도 2회 = 총 3회 시도"** 의미를 그대로 보존해야 한다.
- `2026-08-31-product-review-phase4-01`(frontend): `200 + 빈 바디`를 `res.json()`이 파싱하려다 터진 계약 불일치. Phase 3의 신규 GET 엔드포인트도 nullable 바디를 만들지 않도록 설계한다.
- `2026-08-29-cart-order-payment-phase6-01`(frontend): 공통 레이아웃의 고정 UI가 페이지 CTA를 덮어 클릭 불가. Phase 4의 관리자 폼 저장 버튼도 같은 함정을 밟지 않는지 브라우저에서 확인한다.
- `2026-08-26-app-redesign-phase2-01`(frontend): 검색 화면의 상태(미입력/입력 중/실행됨) 조건이 서로 배타적이지 않았던 문제. Phase 5의 `/search` 서버 검색 전환에서 상태 조건을 다시 흐트러뜨리지 않는다.
- `2026-08-31-product-review-phase4-02`(test): 같은 상품명이 여러 위젯에 중복 렌더링될 때 `getByText().first()`가 엉뚱한 카드를 잡음. Phase 6 e2e 셀렉터는 컨테이너를 먼저 스코프한다.

## Phase 1: 관리자 인가 기반

JWT에 실린 실제 권한으로 인가가 동작한다. 이 phase가 끝나면 `/admin/**` 아래에 아무 엔드포인트나 하나 올려두고 (a) 비로그인 → 401, (b) 일반 회원 → 403, (c) 승격된 관리자 → 통과가 확인된다. 상품 도메인은 아직 건드리지 않는다.

- [x] `JwtTokenProvider`(`backend/.../auth/security/JwtTokenProvider.java`)가 access token에 `role` 클레임을 싣는다 — `createAccessToken(Long userId, Role role)`로 시그니처를 확장하고, 파싱 시 userId와 `Role`을 함께 돌려주는 결과 타입(예: `AccessTokenPayload` record)을 노출한다. refresh token은 현행 유지
- [x] `AuthService`(`backend/.../auth/service/AuthService.java`)의 토큰 발급 경로(signup/login/refresh) 전부가 사용자 `Role`을 넘겨 access token을 만든다 — 컴파일 에러가 나는 호출부가 남지 않도록 전 경로 반영
- [x] `JwtAuthenticationFilter`(`backend/.../auth/security/JwtAuthenticationFilter.java`)의 `ROLE_USER` 하드코딩(38행)을 제거하고 토큰의 `role` 클레임으로 `SimpleGrantedAuthority("ROLE_" + role)`을 부여한다. `role` 클레임이 없는 구 토큰은 `ROLE_USER`로 취급한다(스펙 시나리오 A의 "최대 30분간 관리자로 인식되지 않는다"가 이 동작)
- [x] `SecurityConfig`(`backend/.../common/config/SecurityConfig.java`)에 `.requestMatchers("/admin/**").hasRole("ADMIN")`을 `anyRequest().authenticated()` 앞에 추가한다
- [x] 403이 기존 `ErrorResponse` 포맷으로 나가도록 `AccessDeniedHandler` 구현체를 추가하고(`AuthEntryPoint`와 같은 패키지, `ErrorCode.FORBIDDEN` 사용) `exceptionHandling`에 등록한다 — `backend/CLAUDE.md`의 "401/403도 동일한 `ErrorResponse`로 직렬화" 컨벤션
- [x] `UserResponse`(`backend/.../auth/dto/UserResponse.java`)에 `role` 필드를 추가해 `GET /auth/me` 응답에 노출한다(`@Schema` 포함)
- [x] ~~`V13__promote_admin_user.sql` + `application.yml`의 flyway placeholder로 관리자를 자동 승격한다~~ → **철회(2026-09-04).** 관리자가 1명이고 환경당 승격이 1회뿐이라, 마이그레이션 + placeholder + 환경변수 + `.env` 로딩까지 얹는 비용이 얻는 것보다 컸다. **DB 수동 `UPDATE` 1회**로 대체한다(절차는 spec 시나리오 A). `V13`은 결번으로 남긴다 — flyway는 번호 공백을 허용하며, 재번호는 이미 적용된 환경의 체크섬을 깨뜨린다
- [x] 프론트 `AuthProvider`/`lib/api/auth.ts`의 사용자 타입에 `role: "USER" | "ADMIN"`을 추가해 `/auth/me` 응답을 그대로 받는다(화면 가드는 Phase 4)
- [ ] 검증 — `./gradlew build`, `./gradlew test` 통과. 기존 인증 관련 테스트가 새 토큰 시그니처로 갱신되어 통과한다
- [ ] 검증 — `npm run build`, `npm run lint` 통과 (`frontend/`)
- [ ] 검증(수동, 로컬 DB + curl) — 로컬에서 회원가입 후 `UPDATE users SET role = 'ADMIN' WHERE email = '...'`을 직접 실행해 승격한다. 재로그인 후 `GET /auth/me`에 `role`이 실려 오는지, `/admin/**` 엔드포인트에 대해 비로그인 401 / 일반 회원 403(`ErrorResponse` 포맷) / 관리자 200이 나오는지 확인

## Phase 2: `ProductVariant` 도입 및 재고·주문 로직 이전

재고의 단위가 `Product`에서 `ProductVariant`로 완전히 옮겨간다. 이 phase가 끝나면 기존 상품 15개가 `size = null` 단일 variant로 이관된 채 주문·결제·취소 전 흐름이 예전과 동일하게 동작하고, **기존 주문 이력이 그대로 보인다**. `product.stock`/`product.sold_out` 컬럼 제거는 되돌리기 어려우므로 검증 step을 두텁게 둔다.

- [x] `ProductVariant` 엔티티와 `ProductVariantRepository`를 추가한다(`backend/.../product/domain/ProductVariant.java`, `.../product/repository/ProductVariantRepository.java`) — 필드는 `id`, `product`(FK, not null), `size`(nullable), `stock`(not null, >= 0), `@Version version`. 재고 변경은 `deductStock(int)`/`restoreStock(int)` 도메인 메서드로만 하고 setter를 두지 않는다(`backend/CLAUDE.md` Entity 컨벤션)
- [x] `ProductStatus` enum(`ON_SALE`/`HIDDEN`/`DELETED`)을 추가하고 `Product`(`backend/.../product/domain/Product.java`)에서 `stock`·`soldOut` 필드와 `deductStock`/`restoreStock`을 제거한다. 대신 `variants` 연관(`@OneToMany(mappedBy="product", cascade=ALL, orphanRemoval=true)`), `status` 필드, 재고 합에서 파생 판정하는 `isSoldOut()`을 둔다. `@Version version`은 유지한다
- [x] `V14__create_product_variant.sql` — `product_variant` 테이블 생성(유니크 제약은 `size`가 nullable임을 고려해 설계), 기존 상품 전체를 `size = NULL` 단일 variant로 INSERT하면서 `product.stock` 값을 그대로 옮긴 뒤 `product.stock` 컬럼 DROP
- [x] `V15__replace_product_sold_out_with_status.sql` — `product.status` 컬럼 추가(not null, 기본 `ON_SALE`), 기존 행 전부 `ON_SALE`로 채운 뒤 `sold_out` 컬럼 DROP
- [x] `V16__add_variant_id_to_order_item.sql` — `order_item.variant_id` nullable 컬럼 + FK 추가. **기존 행의 `size` 문자열은 갱신하지 않는다**(소급 매핑은 스펙 Out of Scope)
- [x] `OrderItem`(`backend/.../order/domain/OrderItem.java`)에 nullable `variant` 연관을 추가하고, `size`는 주문 시점 스냅샷 문자열로 계속 보존한다. 신규 주문은 variant의 `size`를 그대로 복사해 채운다
- [x] `OrderItemRequest`(`backend/.../order/dto/OrderItemRequest.java`)가 `size` 대신 `variantId`(`@NotNull`)를 받는다. `productId`·`quantity`는 유지
- [x] `OrderService`(`backend/.../order/service/OrderService.java`)의 `assertAllInStock`과 `deductStockWithRetry`가 `ProductVariant` 기준으로 동작한다 — variant 조회 실패 시 `PRODUCT_NOT_FOUND`, variant가 요청 `productId`에 속하지 않으면 거부, 재고 부족은 기존 `OUT_OF_STOCK` 응답 형태 유지. **재시도 카운팅은 "최초 1회 + 재시도 최대 2회 = 총 3회 시도" 의미를 그대로 유지한다**(`docs/backlog/2026-08-29-cart-order-payment-phase1-01.md` 재발 방지)
- [x] `OrderPaymentTransactionSupport`(`backend/.../payment/service/OrderPaymentTransactionSupport.java`)의 `restoreStockWithRetry`가 `item.getProduct()` 대신 `item.getVariant()` 기준으로 복원한다. `variant`가 `null`인 구 주문 행은 복원 대상에서 건너뛴다(과거 주문은 이미 종결 상태이며 매핑할 variant가 없음)
- [x] `ProductSummaryResponse`/`ProductDetailResponse`(`backend/.../product/dto/`)의 `soldOut`을 `product.isSoldOut()` 파생값으로 바꾸고, 상세 응답에 `variants` 배열(`variantId`, `size`, `stock`, `soldOut`)을 추가한다(`@Schema` 포함)
- [x] 고객 조회가 `ON_SALE`만 노출한다 — `ProductRepository.findAllByCategory` 쿼리에 `status = ON_SALE` 조건 추가, `ProductService.getProduct`는 `ON_SALE`이 아니면 `PRODUCT_NOT_FOUND`(404)
- [x] 기존 테스트를 새 모델로 갱신한다 — `OrderServiceTest`, `OrderServiceStockRetryTest`, `PaymentServiceTest`의 `getStock()` 단언과 픽스처가 variant 기준으로 바뀐다
- [x] 서로 다른 사이즈를 동시에 주문해도 낙관적 락 충돌이 발생하지 않음을 검증하는 테스트를 추가한다(같은 상품의 서로 다른 variant 두 개를 동시 주문 → 둘 다 성공)
- [ ] 검증 — `./gradlew build`, `./gradlew test` 통과
- [ ] 검증(수동, 로컬 DB) — 마이그레이션 적용 후 `product_variant` 행 수가 기존 상품 수와 같고, 각 행의 `stock`이 이관 전 `product.stock`과 일치하는지 SQL로 대조한다(이관 전 값을 미리 조회해 기록해둘 것). `product` 테이블에 `stock`/`sold_out` 컬럼이 없고 `status`가 전부 `ON_SALE`인지 확인
- [ ] 검증(수동, 로컬 DB) — **마이그레이션 후 기존 `order_item` 행의 `size` 문자열이 변경·유실 없이 그대로 남아 있는지** SQL로 확인한다(`variant_id`는 전부 `NULL`이어야 정상)
- [ ] 검증(수동, 브라우저) — 마이그레이션 이전에 생성된 기존 주문의 `/mypage/orders`, `/mypage/orders/[orderId]` 화면이 상품명·사이즈·금액까지 정상 표시되는지 눈으로 확인한다. 신규 주문 1건을 `variantId`로 생성해 재고가 해당 variant에서만 차감되는지, 주문 취소/만료 시 같은 variant로 복원되는지 확인

## Phase 3: 관리자 상품 API 및 상품 검색

화면 없이도 curl만으로 상품을 등록·수정·숨김·삭제할 수 있고, 등록한 상품이 고객 목록과 검색에 즉시 잡힌다. 운영 병목이 실질적으로 해소되는 지점이다.

- [x] `ErrorCode`(`backend/.../common/exception/ErrorCode.java`)에 `VARIANT_REQUIRED`, `DUPLICATE_VARIANT_SIZE`, `VARIANT_IN_USE`, `IMAGE_LIMIT_EXCEEDED`(모두 400)를 추가한다
- [x] 관리자 상품 요청/응답 DTO를 추가한다(`backend/.../product/dto/` 또는 `admin` 하위 패키지) — `AdminProductRequest`(`name`, `description`, `price`, `discountPrice`, `category`, `status`, `imageUrls`, `variants[{id, size, stock}]`), `AdminProductResponse`, `AdminProductListResponse`. 형식 검증은 Bean Validation, DB 조회가 필요한 규칙은 Service에서 `CustomException`으로 처리한다(`backend/CLAUDE.md` Write API 검증)
- [x] `AdminProductService`가 등록·수정·soft delete를 처리한다 — variant 0개면 `VARIANT_REQUIRED`, 같은 상품 내 `size` 중복이면 `DUPLICATE_VARIANT_SIZE`(`null` 사이즈도 하나만 허용), 요청에 없는 기존 variant는 삭제하되 `OrderItemRepository`로 사용 이력이 있으면 `VARIANT_IN_USE`, `imageUrls`가 6개 이상이면 `IMAGE_LIMIT_EXCEEDED`. `DELETE`는 행을 지우지 않고 `status = DELETED`로 전이
- [x] `AdminProductController`(`backend/.../product/controller/AdminProductController.java`)가 `GET /admin/products`(page·size·status 기본 `ON_SALE,HIDDEN`·q), `GET /admin/products/{id}`(`DELETED`도 조회 가능), `POST /admin/products`, `PUT /admin/products/{id}`, `DELETE /admin/products/{id}`를 노출한다. Controller는 Repository를 직접 호출하지 않는다
- [x] 관리자 상세/목록 응답은 **비어 있는 바디를 절대 내려보내지 않는다** — nullable 최상위 응답을 만들지 않고, 없는 리소스는 `PRODUCT_NOT_FOUND` 404로 명시 처리한다(`docs/backlog/2026-08-31-product-review-phase4-01.md` 재발 방지)
- [x] Cloudinary 서명 발급을 추가한다 — `POST /admin/images/signature`가 `{ signature, timestamp, apiKey, cloudName, folder }`를 반환한다. 서명 생성 로직은 서비스 계층에 두고, API secret은 `application.yml`에서 `${MOMENTIVE_CLOUDINARY_API_SECRET}` 등 환경변수로 주입하며 코드·설정 파일에 값이 남지 않고 **응답에도 포함되지 않는다**
- [x] `GET /products`에 `q` 파라미터를 추가한다 — `ProductRepository`의 조회 쿼리에 `name` 부분일치(대소문자 무시) 조건을 넣고 `category`/`sort`/페이지네이션과 조합되게 한다. `q`가 없으면 기존과 동일 동작
- [x] 신규 엔드포인트 전부에 `@Operation`, DTO 필드에 `@Schema`, 인증 필요 엔드포인트에 `@SecurityRequirement`를 작성한다
- [x] `AdminProductService` 단위/통합 테스트를 추가한다 — `VARIANT_REQUIRED`, `DUPLICATE_VARIANT_SIZE`, `VARIANT_IN_USE`, `IMAGE_LIMIT_EXCEEDED` 각 케이스와 soft delete 후 행이 남아 있는지, `HIDDEN`/`DELETED` 상품이 고객 목록·검색·상세에서 제외되는지 검증
- [x] `q` 검색 테스트를 추가한다 — 부분일치 결과, `category`·`sort` 조합 적용, **100개를 초과하는 상품을 넣고 101번째 이후 상품이 검색되는지**(현재 프론트 100개 캡이 근본 원인이었던 버그의 회귀 방지)
- [ ] 검증 — `./gradlew build`, `./gradlew test` 통과
- [ ] 검증(수동, curl) — 관리자 토큰으로 상품 등록 → `GET /products`에 즉시 노출 → `PUT`으로 variant 재고 수정 → `DELETE`로 `DELETED` 전이 → 고객 상세 404 확인. 일반 회원 토큰으로 같은 엔드포인트 호출 시 403 확인
- [ ] 검증(수동, 외부 연동) — `POST /admin/images/signature`로 받은 서명으로 Cloudinary에 실제 파일을 업로드해 secure URL이 반환되는지 확인한다. 자동 reviewer로는 검증 불가능한 외부 서비스 연동이다. 키 발급과 계정/폴더 설정이 별개 단계일 수 있으므로 착수 전 Cloudinary 콘솔에서 업로드 preset·폴더 권한을 먼저 확인한다(`docs/backlog/2026-08-30-cart-order-payment-phase4-01.md` 재발 방지)

## Phase 4: 관리자 화면

관리자가 브라우저에서 상품을 등록·수정할 수 있다. `/admin`은 `(shell)` 밖 데스크톱 폭 레이아웃이며(모바일 프레임·하단탭 없음 — `frontend/CLAUDE.md`의 "셸을 의도적으로 피하는 예외"에 해당), 접근 보호는 레이아웃 한 곳에서만 한다.

- [x] `frontend/src/lib/api/admin.ts`를 추가한다 — 상품 목록/상세/등록/수정/삭제, 이미지 서명 발급 함수와 요청·응답 타입. 반드시 공통 `apiFetch`(`src/lib/api/client.ts`)를 통해 호출하고 `ApiError`를 그대로 던진다(도메인 API 파일이 `fetch`를 직접 호출하지 않는다는 필수 컨벤션)
- [x] `frontend/src/app/admin/layout.tsx`를 추가한다 — `AuthProvider`의 `user.role !== "ADMIN"`이면 홈으로 리다이렉트. 검사는 이 파일에서만 하고 하위 페이지에 복붙하지 않는다. 프론트 검사는 UX 차원이고 실제 방어선은 백엔드 `hasRole("ADMIN")`임을 주석으로 남긴다
- [x] `frontend/src/app/admin/page.tsx`(상품 목록)를 추가한다 — 썸네일/이름/카테고리/가격/재고 합/상태/수정 링크 열, 상단 검색 입력·상태 필터·"상품 등록" 버튼, 페이지네이션. 표 컴포넌트는 `src/app/admin/` 안의 로컬 컴포넌트로 두고 공용 `src/components/`에 올리지 않는다(스펙 명시)
- [x] `frontend/src/app/admin/products/new/page.tsx`와 `frontend/src/app/admin/products/[id]/page.tsx`(등록·수정 폼)를 추가한다 — React Hook Form + Zod, 기존 `Button`/`TextField` 등 공용 컴포넌트와 디자인 토큰 재사용, 서버 `ApiError.fieldErrors`를 `setError`로 인라인 매핑
- [x] 폼의 이미지 영역을 구현한다 — 파일 선택 → 서명 발급 → Cloudinary 직접 업로드 → 업로드 순서대로 미리보기 나열(순서가 `displayOrder`), 순서 변경·개별 삭제, 최대 5장. **업로드 실패는 해당 장만 실패 표시하고 나머지 미리보기는 유지**한다. 이미지 0장으로도 저장 가능
- [x] 폼의 variant 영역을 구현한다 — 사이즈 이름 + 재고 수량 행 추가/삭제, 사이즈를 비우면 `size = null` 단일 variant로 전송. 최소 1행 요구를 클라이언트에서도 안내하되 최종 판정은 서버 `VARIANT_REQUIRED`/`DUPLICATE_VARIANT_SIZE`/`VARIANT_IN_USE` 응답을 인라인 표시로 반영한다
- [ ] 검증 — `npm run build`, `npm run lint` 통과 (`frontend/`)
- [ ] 검증(수동, 브라우저 + 외부 연동) — 관리자 계정으로 `/admin` 진입 후 상품 1개를 실제 이미지 업로드까지 포함해 등록하고, 목록에 노출되는지 확인한다. 이어서 재고를 수정하고 `HIDDEN`/`DELETED`로 전환해 고객 화면에서 사라지는지 확인. 저장/등록 버튼이 다른 고정 UI에 가려지지 않고 실제로 클릭되는지 함께 확인한다(`docs/backlog/2026-08-29-cart-order-payment-phase6-01.md` 재발 방지). Cloudinary 실업로드가 포함되어 자동 reviewer로는 검증 불가능하다
- [ ] 검증(수동, 브라우저) — 일반 회원 계정과 비로그인 상태에서 각각 `/admin`에 직접 접근해 홈으로 리다이렉트되는지 확인한다

## Phase 5: 고객 화면 반영

고객이 보는 화면이 실제 variant와 서버 검색으로 동작한다. 이 phase가 끝나면 상품상세의 S/M/L/XL 하드코딩이 사라지고, 100개 캡 때문에 검색되지 않던 상품이 검색되며, 장바구니가 `variantId`를 갖는다.

- [x] `frontend/src/lib/api/products.ts`의 타입과 호출을 갱신한다 — `ProductDetail`에 `variants: { variantId, size, stock, soldOut }[]` 추가, `getProducts`에 `q` 옵션 추가. 이 파일은 서버 컴포넌트에서도 호출되는 기존 raw `fetch` 경로를 유지하되(인증 불필요 공개 API), 신규 관리자 API는 Phase 4대로 `apiFetch`를 쓴다
- [x] `ProductDetailView`(`frontend/src/components/commerce/ProductDetailView.tsx`)에서 `SIZES = ["S","M","L","XL"]` 하드코딩(31행)을 제거하고 `product.variants`로 대체한다 — 재고 0인 사이즈는 선택 불가로 표시, variant가 1개이고 `size`가 `null`이면 사이즈 선택 영역과 "사이즈 가이드" 링크를 렌더링하지 않는다, 전체 재고 합 0이면 상품 전체 품절 표시
- [x] `SizeSelector`(`frontend/src/components/commerce/SizeSelector.tsx`)가 선택 불가(품절) 사이즈를 표현할 수 있도록 확장한다 — 비활성 상태 스타일은 `globals.css` 토큰만 사용하고 raw hex/shadow 값을 넣지 않는다
- [x] `frontend/src/lib/storage/cart.ts`의 `CartItem`에 `variantId`를 추가하고 `cartKeyOf`를 `variantId` 기준으로 바꾼다. **`variantId`가 없는 구 형식 항목은 로드 시(`getCart`) 조용히 버린다** — 재고 검증이 불가능한 유령 항목을 남기지 않는다
- [x] 장바구니 담기·주문 생성 경로가 `variantId`를 실어 나른다 — `ProductDetailView`의 `addToCart`, `/cart` 페이지, `checkoutSelection`, `POST /orders` 요청 본문(`items[].variantId`)까지 한 줄로 이어지는지 확인 가능한 형태로 반영한다
- [x] `/search`(`frontend/src/app/(shell)/search/page.tsx`)를 서버 검색으로 전환한다 — `getProducts(0, 100)` + `name.includes()` 클라이언트 필터링을 제거하고 `q` 파라미터 호출로 대체, 자동완성은 같은 API를 작은 `size`로 호출. **미입력/입력 중/검색 실행됨 세 상태의 렌더 조건이 서로 배타적으로 유지되는지** 함께 확인한다(`docs/backlog/2026-08-26-app-redesign-phase2-01.md` 재발 방지)
- [x] 검색 API 실패 시 "검색 결과가 없어요"와 명확히 구분되는 실패 안내를 표시한다 — `catch`에서 `ApiError`를 임의 문자열로 뭉뚱그리지 않는다
- [ ] 검증 — `npm run build`, `npm run lint` 통과 (`frontend/`)
- [ ] 검증(수동, 브라우저) — 사이즈가 있는 상품 상세에서 등록된 사이즈만 보이고 재고 0 사이즈가 선택 불가인지, `size = null` 상품에서 사이즈 선택 영역이 아예 없는지 확인. 구 형식 장바구니 데이터를 localStorage에 심어둔 뒤 새로고침해 조용히 사라지는지 확인. `/search`에서 101번째 이후에 등록한 상품이 검색되는지 확인

## Phase 6: E2E 검증

관리자 등록부터 고객 구매까지 한 유저 플로우로 이어 실행해 통과를 확인한다.

- [ ] `e2e-tester` 에이전트가 `.claude/rules/e2e-format.md` 규격으로 `docs/e2e/2026-09-04-admin-product-management.md`를 생성한다 — 시나리오 축은 (1) 관리자 로그인 후 `/admin` 진입, (2) 사이즈 있는 상품 등록, (3) 고객 화면에서 검색으로 그 상품 찾기, (4) 상품상세에서 품절 사이즈 선택 불가 확인 후 재고 있는 사이즈로 장바구니 담기, (5) 주문 생성까지, (6) 관리자가 `HIDDEN`으로 전환 후 고객 화면에서 사라지는지
- [ ] 셀렉터는 컨테이너를 먼저 스코프한 뒤 텍스트를 찾는다 — 같은 상품명이 랭킹 캐러셀과 메인 그리드에 중복 렌더링되므로 `getByText().first()`처럼 DOM 순서에 의존하지 않는다(`docs/backlog/2026-08-31-product-review-phase4-02.md` 재발 방지)
- [ ] 검증(수동, 브라우저 자동화) — dev-browser로 전체 스크립트를 1회 실행해 모든 시나리오가 PASS하는지 확인한다. 실패 시 `.claude/rules/backlog-format.md` 규격으로 `docs/backlog/2026-09-04-admin-product-management-phase6-01.md`를 남긴다
