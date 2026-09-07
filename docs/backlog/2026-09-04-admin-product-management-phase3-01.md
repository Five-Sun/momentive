---
date: 2026-09-04
feature: admin-product-management
phase: 3
category: backend
---

# admin-product-management / Phase 3 — 2026-09-04

## 실패

plan Phase 3의 `AdminProductService` step("등록·수정·soft delete를 처리한다")과 관리자 응답 계약 step에 대한 정적 검증에서, **`PUT /admin/products/{id}` 응답이 실제로 DB에 채워지는 식별자를 `null`로 내려보내는 매핑 결함**을 발견했다.

`backend/src/main/java/com/momentive/backend/product/service/AdminProductService.java`의 `updateProduct`는 트랜잭션 내에서 managed `Product`를 조작한 뒤 flush 전에 응답 DTO를 조립한다.

```java
product.replaceImages(imageUrls);   // images.clear() 후 전부 새 ProductImage로 재생성
applyVariants(product, variants);   // 요청의 id == null 인 항목은 product.addVariant(...)로 새 ProductVariant 생성
return AdminProductResponse.from(product);   // ← 이 시점엔 아직 flush 전
```

`ProductVariant`/`ProductImage`는 `GenerationType.IDENTITY`이고, managed 부모 컬렉션에 새로 추가된 자식은 `CascadeType.ALL`이라도 **flush 시점에야** INSERT되어 PK가 부여된다. `updateProduct`에는 `save`/`flush`/이후 조회 쿼리가 없으므로, DTO는 PK가 부여되기 전 상태를 캡처한다. 결과적으로 응답은 다음과 같이 나간다.

- `variants[]` 중 이번 요청에서 새로 추가된 항목: `id: null` (기존 유지 variant의 `id`는 정상)
- `images[]`: **모든 항목이 `id: null`** (`replaceImages`가 매 수정마다 전량 재생성하므로 예외 없음)

같은 코드의 `createProduct`는 `productRepository.save(product)`가 persist 시점에 cascade INSERT를 유발하므로 `id`가 정상적으로 채워진다. 즉 **POST는 맞고 PUT만 틀린 비대칭**이며, `AdminProductVariantResponse`가 스스로 문서화한 계약("수정 폼이 그대로 되돌려 보낼 수 있도록 요청 DTO와 같은 필드명(`id`)을 쓴다")이 PUT 경로에서 성립하지 않는다.

`backend/src/test/java/com/momentive/backend/product/AdminProductServiceTest.java`의 `updateProduct_replaces_basic_fields_images_and_variants`는 새로 추가한 variant("L")와 교체된 이미지에 대해 `size`/`url`/`displayOrder`만 단언하고 `id`는 단언하지 않아 이 결함을 잡지 못한다.

부가로, 이 phase의 `검증 — ./gradlew build, ./gradlew test 통과` step은 이번 리뷰 세션에 셸 실행 수단이 없어 reviewer가 직접 실행하지 못했다(실패 판정의 근거는 아니며, 위 매핑 결함과 무관하게 미검증 상태로 남는다).

## 원인

JPA의 "IDENTITY 키는 flush 시점에 부여된다"는 특성과, 응답 DTO를 트랜잭션 커밋 전에 엔티티 그래프에서 즉시 조립하는 구현 방식이 충돌했다. 등록 경로(`createProduct`)는 `save()`가 persist를 즉시 유발해 우연히 정상 동작했기 때문에, 같은 방식으로 작성한 수정 경로도 동작할 것처럼 보였다. 테스트가 `id`가 아니라 `size`/`url` 같은 값 필드만 단언해 이 차이를 드러내지 못한 것이 결함이 남은 직접 이유다.

이 결함은 Phase 4 관리자 폼과 결합하면 실제 동작 오류가 된다. 폼이 저장 응답을 그대로 상태로 반영한 뒤 사용자가 한 번 더 저장하면, 새 variant가 `id: null`로 재전송되어 `applyVariants`가 **기존 행을 삭제하고 다시 INSERT**한다. 그 사이 해당 variant가 주문에 사용됐다면 재고만 고쳐 저장했는데도 `VARIANT_IN_USE`(400)로 거부되어, spec 시나리오 C("재입고 / 수정")가 깨진다.

## 조치

1. `AdminProductService.updateProduct`가 응답을 조립하기 전에 영속화를 확정한다. 가장 간단한 방법은 `productRepository.saveAndFlush(product)`를 호출한 뒤 `AdminProductResponse.from(product)`를 만드는 것이다(`createProduct`도 `saveAndFlush`로 맞추면 두 경로의 동작이 같아진다). `EntityManager`를 직접 주입해 `flush()`하는 방식도 동등하다.
2. `AdminProductServiceTest.updateProduct_replaces_basic_fields_images_and_variants`에 회귀 단언을 추가한다 — 수정 응답의 `variants()`와 `images()`의 모든 `id()`가 `null`이 아니고, 새로 추가한 "L" variant의 `id`를 그대로 다시 PUT에 실어 보내면 (삭제·재생성이 아니라) 같은 `id`가 유지되는지까지 확인한다.
3. (선택) 같은 검증을 이미지에도 적용해, `ProductImageResponse.id`가 수정 응답에서도 실제 PK를 갖는지 단언한다.

### 실제 수정 내역 (2026-09-04)

1. `AdminProductService.updateProduct` — `applyVariants` 직후 `productRepository.saveAndFlush(product)`를 호출하고 그 뒤에 `AdminProductResponse.from(product)`를 조립하도록 변경. `createProduct`도 `save` → `saveAndFlush`로 맞춰 등록/수정 두 경로의 동작을 동일하게 했다. 왜 flush가 필요한지(자식 IDENTITY PK, 폼 재전송 시 `VARIANT_IN_USE`)를 각 호출부 주석으로 남겼다.
2. `Product.replaceImages` — advisory를 반영해 전량 DELETE+INSERT를 없앴다. URL별 큐(`Map<String, Deque<ProductImage>>`)로 기존 행을 모아 두고, 요청 URL과 일치하는 행은 재사용해 `displayOrder`만 갱신하며(신규 `ProductImage.updateDisplayOrder`), 재사용되지 않은 행만 컬렉션에서 제거해 `orphanRemoval`로 삭제한다. 마지막에 `displayOrder` 기준으로 in-memory 정렬해(`@OrderBy`는 조회 시점에만 적용됨) 같은 트랜잭션에서 조립되는 응답의 이미지 순서를 보장한다. 결과적으로 URL이 바뀌지 않은 수정에서는 이미지 행이 유지되어 `ProductImageResponse.id`가 안정적이고, 불필요한 DELETE/INSERT 쓰기도 사라진다.
3. `ProductImage.updateDisplayOrder(int)` 추가 — 엔티티 setter 금지 컨벤션에 맞춘 도메인 메서드.
4. `AdminProductServiceTest` 회귀 단언 보강:
   - `createProduct_saves_images_and_variants_and_exposes_product_to_customer_list` / `updateProduct_replaces_basic_fields_images_and_variants` 에 `variants`·`images`의 모든 `id`가 non-null임을 단언(`doesNotContainNull`) 추가.
   - 신규 테스트 `updateProduct_response_ids_survive_a_second_save_of_the_same_payload` 추가 — 1차 수정으로 variant `L`과 이미지를 새로 추가하고, 응답의 모든 `id`가 non-null인지와 URL이 유지된 이미지의 `id`가 그대로인지 확인한 뒤, 그 variant로 실제 주문을 넣고 응답을 그대로 되돌려 보내 재저장한다. 재저장 후 variant·image `id`가 1차 응답과 완전히 동일하고 재고만 갱신되는지 단언해, spec 시나리오 C(재입고/수정)가 `VARIANT_IN_USE`로 깨지지 않음을 보장한다.
   - 수정 전 코드로 되돌려 확인한 결과 이 두 테스트가 실제로 실패함을 확인했다(회귀 테스트가 결함을 잡는다는 검증).
5. 검증: `./gradlew build`, `./gradlew test --rerun-tasks` 모두 통과 (`AdminProductServiceTest` 18건 전부 성공).

### 남은 관찰 (이번 수정 범위 밖)

`applyVariants`가 기존 variant를 삭제하고 같은 `size`의 새 variant를 추가하는 요청을 받으면, Hibernate가 INSERT를 DELETE보다 먼저 수행하므로 `uq_product_variant_product_size`(V14 마이그레이션의 부분 유니크 인덱스) 위반이 날 수 있다. 이는 이번 backlog 결함과 무관한 기존 동작이며(flush 시점만 커밋 시점에서 `saveAndFlush` 시점으로 앞당겨졌을 뿐 순서 문제는 동일), 별도 이슈로 다루는 편이 낫다.

## 재발 방지

트랜잭션 안에서 새로 만든 자식 엔티티(`IDENTITY` PK)를 포함해 응답 DTO를 조립하는 서비스 메서드를 작성할 때는, DTO 조립 전에 `saveAndFlush`/`flush`로 PK 부여를 확정했는지 확인하고, 테스트에서 값 필드뿐 아니라 **응답의 모든 식별자 필드가 `null`이 아닌지**를 단언한다.
