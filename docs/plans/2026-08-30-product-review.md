---
date: 2026-08-30
feature: product-review
spec: 2026-08-30-product-review.md
status: planned
---

# 상품 리뷰 (조회 + 작성) 플랜

## 개요

`docs/specs/2026-08-30-product-review.md`를 기반으로 백엔드 Review 도메인/평점 집계 → 프론트 상품상세 리뷰 조회·작성 → 프론트 마이페이지 진입점·목록형 화면 평점 교체 → E2E 검증 순서로 진행한다.

cart-order-payment와 동일하게 백엔드 API 계약을 먼저 확정한 뒤 프론트가 그 위에 쌓이는 순서를 따르되, 이번 도메인은 엔티티 1개(Review) + 기존 Product/Order 확장뿐이라 외부 연동이 없는 단순한 구조이므로 백엔드는 하나의 phase로 묶는다. 프론트는 spec의 핵심 시나리오(상품상세 조회+작성/수정/삭제)를 먼저 완결시킨 뒤, 그 위에서 재사용 가능한 폼/버튼 로직을 마이페이지 진입점과 목록형 화면에 이어붙이는 순서로 2개 phase로 나눈다. 목록형 화면의 하드코딩 평점 교체는 마이페이지 진입점 추가와 성격은 다르지만 둘 다 "이미 있는 화면에 값/버튼만 갈아끼우는" 가벼운 마무리 작업이라 같은 phase로 묶는다.

## Phase 1: 백엔드 Review 도메인 + Product 평점 집계

이 phase가 끝나면 Toss/프론트 연동 없이도 curl/Postman으로 리뷰 작성(구매 확인 검증 포함)·조회·수정·삭제와 `Product`의 `averageRating`/`reviewCount` 동기 갱신까지 전체 흐름을 검증할 수 있는 상태가 된다.

- [ ] `Review` 엔티티(`backend/src/main/java/com/momentive/backend/review/domain/Review.java`): `id`, `product`(`@ManyToOne` FK), `user`(`@ManyToOne` FK), `rating`(Integer, 1~5, not null), `text`(String, 10~500자, not null), `createdAt`, `updatedAt`. `@Getter`, `@NoArgsConstructor(access = PROTECTED)`, 정적 팩토리 + `update(rating, text)` 도메인 메서드로 상태 변경(Setter 없음, 기존 `Address`/`Order` 컨벤션과 동일)
- [ ] `Product` 엔티티에 `averageRating`(Double, nullable), `reviewCount`(Integer, not null, default 0) 필드 추가 + 이 값을 갱신하는 도메인 메서드(`updateRatingSummary(averageRating, reviewCount)` 등) 추가
- [ ] `backend/src/main/resources/db/migration/V6__create_review.sql` 신규 작성(최신 마이그레이션은 `V5__create_order_address_and_product_stock.sql`): `review` 테이블 생성(`product_id`, `user_id`, `rating`, `text`, `created_at`, `updated_at`, `UNIQUE(product_id, user_id)` 제약) + `product` 테이블에 `average_rating`(nullable), `review_count`(not null default 0) 컬럼 추가
- [ ] `ReviewRepository`(`backend/src/main/java/com/momentive/backend/review/repository/ReviewRepository.java`): `findByProductIdAndUserId`, `findByProductIdOrderByCreatedAtDesc`(Pageable), 평점 재계산용 집계 쿼리(예: `product_id`별 `AVG(rating)`/`COUNT(*)` — `@Query` 또는 Service에서 조회 후 계산)
- [ ] `OrderItemRepository`에 구매 확인 검증용 쿼리 추가: 특정 `userId`가 `PAID` 상태 주문 중 특정 `productId`를 포함한 주문을 가진 적이 있는지 확인하는 메서드(예: `existsByOrder_User_IdAndOrder_StatusAndProduct_Id`)
- [ ] `ReviewService`(`backend/src/main/java/com/momentive/backend/review/service/ReviewService.java`):
  - `getReviews(productId, page, size, currentUserId)`: 최신순 페이지 조회, 각 항목의 `isMine`은 `currentUserId`와 작성자 일치 여부(비로그인이면 항상 false)
  - `getMyReview(productId, userId)`: 구매 이력 없으면 `CustomException(ErrorCode.PURCHASE_NOT_VERIFIED)`, 있으면 기존 리뷰(`Review` 또는 null) 반환
  - `createReview(productId, userId, request)`: 구매 확인 검증(`PURCHASE_NOT_VERIFIED`), `(product_id, user_id)` 중복이면 `REVIEW_ALREADY_EXISTS`, 저장 후 해당 상품의 `averageRating`/`reviewCount` 동기 재계산
  - `updateReview(productId, reviewId, userId, request)`: 소유권 검증(`FORBIDDEN`, `AddressService.getOwnedAddress`와 동일 패턴), 존재하지 않으면 `REVIEW_NOT_FOUND`, 수정 후 재계산
  - `deleteReview(productId, reviewId, userId)`: 소유권/존재 검증 동일, 삭제 후 재계산
  - `@Transactional` 경계는 Service, 읽기 전용 조회는 `@Transactional(readOnly = true)`
- [ ] `ReviewController`(`backend/src/main/java/com/momentive/backend/review/controller/ReviewController.java`): spec "인터페이스 > API" 섹션 요청/응답 계약 그대로
  - `GET /products/{productId}/reviews`(page 기본 0, size 기본 5) — 인증 불필요(비로그인도 조회 가능, `@CurrentUser`는 optional 처리 필요 — 기존 `@CurrentUser`가 필수 인증 전제라면 비로그인 시 `isMine=false` 처리를 위한 별도 방식 확인 필요, 없으면 nullable 지원 방식 추가)
  - `GET /products/{productId}/reviews/me` — `@SecurityRequirement` 필요(인증 필수)
  - `POST /products/{productId}/reviews`(201) — `@SecurityRequirement` 필요
  - `PATCH /products/{productId}/reviews/{reviewId}`(200) — `@SecurityRequirement` 필요
  - `DELETE /products/{productId}/reviews/{reviewId}`(204) — `@SecurityRequirement` 필요
  - `@CurrentUser`를 쓰는 모든 파라미터에 `@Parameter(hidden = true)` 적용(`backend/CLAUDE.md` Swagger 컨벤션), 모든 엔드포인트에 `@Operation(summary = ...)`
- [ ] `ErrorCode`(`backend/src/main/java/com/momentive/backend/common/exception/ErrorCode.java`)에 `PURCHASE_NOT_VERIFIED(HttpStatus.FORBIDDEN, ...)`, `REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, ...)`, `REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, ...)` 3종 추가. `FORBIDDEN`(타인 리뷰 수정/삭제)은 기존 값 재사용
- [ ] Request/Response DTO 신규(Entity 직접 노출 금지): `ReviewCreateRequest`(`rating`, `text`, Bean Validation `@Min(1) @Max(5)`, `@Size(min=10, max=500)`), `ReviewResponse`, `ReviewListResponse`(`reviews`, `hasNext`, `totalCount`), `MyReviewResponse`(nullable 대응) — 전 필드 `@Schema(description = ...)` 필수
- [ ] 기존 `ProductSummaryResponse`(`backend/src/main/java/com/momentive/backend/product/dto/ProductSummaryResponse.java`)와 `ProductDetailResponse`에 `averageRating`(Double, nullable), `reviewCount`(Integer) 필드 추가, `from(Product)` 정적 팩토리 갱신, `@Schema` 추가
- [ ] `ReviewService` 단위/통합 테스트(`backend/src/test/java/com/momentive/backend/review/ReviewServiceTest.java`): 정상 작성 시 `averageRating`/`reviewCount` 갱신 확인, 구매 이력 없으면 `PURCHASE_NOT_VERIFIED`, 같은 상품 중복 작성 시 `REVIEW_ALREADY_EXISTS`, 본인 아닌 리뷰 수정/삭제 시 `FORBIDDEN`, 삭제 후 같은 상품에 재작성 성공, 목록이 최신순으로 페이지네이션되어 반환됨
- [ ] 검증(자동): `./gradlew build`, `./gradlew test` 통과

## Phase 2: 프론트 상품상세 리뷰 조회 + 작성/수정/삭제

이 phase가 끝나면 브라우저에서 상품상세 화면에 진입해 평점 요약과 리뷰 목록(최신순+더보기)을 볼 수 있고, 구매 이력이 있는 사용자는 리뷰를 작성·수정·삭제까지 끝까지 수행할 수 있는 상태가 된다.

- [ ] `frontend/src/lib/api/reviews.ts` 신규: `getReviews(productId, page, size)`, `getMyReview(productId)`, `createReview(productId, request)`, `updateReview(productId, reviewId, request)`, `deleteReview(productId, reviewId)` — `apiFetch`(`src/lib/api/client.ts`) 경유, `Review`/`ReviewListResponse` 등 타입 명시(`any` 없음)
- [ ] `frontend/src/lib/api/products.ts`의 `ProductSummary`, `ProductDetail` 인터페이스에 `averageRating: number | null`, `reviewCount: number` 필드 추가
- [ ] 리뷰 작성/수정 폼 컴포넌트 신규(`frontend/src/components/commerce/ReviewForm.tsx` — 상품 도메인 전용 폼이라 `commerce/` 배치, React Hook Form + Zod): 별점 선택 UI(1~5 정수, 탭으로 선택), 텍스트 입력(10~500자, 글자 수 카운터), 서버 `fieldErrors`는 `setError`로 매핑
- [ ] `frontend/src/components/commerce/ProductDetailView.tsx`: `MOCK_REVIEWS` 하드코딩 배열과 `Rating value={4.5}`(105행) 제거, `getProduct` 응답의 `averageRating`/`reviewCount`로 평점 요약 표시(리뷰 0개면 평점 영역 생략), `getReviews`로 리뷰 목록 최신순 조회 + "더보기" 버튼으로 다음 페이지 추가 로드
- [ ] "리뷰 쓰기"/"리뷰 수정" 버튼 상태 분기: `getMyReview` 호출 결과로 판단 — 비로그인/구매이력없음(`PURCHASE_NOT_VERIFIED` 에러코드 수신 시 버튼 비활성 또는 숨김 + 안내 문구), 리뷰 없음(버튼 텍스트 "리뷰 쓰기" → 빈 폼), 리뷰 있음(버튼 텍스트 "리뷰 수정" → 기존 값 채운 폼)
- [ ] `frontend/src/components/commerce/ReviewCard.tsx`: props를 실제 API 응답 필드(`authorNickname`, `rating`, `createdAt`, `text`)에 맞게 갱신(기존 `photoCount` prop은 이미지 미지원이므로 항상 0 또는 prop 자체 제거 — spec에서 이미지 첨부는 범위 밖), `isMine`이 true인 리뷰에 수정/삭제 버튼 추가
- [ ] 리뷰 삭제 시 확인 후 `deleteReview` 호출, 성공하면 목록에서 제거하고 평점 요약 갱신(상품 재조회 또는 응답값 반영)
- [ ] 검증(자동): `npm run build`, `npm run lint` 통과
- [ ] 검증(수동, 브라우저): 구매 이력 있는 상품에서 리뷰 작성 → 목록에 즉시 반영 및 평점 갱신 확인, 구매 이력 없는 상품에서 버튼 비활성/안내 확인, 이미 작성한 리뷰의 "리뷰 수정" 버튼 클릭 시 기존 값이 채워진 폼으로 진입하는지 확인, 본인 리뷰 삭제 후 같은 상품에 재작성 가능한지 확인, 리뷰 0개 상품에서 평점 영역이 생략되는지 확인

## Phase 3: 프론트 마이페이지 진입점 + 목록형 화면 평점 교체

이 phase가 끝나면 마이페이지 주문내역 상세에서 구매한 상품별로 리뷰를 작성/수정할 수 있고, 홈/카테고리(검색 경유)/검색/위시리스트 목록 화면이 하드코딩된 평점 대신 실제 평균 평점을 보여주는 상태가 된다.

- [ ] `frontend/src/app/(shell)/mypage/orders/[orderId]/page.tsx`: `order.status === "PAID"`인 주문의 각 `order.items` 항목(상품 카드, `item.productId` 보유 확인됨)마다 리뷰 작성/수정 버튼 추가 — Phase 2의 `ReviewForm`/`getMyReview` 로직 재사용. 기존 하단 고정 취소 버튼(`fixed bottom-0` CTA)과 레이아웃이 겹치지 않도록 리뷰 버튼은 각 상품 카드 내부에 배치(하단 고정 영역에 추가하지 않음)
- [ ] `frontend/src/components/commerce/ProductGridItem.tsx`(56행, 홈 `(shell)/page.tsx`와 검색 `(shell)/search/page.tsx`에서 공용 사용 — 카테고리 화면은 `/search?category=...`로 라우팅만 하고 자체 상품 그리드가 없어 이 컴포넌트 수정만으로 홈/카테고리/검색 3개 화면이 함께 커버됨): `Rating value={4.5}` 하드코딩을 `product.averageRating`으로 교체, `averageRating`이 null이면 `ProductCard`의 `rating` prop 자체를 생략(0점 표시 금지, prop이 optional임을 활용)
- [ ] `frontend/src/app/(shell)/wishlist/page.tsx`(81행): 동일하게 `Rating value={4.5}` 하드코딩을 해당 상품의 `averageRating`으로 교체, null이면 rating 영역 생략
- [ ] 검증(자동): `npm run build`, `npm run lint` 통과
- [ ] 검증(수동, 브라우저): 마이페이지 주문내역 상세에서 `PAID` 주문의 상품별 리뷰 버튼 노출 및 하단 취소 버튼과 레이아웃 충돌 없는지 확인, 홈/검색(카테고리 필터 포함)/위시리스트에서 리뷰 있는 상품은 실제 평점이, 리뷰 없는 상품은 평점 영역이 생략되어 보이는지 확인

## Phase 4: E2E 검증

Phase 3의 frontend-reviewer 승인 직후, `e2e-tester`가 spec `docs/specs/2026-08-30-product-review.md`의 사용자 시나리오(상품상세 리뷰 조회, 구매 확인 기반 작성, 마이페이지 진입점, 수정/삭제, 목록 화면 평점 반영)를 근거로 `docs/e2e/` 규격(`.claude/rules/e2e-format.md`)에 맞춰 케이스를 도출·실행한다.

- [ ] `docs/e2e/YYYY-MM-DD-product-review.md` 작성 및 각 시나리오 실행, 전체 pass 확인 후 이 phase의 체크박스를 체크한다. 실패 시나리오가 있으면 `docs/backlog/` 규격대로 실패를 기록하고 이 phase는 미완료로 남긴다.
