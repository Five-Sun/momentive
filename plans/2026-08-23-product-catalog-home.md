---
date: 2026-08-23
feature: product-catalog-home
spec: 2026-08-18-product-catalog.md
status: planned
---

# 상품 목록 조회 → 홈 화면 플랜

## 개요

이 플랜은 `specs/2026-08-18-product-catalog.md`(API/데이터 모델/수용 기준)와 `specs/2026-08-23-home-screen.md`(홈 화면의 실제 시각 디자인)를 함께 실행하는 단위다. 두 스펙은 "홈(`/`) = 상품 목록"이라는 동일한 기능 계약을 공유하며, home-screen spec 자체가 product-catalog spec의 API 계약을 그대로 재사용하도록 정의되어 있어 서로 독립적으로 구현할 수 없다. 하나의 plan으로 묶어 phase 경계를 API(백엔드) → 화면(프론트)으로 자연스럽게 나눈다.

Phase는 다음 순서로 진행한다.

1. **백엔드 먼저**: 프론트(목록 그리드, 상세 페이지)가 실제 데이터에 의존하므로 API와 시딩 데이터가 먼저 존재해야 프론트 작업을 눈으로 검증할 수 있다. 이번이 첫 기능이라 `CustomException(ErrorCode)` 공통 처리 골격도 이 phase에서 최초로 도입한다.
2. **홈 화면(목록+그리드+무한스크롤)**: home-screen spec의 대부분과 product-catalog spec의 목록 관련 수용 기준을 함께 만족시키는 화면. 상품 카드 클릭 시 이동할 상세 페이지는 아직 없어도 되므로(링크만 걸어두고 다음 phase에서 실제 페이지 완성) 목록 화면을 먼저 완결된 마일스톤으로 검증한다.
3. **상품 상세 페이지**: 목록에서 분리 가능한 별도 라우트이자 별도 화면이므로 마지막 phase로 독립시킨다. 홈 화면의 카드 클릭 동작과 연결해 전체 사용자 시나리오(목록 → 상세)를 완성한다.

같은 feature 또는 category의 과거 backlog 실패 항목은 없음 (`.claude/backlog/` 비어 있음).

## Phase 1: 백엔드 — Product 도메인 및 조회 API

이 phase가 끝나면 `GET /products`, `GET /products/{id}`가 시딩된 더미 데이터를 기준으로 정상 동작하고, 테스트와 curl로 목록/상세/404 케이스를 확인할 수 있는 상태가 된다.

- [ ] `backend/src/main/java/com/momentive/backend/product/` 하위에 domain/repository/service/controller/dto 구조로 `Product`, `ProductImage` JPA 엔티티와 연관관계(1:N) 구현
- [ ] 공통 예외 처리 골격(`ErrorCode`, `CustomException`) 최초 도입 및 `PRODUCT_NOT_FOUND` 코드 추가
- [ ] `GET /products` 구현: `page`/`size`(기본값 0/20) 파라미터, 최신 등록순 정렬, 페이지네이션 메타데이터 포함 응답, 상품 0개 시 빈 배열 + 200
- [ ] `GET /products/{id}` 구현: 상세 응답(이름/설명/가격/할인가/품절여부/이미지 전체 순서대로), 존재하지 않는 id는 404 + `PRODUCT_NOT_FOUND`
- [ ] 더미 상품 10~20개 시딩(일부 품절, 일부 할인가 있음, 일부는 깨진 이미지 URL 포함해 프론트 플레이스홀더 검증에 재사용 가능하도록 구성)
- [ ] `ProductControllerTest` 작성: 목록 페이지네이션, 빈 목록 200, 상세 404 케이스 커버
- [ ] 검증: `./gradlew test` 통과, curl로 `GET /products`(목록/빈 목록 상황), `GET /products/{id}`(정상/404) 수동 확인 — product-catalog spec AC 1, 2, 3, 7 충족

## Phase 2: 프론트 — 홈 화면 (목록 + 그리드 + 무한스크롤)

이 phase가 끝나면 `/`에 접속했을 때 헤더와 상품 그리드가 실제 API 데이터로 렌더링되고, 스크롤 시 다음 페이지가 이어붙으며, 로딩/빈 상태/이미지 실패가 모두 시각적으로 확인 가능한 상태가 된다.

- [ ] `frontend/src/lib/api/products.ts` 등 API fetch wrapper 최초 도입 (`NEXT_PUBLIC_API_BASE_URL` 사용, 목록/상세 조회 함수 포함)
- [ ] `frontend/src/app/page.tsx` 교체: 헤더(로고 + `SearchInput` + 히어로 문구 + 장바구니 아이콘 버튼) 구현, `SearchInput` 클릭 시 `/search`, 장바구니 아이콘 클릭 시 `/cart`로 이동(대상 화면 없어 404 허용)
- [ ] 상품 그리드 구현: 기존 `ProductCard`, `Rating`, `Badge` 컴포넌트 재사용, 반응형 열 수(모바일 2 ~ 데스크톱 5), 이미지는 `<img onError>`로 `surface-strong` + 상품명 플레이스홀더 전환, 평점은 더미 표시
- [ ] 스켈레톤 컴포넌트 구현 및 초기 로드/추가 페이지 로드 중 노출
- [ ] 빈 상태 문구("아직 준비된 상품이 없어요" 등) 구현
- [ ] IntersectionObserver 기반 커스텀 훅으로 무한스크롤 구현 (외부 라이브러리 없이, `page+1`/`size=20` 자동 fetch 후 이어붙임)
- [ ] 검증: `npm run build`, `npm run lint` 통과, 브라우저로 그리드 반응형/무한스크롤/빈 상태/스켈레톤/검색바·장바구니 링크(404 허용) 수동 확인 — home-screen spec AC 대부분 및 product-catalog spec AC 4, 5 충족

## Phase 3: 프론트 — 상품 상세 페이지

이 phase가 끝나면 홈 화면의 상품 카드를 클릭해 상세 페이지로 이동할 수 있고, 존재하지 않는 id는 404로 처리되어 목록→상세 전체 시나리오가 완결된다.

- [ ] `frontend/src/app/products/[id]/page.tsx` 구현: 이미지 갤러리(전체 이미지, 등록 순서대로), 상품명/설명/가격(할인가 병기)/품절 여부 표시, 장바구니/구매 등 액션 버튼 없음
- [ ] 존재하지 않는 id로 접근 시 Next.js `notFound()`를 이용한 404 처리
- [ ] Phase 2의 상품 카드에 상세 페이지로 이동하는 `<Link href="/products/{id}">` 연결
- [ ] 검증: `npm run build` 통과, 브라우저로 정상 상세 페이지 및 존재하지 않는 id 접근 시 404 수동 확인 — product-catalog spec AC 6, home-screen spec 마지막 AC 충족
