---
date: 2026-08-31
feature: product-review
phase: 4
category: frontend
---

# product-review / Phase 4 — 2026-08-31

## 실패

E2E 시나리오 1(`docs/e2e/2026-08-30-product-review.md`)에서 "로그인 + 리뷰 없는 상품상세 진입" 판정이 실패했다. 구매 이력이 있는 로그인 사용자(`e2e-review-buyer@momentive.test`, 상품 279를 포함한 `PAID` 주문 158 보유)로 `/products/279`에 진입했음에도 "리뷰 쓰기" 버튼이 렌더링되지 않고, 대신 "구매한 상품만 리뷰를 쓸 수 있어요"(구매 미확인 안내)가 표시되었다. 이 상태 의존으로 이후 시나리오 2~6은 실행되지 않았다(시나리오 1 실패로 미실행).

스크린샷: `~/.dev-browser/tmp/product-review-scenario-1-no-write-button`

## 원인

`GET /products/{productId}/reviews/me`는 사용자가 해당 상품에 아직 리뷰를 쓰지 않은 정상 케이스에서 `MyReviewResponse` 값으로 `null`을 반환하도록 설계되어 있다(`ReviewController.getMyReview` → `ReviewService.getMyReview`). Spring MVC는 컨트롤러가 `null`을 반환하면 응답 바디를 아예 쓰지 않으므로, 실제 HTTP 응답은 `200 OK` + `Content-Length: 0`(빈 바디)로 내려간다. `curl`로 직접 확인한 결과도 동일했다:

```
GET /products/279/reviews/me (구매 이력 O, 리뷰 미작성)
→ HTTP/1.1 200, Content-Length: 0, 바디 없음
```

프론트 공통 fetch 래퍼 `frontend/src/lib/api/client.ts`의 `apiFetch`는 `res.ok`이면 무조건 `res.json()`을 호출한다:

```ts
if (!res.ok) {
  throw await parseErrorResponse(res);
}
if (res.status === 204) {
  return undefined as T;
}
return res.json() as Promise<T>;
```

`200` + 빈 바디는 `204` 분기를 타지 않으므로 `res.json()`이 그대로 호출되고, 빈 문자열을 파싱하려다 `SyntaxError: Unexpected end of JSON input`을 던진다. 브라우저에서 직접 재현해 확인했다:

```js
const res = await fetch("http://localhost:8081/products/279/reviews/me", { credentials: "include" });
await res.json(); // SyntaxError: Unexpected end of JSON input
```

이 `SyntaxError`는 `frontend/src/components/commerce/ProductDetailView.tsx`의 `getMyReview` 호출부 `.catch((err) => { if (err instanceof ApiError && err.errorCode === "PURCHASE_NOT_VERIFIED") {...} setPurchaseVerified(false); setMyReview(null); })`로 흘러간다. `err instanceof ApiError`가 거짓이므로 `else` 분기(사실상 폴백)로 빠져 `setPurchaseVerified(false)`가 실행된다. 결과적으로 구매 이력이 있어 정상적으로 리뷰를 쓸 수 있는 사용자도 "구매 미확인" 상태로 잘못 처리되어 리뷰 작성 버튼 자체가 나타나지 않는다.

즉 근본 원인은 백엔드가 "리뷰 없음"을 `200 + null 바디`로 표현하는 API 계약과, 프론트가 `200`이면 항상 JSON 바디가 있다고 가정하는 `apiFetch` 구현 사이의 불일치다.

## 조치

다음 중 하나로 API 계약을 명확히 하고 양쪽을 일치시켜야 한다(둘 다 고쳐도 되지만 최소 하나는 필수).

1. **백엔드**: `ReviewController.getMyReview`가 `null`일 때도 명시적으로 JSON `null` 바디를 응답하도록 변경한다. 예: `ResponseEntity<MyReviewResponse>`로 바꿔 `ResponseEntity.ok(response)`(response가 null이어도 Jackson이 문자 그대로 `null`을 직렬화하도록)로 리턴하거나, `@ResponseBody` 직렬화 설정을 점검한다. 가장 간단한 방법은 컨트롤러 반환 타입을 `ResponseEntity<MyReviewResponse>`로 바꾸고 `return ResponseEntity.ok().body(reviewService.getMyReview(...))`로 명시하는 것 — Spring이 `null` 값을 가진 `ResponseEntity`도 바디를 생략할 수 있으므로, 실제로는 `MyReviewResponse` 자체를 `{ reviewId: null, rating: null, text: null }` 같은 빈 객체로 감싸는 방식이 더 안전할 수 있다. spec의 `Response 200: { reviewId, rating, text } | null` 계약을 유지하려면 실제로 body가 `null` 문자열(JSON `null`)로 나가는지 재확인 필요.
2. **프론트**: `apiFetch`에서 `Content-Length: 0`(또는 `res.text()`가 빈 문자열)인 200 응답을 `204`와 동일하게 취급해 `undefined`/`null`을 반환하도록 방어 로직을 추가한다. 예: `res.json()` 대신 `const text = await res.text(); return text ? JSON.parse(text) : (undefined as T);`
3. `ProductDetailView.tsx`의 `getMyReview` catch 블록도 `err instanceof ApiError`가 아닌 예외(파싱 에러 등)를 구매 미확인으로 오인하지 않도록, 예상 못한 에러는 별도로 로깅/처리해 향후 유사 버그가 조용히 묻히지 않게 한다.

권장은 1번(백엔드가 계약대로 명시적 `null` JSON을 내려주는 것)과 2번(프론트가 빈 바디에 방어적인 것)을 함께 적용하는 것이다.

### 실제 조치 내역 (2026-08-31)

1번, 2번, 3번 모두 반영했다.

- **백엔드** `backend/src/main/java/com/momentive/backend/review/controller/ReviewController.java`: `getMyReview`의 반환 타입을 `MyReviewResponse`에서 `ResponseEntity<MyReviewResponse>`로 변경하고 `return ResponseEntity.ok(reviewService.getMyReview(productId, userId));`로 명시했다. Spring MVC가 `@ResponseBody`(또는 `@RestController`) 메서드가 `null`을 직접 반환하면 `HttpMessageConverter` 호출 자체를 건너뛰어 바디가 생략되지만, `ResponseEntity`로 감싸면 `HttpEntityMethodProcessor`가 body가 `null`이어도 메시지 컨버터를 거쳐 JSON literal `null`을 실제로 직렬화한다. spec의 `Response 200: { reviewId, rating, text } | null` 계약을 그대로 유지한다.
- **프론트** `frontend/src/lib/api/client.ts`의 `apiFetch`: `res.json()`을 바로 호출하던 부분을 `const text = await res.text(); return (text ? JSON.parse(text) : undefined) as T;`로 교체했다. 200 응답이라도 바디가 비어 있으면(204와 마찬가지로) `undefined`를 반환하도록 방어했고, 바디가 있으면 `JSON.parse`로 파싱한다.
- **프론트** `frontend/src/components/commerce/ProductDetailView.tsx`의 `getMyReview` 호출부 `.catch` 블록: `err instanceof ApiError && err.errorCode === "PURCHASE_NOT_VERIFIED"`가 아닌 예상 못한 에러(네트워크 오류, 파싱 실패 등)를 조용히 "구매 미확인"으로 처리하지 않도록, `console.error`로 로깅하고 `showToast`로 사용자에게도 알리도록 변경했다(기존처럼 `purchaseVerified=false`/`myReview=null`로 폴백은 하되, 원인이 드러나게 함).

검증: `./gradlew build`(리뷰 무관 기존 테스트 12건 실패는 FK 제약 위반 등 DB 상태 이슈로 이번 변경과 무관 — `ReviewServiceTest`만 별도로 `./gradlew test --tests "com.momentive.backend.review.*"` 실행해 통과 확인), `npm run build`, `npm run lint` 모두 통과.

## 재발 방지

nullable 응답 바디를 갖는 GET 엔드포인트를 설계/구현할 때는, 컨트롤러가 실제로 `null`을 반환했을 때 HTTP 응답이 `200 + 빈 바디`가 아니라 `200 + JSON literal null`로 나가는지 `curl -i`로 직접 확인하고, 프론트 `apiFetch` 공통 래퍼가 그 케이스(및 204)를 모두 안전하게 처리하는지 함께 검증한다.
