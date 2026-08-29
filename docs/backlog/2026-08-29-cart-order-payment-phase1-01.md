---
date: 2026-08-29
feature: cart-order-payment
phase: 1
category: backend
---

# cart-order-payment / Phase 1 — 2026-08-29

## 실패

`OrderService.deductStockWithRetry`(`backend/src/main/java/com/momentive/backend/order/service/OrderService.java:121-137`)의 낙관적 락 재시도 횟수가 `backend/CLAUDE.md`의 필수 컨벤션 및 plan Phase 1 step("재고 검증 후 차감을 `@Version` 낙관적 락 하에 for-loop 최대 2회 재시도, 2회 실패 시 `STOCK_CONFLICT`")에 미달한다.

현재 구현:
```java
private static final int MAX_STOCK_RETRY = 2;
...
int attempts = 0;
while (true) {
    try {
        ...
        return product;
    } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
        attempts++;
        if (attempts >= MAX_STOCK_RETRY) {
            throw new CustomException(ErrorCode.STOCK_CONFLICT);
        }
    }
}
```
최초 시도가 실패하면 `attempts`가 1이 되고, `1 < 2`이므로 한 번만 더 시도(2번째 attempt)한다. 그 2번째 시도도 실패하면 `attempts`가 2가 되어 즉시 `STOCK_CONFLICT`로 종료한다. 즉 "최초 시도 1회 + 재시도 1회" = 총 2회 시도만 이루어지고, 재시도 자체는 1회뿐이다.

## 원인

`backend/CLAUDE.md`(53-56행)는 "낙관적 락 충돌 시 Service에서 직접 for-loop로 최대 2회까지 재조회 후 재시도한다... 2회 재시도 후에도 충돌하면 STOCK_CONFLICT로 즉시 실패 응답한다"라고 명시한다. 이는 최초 시도 실패 이후 "재시도"가 최대 2회 있어야 함(즉 총 시도 횟수는 최대 3회: 최초 1 + 재시도 2)을 의미하는데, 구현에서는 `MAX_STOCK_RETRY`를 "총 시도 횟수 상한(2)"으로 잘못 해석해 재시도 상한을 1회로 카운트했다. off-by-one 성격의 재시도 카운팅 오류로, 컨벤션 문구의 "재시도 N회"가 "총 시도 N회"인지 "최초시도 + 재시도 N회"인지 구현 시점에 명확히 확인하지 않은 것이 근본 원인이다. 현재 동시성 테스트(`OrderServiceTest.concurrent_orders_on_same_product_only_one_succeeds`)는 스레드 2개·재고 1개 시나리오라 재시도가 1회만 있어도 통과하므로 테스트로는 드러나지 않았다.

## 조치

`OrderService.deductStockWithRetry`의 재시도 카운팅을 수정한다. "최초 시도 + 최대 2회 재시도(총 최대 3회 시도)" 후에도 충돌하면 `STOCK_CONFLICT`로 종료하도록 `MAX_STOCK_RETRY`의 의미를 "재시도 횟수 상한"으로 명확히 하고 루프 종료 조건을 조정한다(예: `attempts > MAX_STOCK_RETRY`로 비교하거나, `MAX_STOCK_RETRY`를 "총 시도 횟수 상한 3"으로 재정의). 수정 후 총 시도 횟수가 의도대로 3회(최초 1 + 재시도 2)가 되는지 확인하는 단위 테스트(예: 낙관적 락 충돌을 2회 연속 강제한 뒤 3번째 시도에서 성공하는 케이스, 또는 3회 모두 실패해 `STOCK_CONFLICT`가 발생하는 케이스)를 추가해 회귀를 방지한다.

### 실제 수정 내용 (2026-08-29)

- `backend/src/main/java/com/momentive/backend/order/service/OrderService.java`의 `deductStockWithRetry`에서 카운터 변수명을 `attempts`(총 시도 횟수 의미로 오독되던 이름)에서 `retryCount`(재시도 횟수임을 명시)로 바꾸고, 종료 조건을 `attempts >= MAX_STOCK_RETRY`(총 2회 시도 상한으로 잘못 동작)에서 `retryCount > MAX_STOCK_RETRY`로 변경했다. `MAX_STOCK_RETRY = 2`는 그대로 "재시도 횟수 상한"으로 의미를 고정했다.
  - 수정 후 동작: 최초 시도 실패 → `retryCount=1`(1 ≤ 2, 재시도) → 2번째 시도 실패 → `retryCount=2`(2 ≤ 2, 재시도) → 3번째 시도 실패 → `retryCount=3`(3 > 2, `STOCK_CONFLICT`). 총 시도 3회(최초 1 + 재시도 2)로 컨벤션과 일치.
- `backend/src/test/java/com/momentive/backend/order/OrderServiceStockRetryTest.java` 신규 추가. `ProductRepository`/`UserRepository`/`AddressRepository`/`AddressService`를 Mockito로 목킹해 `productRepository.saveAndFlush`가 `ObjectOptimisticLockingFailureException`을 정확히 N회 던지도록 제어하는 방식으로 경계값을 결정적으로 검증했다(스레드 타이밍에 의존하지 않음).
  - `succeeds_on_third_attempt_after_two_optimistic_lock_failures`: 낙관적 락 충돌을 2회 연속 강제한 뒤 3번째 시도에서 성공 → 주문이 정상적으로 `PENDING` 생성됨을 검증.
  - `fails_with_stock_conflict_after_exhausting_initial_attempt_plus_two_retries`: 매 시도마다 충돌을 강제해 총 3회 시도 후 `STOCK_CONFLICT`가 발생하고, `saveAndFlush`가 정확히 3회 호출됐음을 검증.
- 검증: `./gradlew test --tests "com.momentive.backend.order.OrderServiceStockRetryTest"` 및 `./gradlew build` 모두 통과 확인 (기존 `OrderServiceTest` 포함 전체 테스트 실패 없음).

## 재발 방지

동시성/재시도 관련 컨벤션 문구("최대 N회 재시도")를 구현할 때는 "총 시도 횟수"와 "재시도 횟수"를 구분해 카운터 변수명과 종료조건에 명시적으로 반영하고, 재시도 상한 경계값(정확히 N번째 실패 시 종료되는지)을 검증하는 단위 테스트를 반드시 추가한다.
