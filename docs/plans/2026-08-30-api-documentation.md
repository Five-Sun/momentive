---
date: 2026-08-30
feature: api-documentation
spec: 2026-08-30-api-documentation.md
status: planned
---

# Swagger(OpenAPI) API 문서화 플랜

## 개요

`docs/specs/2026-08-30-api-documentation.md`를 기반으로 springdoc-openapi 도입 → 기존 4개 컨트롤러(Auth/Product/Address/Order) 애노테이션 소급 적용 → 컨벤션/리뷰어 체크리스트 반영 순서로 진행한다.

이 순서를 택한 이유: 설정(Phase 1)이 먼저 끝나야 애노테이션을 붙였을 때(Phase 2) Swagger UI로 바로 확인할 수 있고, 컨벤션 문서화(Phase 3)는 소급 적용 과정에서 실제로 어떤 패턴이 반복되는지 확인한 뒤 정리하는 편이 정확하다. 이 기능은 사용자 시나리오(로그인/장바구니 같은 플로우)가 아니라 정적 문서 확인이 전부이므로 별도의 E2E 검증 phase는 두지 않고, Phase 1/2의 수동 브라우저 확인 step으로 검증을 완결한다.

## Phase 1: springdoc 의존성 도입 및 기본 설정

이 phase가 끝나면 로컬에서 `/swagger-ui/index.html`에 접속했을 때(엔드포인트 내용은 비어있거나 애노테이션 없는 상태로 보이더라도) Swagger UI 화면 자체가 정상적으로 뜨고, title/version이 표시되는 상태가 된다.

- [ ] `backend/build.gradle.kts`의 `dependencies` 블록에 `implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")` 추가
- [ ] `backend/src/main/java/com/momentive/backend/common/config/OpenApiConfig.java` 신설 — `@Configuration` 클래스에 `OpenAPI` 빈을 정의. `Info`는 title "모멘티브 API", version 고정 문자열 `"v1"`(description은 생략 또는 한 줄). 쿠키 기반 인증(`AuthCookieProvider.ACCESS_TOKEN_COOKIE = "access_token"`)을 문서화할 `SecurityScheme`(`SecurityScheme.Type.APIKEY`, `SecurityScheme.In.COOKIE`, name `"access_token"`)을 정의하고, `Components`에 등록해 이후 Phase 2에서 `@SecurityRequirement`가 참조할 스킴명을 확정한다.
- [ ] `backend/src/main/java/com/momentive/backend/common/config/SecurityConfig.java`의 `authorizeHttpRequests` permitAll 목록(`requestMatchers("/health", "/auth/signup", "/auth/login", "/auth/logout", "/auth/refresh").permitAll()`, `requestMatchers("/products/**").permitAll()`)에 `/swagger-ui/**`, `/v3/api-docs/**`를 추가하는 새 `requestMatchers(...).permitAll()` 라인 추가
- [ ] 검증(자동): `cd backend && ./gradlew build` 통과
- [ ] 검증(수동, 브라우저): 로컬 서버(`./dev.sh`) 기동 후 `http://localhost:8081/swagger-ui/index.html` 접속 시 문서 화면이 렌더링되고 상단에 title "모멘티브 API", version "v1"이 표시되는지 확인

## Phase 2: 기존 4개 컨트롤러 애노테이션 소급 적용

이 phase가 끝나면 Swagger UI에서 Auth/Product/Address/Order 4개 도메인의 전체 엔드포인트가 단일 그룹으로 노출되고, 각 엔드포인트의 요약과 요청/응답 필드 설명을 확인할 수 있으며, 인증이 필요한 엔드포인트만 자물쇠 아이콘으로 구분되는 상태가 된다.

- [ ] `backend/src/main/java/com/momentive/backend/auth/controller/AuthController.java`: 5개 엔드포인트(`signup`, `login`, `logout`, `refresh`, `me`) 전체에 `@Operation(summary = "...")` 추가. `me`(`GET /auth/me`)에만 `@SecurityRequirement(name = "...")`(Phase 1에서 정의한 스킴명) 추가 — 나머지 4개는 `SecurityConfig`상 permitAll이므로 붙이지 않음
- [ ] `backend/src/main/java/com/momentive/backend/auth/dto/{LoginRequest,SignupRequest,UserResponse}.java`: record 컴포넌트(생성자 파라미터)에 `@Schema(description = "...")` 추가 — 3개 DTO 전체 필드
- [ ] `backend/src/main/java/com/momentive/backend/product/controller/ProductController.java`: 2개 엔드포인트(`getProducts`, `getProduct`) 전체에 `@Operation(summary = "...")` 추가. `SecurityConfig`상 `/products/**`가 전부 permitAll이므로 `@SecurityRequirement`는 붙이지 않음
- [ ] `backend/src/main/java/com/momentive/backend/product/dto/{ProductListResponse,ProductDetailResponse,ProductSummaryResponse,ProductImageResponse}.java`: 4개 DTO 전체 필드에 `@Schema(description = "...")` 추가
- [ ] `backend/src/main/java/com/momentive/backend/address/controller/AddressController.java`: 클래스 레벨 또는 3개 엔드포인트(`getAddresses`, `createAddress`, `updateAddress`) 각각에 `@Operation(summary = "...")`와 `@SecurityRequirement` 추가 — `SecurityConfig`상 `/addresses/**`는 `anyRequest().authenticated()`로 전체 인증 필요
- [ ] `backend/src/main/java/com/momentive/backend/address/dto/{AddressRequest,AddressResponse}.java`: 2개 DTO 전체 필드에 `@Schema(description = "...")` 추가
- [ ] `backend/src/main/java/com/momentive/backend/order/controller/OrderController.java`: 클래스 레벨 또는 4개 엔드포인트(`createOrder`, `getOrders`, `getOrder`, `confirmOrder`, `cancelOrder` — 총 5개) 각각에 `@Operation(summary = "...")`와 `@SecurityRequirement` 추가 — `SecurityConfig`상 `/orders/**` 전체 인증 필요
- [ ] `backend/src/main/java/com/momentive/backend/order/dto/{OrderCreateRequest,OrderItemRequest,OrderConfirmRequest,OrderResponse,OrderItemResponse,OrderSummaryResponse,OrderStatusResponse,OutOfStockItem}.java`: 8개 DTO 전체 필드에 `@Schema(description = "...")` 추가
- [ ] `@ApiResponse`(에러 응답 명시)는 spec에서 선택 사항으로 결정했으므로 추가하지 않는다 — 리뷰 시 누락으로 지적하지 않음
- [ ] 검증(자동): `cd backend && ./gradlew build` 통과
- [ ] 검증(수동, 브라우저): `http://localhost:8081/swagger-ui/index.html`에서 (1) Auth/Product/Address/Order 4개 도메인 전체 엔드포인트가 노출되는지, (2) 각 엔드포인트에 `@Operation` summary와 요청/응답 스키마 필드 설명이 표시되는지, (3) Product 전체와 `/auth/signup`·`login`·`logout`·`refresh`에는 자물쇠 아이콘이 없고 Address 전체·Order 전체·`/auth/me`에는 자물쇠 아이콘이 표시되는지 확인

## Phase 3: 컨벤션 문서 및 리뷰어 체크리스트 반영

이 phase가 끝나면 이후 새로운 컨트롤러/DTO를 작성할 때 Swagger 애노테이션 규칙이 `backend/CLAUDE.md`에서 확인 가능하고, `backend-reviewer`가 이 규칙 준수 여부를 자동으로 검증하는 상태가 된다. 코드 변경이 없는 문서 전용 phase다.

- [ ] `backend/CLAUDE.md`에 Swagger/OpenAPI 컨벤션 섹션 추가 — spec의 "컨벤션(backend/CLAUDE.md에 반영될 내용)" 섹션 내용을 그대로 반영: 모든 엔드포인트 `@Operation` summary 필수, 모든 요청/응답 DTO 필드 `@Schema` description 필수, 인증 필요 엔드포인트 `@SecurityRequirement` 필수, `@ApiResponse`는 선택, `Info` 메타정보 규칙(title/version 고정), 그룹 미분리 원칙(admin 도메인 생기면 재검토)
- [ ] `.claude/agents/backend-reviewer.md`의 정적 리뷰 체크리스트에 검증 항목 추가: 새/변경된 컨트롤러 엔드포인트에 `@Operation` summary가 있는지, 새/변경된 요청·응답 DTO 필드에 `@Schema` description이 있는지, 인증이 필요한 엔드포인트에 `@SecurityRequirement`가 있는지
- [ ] 검증: 코드 변경이 없으므로 빌드/테스트 불필요. `backend/CLAUDE.md`와 `.claude/agents/backend-reviewer.md`의 diff에 위 내용이 실제로 반영됐는지 확인하는 것으로 충분
