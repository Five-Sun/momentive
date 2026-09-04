---
date: 2026-09-04
feature: admin-product-management
status: confirmed
---

# 관리자 기반 및 상품 관리

## 목적 (Why)

실제 고객이 있는 서비스인데 **상품을 등록할 방법이 없다.** `ProductController`에는 `GET` 2개뿐이고 상품 데이터는 `V2__seed_product.sql`의 15개 시드가 전부다. 신상품을 올리거나 재입고를 반영하려면 매번 flyway 마이그레이션을 작성하고 Railway에 재배포해야 한다. 이것이 현재 운영을 막고 있는 가장 큰 병목이다.

이 병목을 풀려면 세 가지가 함께 해결되어야 한다.

**1. 관리자 인가 체계가 아예 없다.** `Role.ADMIN`은 enum 값으로만 존재하고 백엔드 전체에서 참조가 0건인 죽은 코드다. `User.createUser()`는 항상 `Role.USER`로 고정해 ADMIN을 만들 경로 자체가 없고, `JwtTokenProvider`는 권한을 클레임에 싣지 않으며, `JwtAuthenticationFilter`는 모든 요청에 `ROLE_USER`를 하드코딩한다. `SecurityConfig`에 `hasRole` 사용도 0건이다. 관리자 API를 만들려면 인가를 바닥부터 세워야 한다.

**2. 상품이 늘어나는 순간 검색이 깨진다.** 백엔드에 검색 API가 없어 프론트가 `getProducts(0, 100)`으로 100개를 받아 `name.includes()`로 거르고 있다. 상품이 100개를 넘으면 그 뒤 상품은 검색에 영원히 잡히지 않는다. 위시리스트도 같은 캡을 쓴다. 상품 등록을 열어놓고 이걸 두면 "등록했는데 검색이 안 된다"가 곧바로 터진다.

**3. 사이즈별 재고가 없다.** `Product.stock`은 단일 정수이고 상품상세의 S/M/L/XL은 프론트 하드코딩(`ProductDetailView.tsx:31`)이다. 그래서 재고가 1개 남아도 네 사이즈 전부 구매 가능하게 보이고, 간식·목줄·방석에도 사이즈 선택이 뜬다. 강아지 의류 쇼핑몰에서 "M만 품절"은 예외가 아니라 일상인데 이를 표현할 수 없다. 주문 데이터가 더 쌓인 뒤에 바꾸면 이관 비용이 지금보다 확실히 커진다.

## 범위 (Scope)

### In Scope

- **관리자 인가 기반** — JWT에 `role` 클레임 추가, `JwtAuthenticationFilter`가 실제 권한 부여, `/admin/**`에 `hasRole("ADMIN")`, `/auth/me` 응답에 `role` 노출, 환경변수 기반 관리자 승격 마이그레이션
- **`ProductVariant` 도입** — 사이즈별 재고 모델 신설, 기존 상품 15개 이관, 재고 차감·복원 및 낙관적 락을 variant 기준으로 이전
- **`Product` 판매 상태 모델** — `soldOut` boolean을 `status` enum(`ON_SALE`/`HIDDEN`/`DELETED`)으로 전환, 품절은 재고 합에서 파생 판정
- **관리자 API** — 상품 CRUD(목록·상세·등록·수정·soft delete), Cloudinary 업로드 서명 발급
- **상품 검색 API** — `GET /products`에 `q` 파라미터 추가(`name` LIKE), 기존 `category`/`sort`/페이지네이션과 조합
- **관리자 화면 2개** — 상품 목록, 상품 등록/수정 폼(재고·이미지 포함). `/admin` 라우트, `(shell)` 밖
- **고객 화면 반영** — 상품상세의 사이즈 선택을 실제 variant로 연동(사이즈별 품절 표시, 사이즈 없는 상품은 선택 UI 숨김), `/search`를 서버 검색으로 전환, 장바구니에 `variantId` 도입

### Out of Scope

- **주문 배송상태·송장 관리** — 후속 spec(B)에서 다룬다. `OrderStatus`는 현행 4종(`PENDING`/`PAID`/`FAILED`/`CANCELLED`) 유지
- **쿠폰 발급 API** — 후속 spec(B). 쿠폰은 계속 flyway 시드로만 정의된다
- **재고 일괄 조정 화면** — 목록에서 여러 상품 재고를 표로 수정하는 화면. 상품이 수십 개로 늘어 실제로 불편해지면 별도 spec
- **관리자 승격 API·권한 관리 화면** — 승격은 마이그레이션으로만. 공격 표면을 늘리지 않는다
- **비밀번호 변경 API** — 관리자 계정도 일반 회원가입 계정이므로 기존 제약을 그대로 따른다
- **Cloudinary 고아 파일 정리** — 상품에서 이미지를 빼도 원본은 남긴다
- **기존 `order_item`의 variant 소급 매핑** — 과거 주문의 `size` 문자열이 어느 variant인지 알 방법이 없다
- **`description` 전문검색·최근 검색어·인기 검색어** — 검색은 `name` LIKE만. 최근/인기 검색어는 현행(localStorage/하드코딩) 유지
- **위시리스트의 100개 캡** — 검색과 별개 경로라 이번에 다루지 않는다
- **적립금·배송조회** — 별개 항목
- **상품 옵션 중 사이즈 외의 축**(색상 등) — variant는 사이즈 한 축만 갖는다

## 사용자 시나리오

### A. 관리자 승격 (최초 1회)

1. 운영자가 평소처럼 일반 회원가입으로 계정을 만든다
2. 배포 환경에 환경변수 `MOMENTIVE_ADMIN_EMAIL`을 그 이메일로 설정한다
3. 배포 시 flyway가 승격 마이그레이션을 실행해 해당 계정의 `role`을 `ADMIN`으로 올린다
4. 다시 로그인하면(새 access token에 `role` 클레임이 실린다) `/admin` 접근이 가능해진다
   - **예외**: 해당 이메일의 계정이 아직 없으면 마이그레이션은 조용히 no-op이다. 회원가입을 먼저 해야 한다
   - **예외**: 이미 로그인 상태라면 기존 access token에 `role`이 없어 최대 30분간 관리자로 인식되지 않는다. 로그아웃 후 재로그인하면 즉시 반영된다

### B. 상품 등록

1. 관리자가 `/admin`에서 "상품 등록"을 누른다
2. 이름·설명·정가·할인가·카테고리를 입력한다
3. 이미지를 최대 5장 올린다. 파일은 브라우저에서 Cloudinary로 직접 업로드되고, 화면에는 업로드된 순서대로 미리보기가 쌓인다. 순서가 곧 `displayOrder`다
4. 사이즈와 재고를 입력한다
   - 의류처럼 사이즈가 있는 상품: `S`/`M`/`L` 등 사이즈 이름과 재고 수량을 행 단위로 추가한다
   - 간식·목줄처럼 사이즈가 없는 상품: 사이즈를 비우고 재고만 입력한다(내부적으로 `size = null`인 단일 variant가 된다)
5. 저장하면 상품이 `ON_SALE` 상태로 생성되고 고객 화면에 즉시 노출된다
   - **예외**: 이미지 업로드가 실패하면 해당 장만 실패 표시되고 나머지는 유지된다. 이미지가 0장이어도 저장은 가능하다(고객 화면은 기존 플레이스홀더로 대체)
   - **예외**: variant가 0개면 저장할 수 없다. 최소 1개가 필요하다
   - **예외**: 같은 상품 안에 사이즈 이름이 중복되면 저장할 수 없다

### C. 재입고 / 수정

1. 관리자가 `/admin` 목록에서 상품을 선택해 수정 폼을 연다
2. 사이즈별 재고 수치를 고쳐 저장한다. 사이즈 행을 추가하거나 제거할 수도 있다
   - **예외**: 이미 주문에 사용된 variant는 제거할 수 없다. 재고를 0으로 두는 방식으로 처리한다

### D. 판매 중단 / 삭제

1. 관리자가 상품을 `HIDDEN`으로 바꾸면 고객 화면(목록·검색·상세)에서 사라진다. 재고는 그대로 남아 다시 `ON_SALE`로 되돌릴 수 있다
2. `DELETED`로 바꾸면 관리자 목록의 기본 필터에서도 빠진다
3. 두 경우 모두 **기존 주문 이력에는 계속 정상적으로 보인다** — `order_item`이 상품을 참조하고 있어 실제 행 삭제는 하지 않는다

### E. 고객 — 사이즈별 품절

1. 고객이 상품상세에 들어가면 그 상품에 실제로 등록된 사이즈만 보인다
2. 재고가 0인 사이즈는 선택할 수 없는 상태로 표시된다
3. 사이즈가 없는 상품은 사이즈 선택 영역 자체가 나타나지 않고 바로 장바구니에 담을 수 있다
4. 모든 사이즈의 재고 합이 0이면 상품 전체가 품절로 표시된다
   - **예외**: 담아둔 사이즈가 그 사이에 품절되면 주문 생성 시 기존 `OUT_OF_STOCK` 흐름으로 처리된다
   - **예외**: 이 변경 이전에 저장된 장바구니 항목은 `variantId`가 없어 재고를 검증할 수 없다. 로드 시 조용히 버린다

### F. 고객 — 검색

1. 고객이 `/search`에서 검색어를 입력하면 서버가 `name` 부분일치로 검색해 페이지 단위로 돌려준다
2. 자동완성도 같은 API를 작은 `size`로 호출해 채운다
3. 정렬·카테고리 필터는 검색어와 함께 적용된다
   - **예외**: 검색 API 호출이 실패하면 "검색 결과가 없어요"로 뭉개지 않고 실패했음을 알린다

## 인터페이스

### API

인증이 필요한 엔드포인트는 기존과 동일하게 httpOnly 쿠키의 access token으로 인증한다. `/admin/**`은 `ADMIN` 권한이 없으면 `403 FORBIDDEN`.

#### 관리자 — 상품

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/admin/products` | 상품 목록. `page`, `size`, `status`(기본 `ON_SALE,HIDDEN`), `q` |
| `GET` | `/admin/products/{id}` | 상품 상세 (variant·이미지 포함, `DELETED`도 조회 가능) |
| `POST` | `/admin/products` | 상품 등록 |
| `PUT` | `/admin/products/{id}` | 상품 수정 (variant·이미지 전체 교체) |
| `DELETE` | `/admin/products/{id}` | soft delete (`status = DELETED`) |

`POST` / `PUT` 요청 본문:

```json
{
  "name": "겨울 패딩",
  "description": "...",
  "price": 28000,
  "discountPrice": 22400,
  "category": "OUTER",
  "status": "ON_SALE",
  "imageUrls": ["https://res.cloudinary.com/...", "..."],
  "variants": [
    { "id": 12, "size": "S", "stock": 10 },
    { "id": null, "size": "M", "stock": 5 }
  ]
}
```

- `variants[].id`가 `null`이면 신규, 값이 있으면 기존 variant 갱신
- 요청에 없는 기존 variant는 삭제 대상. 단 주문에 사용된 적이 있으면 `VARIANT_IN_USE`로 거부
- `imageUrls` 배열 순서가 `displayOrder`

에러 코드:

| 코드 | status | 상황 |
|---|---|---|
| `FORBIDDEN` | 403 | `ADMIN` 권한 없이 `/admin/**` 접근 |
| `PRODUCT_NOT_FOUND` | 404 | 없는 상품 |
| `VALIDATION_FAILED` | 400 | 형식 검증 실패 (`fieldErrors` 포함) |
| `VARIANT_REQUIRED` | 400 | variant가 0개 |
| `DUPLICATE_VARIANT_SIZE` | 400 | 한 상품 안에 같은 사이즈 이름 중복 |
| `VARIANT_IN_USE` | 400 | 주문에 사용된 variant 삭제 시도 |
| `VARIANT_NOT_FOUND` | 400 | 요청한 `variantId`가 해당 상품에 존재하지 않음 (구현 중 보강) |
| `IMAGE_LIMIT_EXCEEDED` | 400 | 이미지 5장 초과 |

#### 관리자 — 이미지 업로드 서명

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/admin/images/signature` | Cloudinary signed upload용 서명 발급 |

응답: `{ "signature": "...", "timestamp": 1757000000, "apiKey": "...", "cloudName": "...", "folder": "momentive/products" }`

파일 바이트는 백엔드를 거치지 않는다. 브라우저가 이 서명으로 Cloudinary에 직접 업로드하고, 반환된 secure URL만 상품 저장 시 `imageUrls`로 보낸다. Cloudinary API secret은 환경변수로 주입하며 코드에 두지 않는다.

#### 고객 — 변경되는 기존 API

| 메서드 | 경로 | 변경 |
|---|---|---|
| `GET` | `/products` | **`q` 파라미터 추가** — `name` 부분일치(대소문자 무시). 기존 `page`/`size`/`category`/`sort`와 조합. `status = ON_SALE`만 노출 |
| `GET` | `/products/{id}` | 응답에 **`variants` 배열 추가**(`variantId`, `size`, `stock`, `soldOut`). `soldOut`은 재고 합 0에서 파생. `ON_SALE`이 아니면 404 |
| `GET` | `/auth/me` | 응답에 **`role` 추가** |
| `POST` | `/orders` | `items[].size` 대신 **`items[].variantId`**를 받는다. 재고 차감·복원이 variant 기준으로 동작 |

### 화면

#### `/admin` — 상품 목록 (신규)

- `(shell)` 밖, 데스크톱 폭 레이아웃. 모바일 프레임·하단탭 없음
- 표 형태: 썸네일 / 이름 / 카테고리 / 가격 / 재고 합 / 상태 / 수정 링크
- 상단에 검색 입력과 상태 필터, "상품 등록" 버튼
- 페이지네이션
- 표 컴포넌트는 기존에 없는 패턴이므로 `/admin` 안의 로컬 컴포넌트로 두고 공용 `src/components/`에는 올리지 않는다

#### `/admin/products/new`, `/admin/products/[id]` — 등록·수정 폼 (신규)

- 기존 `Button`, `TextField` 등 공용 컴포넌트와 디자인 토큰을 그대로 재사용
- 이미지 영역: 파일 선택 → Cloudinary 직접 업로드 → 미리보기 나열, 순서 변경·개별 삭제, 최대 5장
- variant 영역: 사이즈 이름 + 재고 수량 행을 추가/삭제. 사이즈를 비우면 사이즈 없는 상품
- React Hook Form + Zod, 서버 `fieldErrors` 인라인 매핑 (기존 컨벤션)

#### `/admin/layout.tsx` — 접근 보호 (신규)

- 레이아웃에서 한 번만 검사한다. 화면이 늘 때마다 검사를 복붙하면 언젠가 빠뜨린다
- `AuthProvider`의 `user.role`이 `ADMIN`이 아니면 홈으로 리다이렉트
- **프론트 검사는 UX 차원이고 실제 방어선은 백엔드 `hasRole("ADMIN")`이다**

#### 상품상세 (`ProductDetailView`) — 변경

- 하드코딩된 `SIZES = ["S","M","L","XL"]` 제거, `product.variants`로 대체
- 재고 0인 사이즈는 선택 불가 표시
- variant가 1개이고 `size`가 `null`이면 사이즈 선택 영역과 "사이즈 가이드" 링크를 렌더링하지 않는다
- 장바구니 담기 시 `variantId`를 함께 저장

#### `/search` — 변경

- 클라이언트 필터링 제거, `q` 파라미터로 서버 검색
- 자동완성은 같은 API를 작은 `size`로 호출
- 검색 실패 시 "검색 결과가 없어요"와 구분되는 실패 안내
- 최근 검색어·인기 검색어는 현행 유지

#### 장바구니 (`lib/storage/cart.ts`) — 변경

- `CartItem`에 `variantId` 추가
- 구 형식 항목(`variantId` 없음)은 로드 시 버린다. 재고를 검증할 수 없는 유령 항목을 남기는 것보다 안전하다

### 데이터 모델

#### `ProductVariant` (신규)

| 필드 | 타입 | 제약 |
|---|---|---|
| `id` | Long | PK |
| `product` | Product | FK, not null |
| `size` | String | nullable (사이즈 없는 상품), 상품 내 유일 |
| `stock` | Integer | not null, >= 0 |
| `version` | Long | `@Version` 낙관적 락 |

- 모든 상품은 **최소 1개 variant**를 갖는다. 사이즈가 없는 상품은 `size = null`인 단일 variant로 표현해 재고 로직을 한 갈래로 유지한다
- 재고 차감·복원과 재시도(2회 후 `STOCK_CONFLICT`)를 이 엔티티 기준으로 수행한다. 서로 다른 사이즈를 동시에 주문할 때 불필요한 충돌이 사라진다

#### `Product` (변경)

- `stock` 제거 → `ProductVariant`로 이동
- `soldOut` 제거 → `status` enum(`ON_SALE` / `HIDDEN` / `DELETED`)으로 대체
- `version`은 유지 (이름·가격 동시 수정 대비). 재고 재시도에는 더 이상 쓰지 않는다
- 품절 여부는 컬럼이 아니라 **variant 재고 합에서 파생**한다. 관리자 의도(`status`)와 사실(재고)을 분리한다

#### `OrderItem` (변경)

- `variant_id` 컬럼을 **nullable로** 추가. 신규 주문부터 채운다
- 기존 행은 건드리지 않고 `size` 문자열을 그대로 보존한다 — 과거 주문의 "S"가 어느 variant인지 정할 방법이 없고, 억지로 매핑하면 없는 사실을 만들어내는 셈이다

#### 마이그레이션 (현재 `V12`까지 사용됨)

| 파일 | 내용 |
|---|---|
| `V13__promote_admin_user.sql` | `UPDATE users SET role = 'ADMIN' WHERE email = '${adminEmail}'`. 값은 flyway placeholder로 주입하며 **실제 이메일은 리포지토리에 남기지 않는다**(리포지토리가 public). `application.yml`의 `spring.flyway.placeholders.adminEmail`이 환경변수 `MOMENTIVE_ADMIN_EMAIL`을 참조 |
| `V14__create_product_variant.sql` | `product_variant` 테이블 생성, 기존 상품 15개를 `size = null` 단일 variant로 이관하며 `product.stock` 값을 그대로 옮김. 이후 `product.stock` 제거 |
| `V15__replace_product_sold_out_with_status.sql` | `product.status` 추가, `sold_out = TRUE`였던 행은 재고 0으로 이미 이관되었으므로 전부 `ON_SALE`로 두고 `sold_out` 제거 |
| `V16__add_variant_id_to_order_item.sql` | `order_item.variant_id` nullable 추가 |

## 수용 기준 (Acceptance Criteria)

### 인가

- [ ] 로그인 시 발급되는 access token에 `role` 클레임이 포함된다
- [ ] `JwtAuthenticationFilter`가 토큰의 `role`로 권한을 부여한다 (`ROLE_USER` 하드코딩 제거)
- [ ] `ADMIN`이 아닌 계정으로 `/admin/**` API를 호출하면 `403 FORBIDDEN`이 기존 `ErrorResponse` 포맷으로 반환된다
- [ ] 비로그인 상태로 `/admin/**` API를 호출하면 `401`이 반환된다
- [ ] `GET /auth/me` 응답에 `role`이 포함된다
- [ ] `ADMIN`이 아닌 계정으로 `/admin` 화면에 접근하면 홈으로 리다이렉트된다
- [ ] `V13` 마이그레이션이 환경변수로 주입된 이메일의 계정을 `ADMIN`으로 승격하며, 해당 계정이 없으면 오류 없이 no-op이다
- [ ] 마이그레이션 SQL 파일과 `application.yml` 어디에도 실제 이메일 문자열이 없다

### 재고 모델

- [ ] 기존 상품 15개가 `size = null` 단일 variant로 이관되고 재고 수치가 보존된다
- [ ] 기존 `order_item` 행의 `size` 문자열이 그대로 남아 있고 주문 상세 화면이 정상 표시된다
- [ ] 재고 차감·복원이 `ProductVariant` 기준으로 동작하고, 충돌 시 2회 재시도 후 `STOCK_CONFLICT`를 반환한다
- [ ] 서로 다른 사이즈를 동시에 주문해도 낙관적 락 충돌이 발생하지 않는다
- [ ] 상품의 품절 여부가 variant 재고 합으로 파생 판정된다 (`soldOut` 컬럼 없음)

### 관리자 API

- [ ] 상품을 등록하면 variant와 이미지가 함께 저장되고 고객 목록에 즉시 노출된다
- [ ] variant 없이 저장을 시도하면 `VARIANT_REQUIRED`가 반환된다
- [ ] 한 상품에 같은 사이즈 이름을 중복 입력하면 `DUPLICATE_VARIANT_SIZE`가 반환된다
- [ ] 주문에 사용된 variant를 삭제하려 하면 `VARIANT_IN_USE`가 반환된다
- [ ] 이미지를 6장 이상 보내면 `IMAGE_LIMIT_EXCEEDED`가 반환된다
- [ ] `DELETE`가 행을 지우지 않고 `status`를 `DELETED`로 바꾼다
- [ ] `HIDDEN` 또는 `DELETED` 상품이 고객 목록·검색·상세에서 제외되고, 상세는 404를 반환한다
- [ ] `HIDDEN`/`DELETED` 상품이 포함된 기존 주문 상세가 정상적으로 표시된다
- [ ] `POST /admin/images/signature`가 유효한 Cloudinary 서명을 반환하고, API secret이 응답에 포함되지 않는다

### 검색

- [ ] `GET /products?q=...`가 `name` 부분일치로 검색하고 페이지네이션이 동작한다
- [ ] `q`와 `category`, `sort`를 함께 지정하면 모두 적용된다
- [ ] 상품을 101개 이상 등록한 상태에서 101번째 이후 상품이 검색된다
- [ ] `/search` 화면이 서버 검색으로 동작하고 자동완성도 같은 API를 사용한다
- [ ] 검색 API 실패 시 "검색 결과가 없어요"와 구분되는 실패 안내가 표시된다

### 고객 화면

- [ ] 상품상세가 해당 상품에 등록된 사이즈만 표시한다 (S/M/L/XL 하드코딩 제거)
- [ ] 재고 0인 사이즈가 선택 불가로 표시된다
- [ ] `size = null` 단일 variant 상품은 사이즈 선택 영역이 렌더링되지 않고 바로 장바구니에 담긴다
- [ ] 장바구니 항목이 `variantId`를 갖고, 주문 생성이 `variantId`로 이루어진다
- [ ] 구 형식 장바구니 항목이 로드 시 조용히 제거된다

### 빌드·검증

- [ ] `./gradlew build` / `./gradlew test` 통과
- [ ] `npm run build` / `npm run lint` 통과
- [ ] 모든 신규 엔드포인트에 `@Operation`, DTO 필드에 `@Schema`, 인증 필요 엔드포인트에 `@SecurityRequirement`가 작성된다
