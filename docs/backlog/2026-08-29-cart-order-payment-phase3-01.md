---
date: 2026-08-29
feature: cart-order-payment
phase: 3
category: frontend
---

# cart-order-payment / Phase 3 — 2026-08-29

## 실패

`frontend/src/app/(shell)/cart/page.tsx`의 두 번째 step("쿠폰 토글 UI는 유지하되 실제 결제 금액 계산에는 반영하지 않음(placeholder임을 코드 주석으로 명시)")이 미충족이다.

코드는 다음과 같이 여전히 쿠폰 할인을 "총 결제금액"에 반영한다.

```ts
// 쿠폰은 아직 결제 금액에 반영하지 않는 placeholder UI (실제 쿠폰 시스템은 이 spec의 범위 밖)
const discount = couponApplied && selectedItems.length > 0 ? COUPON_DISCOUNT : 0;
const total = Math.max(0, subtotal - discount);
```

`total`은 화면 하단 "총 결제금액"으로 그대로 렌더링되므로(`<span className="text-price text-ink">{formatWon(total)}</span>`), 쿠폰 토글을 켜면 화면상 결제 예정 금액이 3,000원 줄어든다. 코드 주석은 "결제 금액에 반영하지 않는다"고 말하지만 실제 계산은 반영하고 있어 주석과 동작이 모순된다.

## 원인

이 쿠폰 계산/`total` 로직은 이번 Phase 3에서 새로 작성된 것이 아니라 이전 app-redesign 작업(`git show HEAD`)부터 존재하던 배송비 포함 계산식(`subtotal - discount + shippingFee`)을 이번 phase에서 배송비 부분만 제거하며 그대로 물려받았다. plan step은 "선택 기준 재계산"과 "배송비 제거"만 명시했고, 쿠�트가 이미 `total`에 반영되던 기존 동작 자체를 검토하라는 지시가 없어 기존 버그(또는 미완성 placeholder)가 그대로 남았다. 이번 phase에서 주석("결제 금액에 반영하지 않는")만 추가되고 실제 계산식은 고쳐지지 않아, "코드가 스스로 거짓말하는" 상태가 됐다.

## 조치

`total` 계산에서 `discount`를 제외해 실제로 결제 금액에 영향을 주지 않게 한다. 예:

```ts
const total = subtotal; // 쿠폰은 placeholder이므로 결제 금액에 반영하지 않음
```

그리고 "할인금액" 표시 줄은 유지하되(쿠폰 UI 자체는 유지 요구사항), 그 금액이 `total` 계산에 들어가지 않고 단순 정보 표시용임을 명확히 한다. 또는 Phase 4에서 `POST /orders`에 전달할 금액(서버가 검증하는 `totalAmount`)과 화면에 노출하는 금액이 항상 `subtotal` 기준으로 일치하도록 맞춘다 — 그래야 이후 phase의 결제 위젯 표시 금액과 실제 청구 금액이 어긋나는 사용자 신뢰 문제로 번지지 않는다.

**실제 수정 내용 (2026-08-29):**

`frontend/src/app/(shell)/cart/page.tsx`에서 `total` 계산식을 `Math.max(0, subtotal - discount)`에서 `subtotal`로 변경했다. `discount` 변수 자체는 그대로 유지해 "할인금액" 표시 줄(정보성 UI)에만 쓰이도록 하고, 주석도 "'할인금액' 표시 줄에서만 사용하는 정보성 값이며, 총 결제금액(total) 계산에는 포함하지 않는다"로 보강해 코드와 주석의 동작이 일치하도록 했다. 쿠폰 토글 UI(버튼, 스위치)는 변경하지 않고 그대로 유지했다.

`checkoutSelection`/`totalAmount` 관련 코드는 아직 Phase 4에서 구현 전(`/checkout`, `POST /orders` 연동 미착수)이므로 이번 수정 범위에는 포함하지 않았다. Phase 4 구현 시 결제 요청 금액이 `subtotal`(쿠폰 미반영) 기준으로 화면 표시 금액과 일치하도록 맞춰야 한다.

검증: `npm run lint` 통과 확인.

## 재발 방지

placeholder/미완성 UI 로직을 다른 phase에서 이어받아 재사용할 때는, 주석만 추가하지 말고 실제 계산식이 주석이 말하는 동작과 일치하는지 diff 단계에서 직접 실행 결과(값)를 계산해 검증한다 — "화면에 최종 렌더링되는 값(`total`)이 어떤 변수들의 합/차로 구성되는지"를 한 줄씩 추적하는 체크리스트를 프론트 리뷰 시 습관화한다.
