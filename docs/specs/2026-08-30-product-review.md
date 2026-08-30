---
date: 2026-08-30
feature: product-review
status: confirmed
---

# 상품 리뷰 (조회 + 작성)

## 목적 (Why)

상품상세 화면의 `ReviewCard`가 전 상품 공통 `MOCK_REVIEWS`(하드코딩 목업)를 보여주고 있고, 별점(`Rating value={4.5}`)도 홈/카테고리/검색/상품상세/위시리스트 5개 화면 전부에서 모든 상품에 동일하게 하드코딩되어 있다. 실제 구매자에게 오해를 줄 수 있는 상태이며(실 운영 서비스 리스크), 리뷰는 구매 결정에 실질적으로 도움이 되는 핵심 신뢰 요소다. 실제 구매자가 남긴 리뷰를 조회/작성할 수 있게 하고, 하드코딩된 평점을 실제 집계값으로 교체한다.

## 범위 (Scope)

### In Scope
- 구매 확인(verified purchase) 기반 리뷰 작성 — `Order.status = PAID`이며 해당 주문에 리뷰 대상 상품이 포함된 경우에만 작성 가능
- 리뷰 텍스트(10~500자, 필수) + 별점(1~5 정수, 필수)
- 사용자당 상품 1개 리뷰 제한, 삭제 후 재작성 허용
- 작성자 본인의 리뷰 수정/삭제
- 리뷰 작성 진입점 2곳: 상품상세 화면, 마이페이지 주문내역 상세(주문에 포함된 상품마다 개별 버튼)
- 이미 리뷰를 작성한 상품에서 "리뷰 쓰기"를 다시 누르면 수정 폼으로 전환
- 상품상세의 리뷰 목록: 최신순 고정, "더보기" 방식 페이지네이션
- `Product`에 평점 집계 필드(`averageRating`, `reviewCount`) 추가, 리뷰 작성/수정/삭제 시점에 동기 갱신
- 홈/카테고리/검색/상품상세/위시리스트 5개 화면의 하드코딩 평점(`4.5`)을 실제 집계값으로 교체
- 리뷰 작성자 표시: `User.nickname` 그대로 노출(마스킹 없음)

### Out of Scope
- 리뷰 이미지 첨부 — 이미지 업로드 인프라(스토리지 연동 등) 자체가 프로젝트에 없어 별도 spec으로 분리
- 리뷰 신고/모더레이션(숨김, 차단) — admin 화면 자체가 프로젝트 범위 밖(`backend/CLAUDE.md` 기존 원칙)
- 별점 0.5 단위 입력 — 입력은 정수 단위, 표시(반올림 별 개수)는 기존 `ReviewCard` 로직 유지
- 리뷰 정렬 옵션(평점순 등) — 최신순 고정만 지원
- 관리자에 의한 리뷰 삭제/편집

## 사용자 시나리오

### 1. 상품상세에서 리뷰 조회
1. 상품상세 화면에 진입하면 평균 평점(`averageRating`)과 리뷰 개수(`reviewCount`)가 상단에 표시된다.
2. 리뷰 목록이 최신순으로 일부(5개) 노출되고, "더보기"를 누르면 다음 페이지가 추가로 로드된다.
3. 리뷰가 하나도 없으면 빈 상태 안내를 보여준다.

### 2. 상품상세에서 리뷰 작성
1. 로그인한 사용자가 해당 상품을 `PAID` 상태로 구매한 적이 있으면 "리뷰 쓰기" 버튼이 활성화된다.
2. 구매 이력이 없으면 버튼이 비활성화되거나 숨겨지고, 안내 문구를 보여준다(비로그인도 동일하게 처리).
3. 이미 이 상품에 리뷰를 작성한 경우, 버튼을 누르면 기존 내용이 채워진 수정 폼으로 진입한다.
4. 별점(1~5)과 텍스트(10~500자)를 입력하고 제출하면 리뷰가 등록되고, 해당 상품의 `averageRating`/`reviewCount`가 즉시 갱신된다.

### 3. 마이페이지 주문내역에서 리뷰 작성
1. 마이페이지 주문내역 상세(`/mypage/orders/[orderId]`)에서, `PAID` 상태인 주문에 포함된 각 상품 항목마다 "리뷰 쓰기" 버튼이 개별로 노출된다.
2. 이미 리뷰를 작성한 상품은 버튼 라벨/동작이 "리뷰 수정"으로 바뀐다.
3. 버튼을 누르면 상품상세와 동일한 작성/수정 폼으로 진입한다.

### 4. 리뷰 수정/삭제
1. 본인이 작성한 리뷰에는 수정/삭제 버튼이 보인다(다른 사용자의 리뷰에는 보이지 않는다).
2. 수정하면 텍스트/별점이 갱신되고 평점 집계도 다시 계산된다.
3. 삭제하면 리뷰가 사라지고 평점 집계가 다시 계산되며, 이후 같은 상품에 대해 다시 리뷰를 작성할 수 있다.

### 5. 목록 화면의 평점 표시
1. 홈/카테고리/검색/위시리스트에서 상품 카드에 표시되는 별점이 각 상품의 실제 `averageRating`으로 표시된다.
2. 리뷰가 없는 상품은 평점 표시를 생략하거나 "리뷰 없음"으로 표시한다(0점으로 표시하지 않는다).

## 인터페이스

### API

**`GET /products/{productId}/reviews`** (리뷰 목록, 최신순, 더보기 페이지네이션)
- Query: `page`(기본 0), `size`(기본 5)
- Response 200: `{ reviews: [{ reviewId, authorNickname, rating, text, createdAt, updatedAt, isMine }], hasNext, totalCount }`
- `isMine`은 로그인 사용자 본인 작성 여부(수정/삭제 버튼 노출 판단용), 비로그인이면 항상 false

**`GET /products/{productId}/reviews/me`** (내가 이 상품에 쓴 리뷰 조회 — 작성/수정 폼 초기값 및 버튼 상태 판단용)
- Response 200: `{ reviewId, rating, text } | null`
- 에러: `PURCHASE_NOT_VERIFIED`(403, 구매 이력 없어 작성 자격 자체가 없는 경우 — 폼 진입 전 버튼 노출 여부 판단에 사용)

**`POST /products/{productId}/reviews`** (리뷰 작성)
- Request: `{ rating: number(1~5), text: string(10~500자) }`
- Response 201: `{ reviewId, rating, text, createdAt }`
- 에러: `VALIDATION_FAILED`(400), `PURCHASE_NOT_VERIFIED`(403, `PAID` 주문에 해당 상품 없음), `REVIEW_ALREADY_EXISTS`(409, 이미 작성한 상품)

**`PATCH /products/{productId}/reviews/{reviewId}`** (리뷰 수정)
- Request: `{ rating: number(1~5), text: string(10~500자) }`
- Response 200: `{ reviewId, rating, text, updatedAt }`
- 에러: `VALIDATION_FAILED`(400), `REVIEW_NOT_FOUND`(404), `FORBIDDEN`(403, 타인 리뷰)

**`DELETE /products/{productId}/reviews/{reviewId}`**
- Response 204
- 에러: `REVIEW_NOT_FOUND`(404), `FORBIDDEN`(403, 타인 리뷰)

**기존 `GET /products`, `GET /products/{id}` 응답 변경**: `averageRating`(nullable, 리뷰 없으면 null), `reviewCount` 필드 추가

### 화면

**상품상세 (`products/[id]/page.tsx`, `ProductDetailView.tsx`)**
- 평점 요약(평균/개수) + 리뷰 목록(최신순, 더보기) — `MOCK_REVIEWS` 제거, 실 API 연동
- "리뷰 쓰기"/"리뷰 수정" 버튼: 구매 확인 여부 및 기존 리뷰 존재 여부에 따라 상태 분기
- 리뷰 작성/수정 폼(별점 선택 UI + 텍스트 입력, 글자 수 카운터)
- 본인 리뷰에는 수정/삭제 버튼

**마이페이지 주문내역 상세 (`mypage/orders/[orderId]/page.tsx`)**
- `PAID` 주문의 상품 항목마다 "리뷰 쓰기"/"리뷰 수정" 버튼 추가

**목록형 화면 (홈/카테고리/검색/위시리스트)**
- 상품 카드의 `Rating value={4.5}` 하드코딩을 각 상품 응답의 `averageRating`으로 교체, 리뷰 없으면 별점 영역 생략

### 데이터 모델

**`Review`** (신규)
- `id` (PK)
- `product_id` (FK → Product)
- `user_id` (FK → User)
- `rating` (Integer, 1~5, not null)
- `text` (String, 10~500자, not null)
- `created_at`, `updated_at`
- 제약: `(product_id, user_id)` unique — 사용자당 상품 1개 리뷰 제한

**`Product` 변경**
- `average_rating` (Double, nullable, 리뷰 없으면 null)
- `review_count` (Integer, not null, default 0)

## 수용 기준 (Acceptance Criteria)

- [ ] `PAID` 상태로 해당 상품을 구매한 사용자만 리뷰를 작성할 수 있고, 구매 이력이 없으면 `PURCHASE_NOT_VERIFIED`로 거부된다
- [ ] 별점(1~5 정수) 또는 텍스트(10~500자)를 만족하지 않으면 `VALIDATION_FAILED`로 거부된다
- [ ] 같은 사용자가 같은 상품에 리뷰를 두 번째로 작성하려 하면 `REVIEW_ALREADY_EXISTS`로 거부된다
- [ ] 리뷰 삭제 후에는 같은 사용자가 같은 상품에 다시 리뷰를 작성할 수 있다
- [ ] 본인이 작성한 리뷰만 수정/삭제할 수 있고, 타인의 리뷰를 수정/삭제 시도하면 `FORBIDDEN`으로 거부된다
- [ ] 리뷰 작성/수정/삭제 시 해당 상품의 `averageRating`/`reviewCount`가 즉시 재계산되어 반영된다
- [ ] 상품상세 화면에서 리뷰 목록이 최신순으로 노출되고, "더보기"로 다음 페이지를 불러올 수 있다
- [ ] 이미 리뷰를 작성한 상품에서 "리뷰 쓰기" 버튼을 누르면 기존 내용이 채워진 수정 폼으로 진입한다
- [ ] 마이페이지 주문내역 상세에서 `PAID` 주문에 포함된 상품마다 리뷰 작성/수정 버튼이 개별로 노출된다
- [ ] 홈/카테고리/검색/상품상세/위시리스트 5개 화면의 상품 카드가 하드코딩된 `4.5` 대신 실제 `averageRating`을 표시하며, 리뷰가 없는 상품은 평점 영역이 생략된다
- [ ] 리뷰 목록/작성 폼에 표시되는 작성자 이름은 `User.nickname` 그대로 노출된다(마스킹 없음)
