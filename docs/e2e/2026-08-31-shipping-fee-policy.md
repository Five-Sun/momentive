---
date: 2026-08-31
feature: shipping-fee-policy
spec: 2026-08-31-shipping-fee-policy.md
plan: 2026-08-31-shipping-fee-policy.md
---

# 배송비 정책 반영 E2E 케이스

실행 시점(2026-08-31) 로컬 DB의 `product` 테이블이 비어 있음을 사전 확인했다 (`GET /products` → `totalElements: 0`, `GET /products/1` → `PRODUCT_NOT_FOUND`). Flyway 마이그레이션 `V2__seed_product.sql`은 `flyway_schema_history`상 이미 `success`로 적용된 상태라 시드 자체는 존재하지만, 실행 시점에 로컬 개발 중 데이터가 삭제된 것으로 추정된다(`docs/backlog/2026-08-29-cart-order-payment-phase4-01.md`에서도 동일 패턴이 관찰됨 — 코드 결함 아님). e2e-tester는 Docker DB를 재생성/재시딩할 권한이 없으므로(`Docker DB까지 띄우는 건 사용자 몫`), 실제 `POST /orders` 주문 생성이 필요한 시나리오(시나리오 3)는 사전조건 미충족으로 스킵한다. 대신 시나리오 1·2는 클라이언트 로컬스토리지 직접 주입(가상 productId)으로 백엔드 상품 데이터 의존 없이 검증 가능해 정상 실행한다.

## 시나리오 1: 장바구니 무료배송 안내 전환

spec 사용자 시나리오 1, 수용 기준 7번째 항목("선택된 상품 합계가 70,000원 미만이면 안내, 이상이면 달성 안내, 체크 해제 시 다시 미달성으로 전환")을 검증한다. 장바구니는 `localStorage`(`momentive:cart`) 기반이므로 상품 API 응답과 무관하게 `page.evaluate`로 `CartItem[]`을 직접 주입한다. `ShippingProgress` 기준금액이 70,000원으로 갱신됐는지도 함께 확인한다.

**사전조건**: 해당 없음 (localStorage 직접 주입으로 상품 데이터 의존성 제거)

**판정 기준**: 상품 A(45,000원)+B(30,000원) 모두 선택 시(합계 75,000원, 70,000원 이상) "무료배송 조건을 달성했어요" 안내가 보인다. B를 체크 해제하면(합계 45,000원) "25,000원 더 담으면 무료배송" 안내로 전환된다. B를 다시 선택하면 달성 안내로 복귀한다.

## 시나리오 2: 체크아웃 배송지 전환에 따른 배송비 재계산(클라이언트 미리보기)

spec 사용자 시나리오 2, 수용 기준 1~5번째 항목("70,000원 미만/이상 × 제주/비제주 4개 조합", "우편번호 파싱 실패 시 안전 처리", "체크아웃 3줄 표시", "배송지 전환 시 즉시 재계산")을 검증한다. `checkout/page.tsx`의 `shippingFee`/`totalAmount`는 `frontend/src/lib/shipping.ts`의 `calculateShippingFee`로 서버 왕복 없이 즉시 계산되므로, 실제 주문 생성(`POST /orders`) 없이도 우편번호 입력값 변경만으로 검증 가능하다. 신규 가입 계정이라 저장된 배송지가 없어 신규 배송지 입력 폼이 바로 노출된다.

**사전조건**: 로그인 상태(신규 가입, 주소록 0건), 장바구니에 선택된 가상 상품 1개(가격은 단계별로 조정, 실제 백엔드 상품 데이터 불필요 — 이 시나리오는 주문을 제출하지 않고 미리보기 계산만 검증)

**판정 기준**:
- 상품금액 20,000원(70,000원 미만) + 비제주 우편번호(12345) → 배송비 "3,400원", 총 결제금액 "23,400원"
- 같은 상품금액 + 제주 우편번호(63100) → 배송비 "7,400원", 총 결제금액 "27,400원"
- 같은 상품금액 + 숫자로 파싱되지 않는 우편번호("ABCDE") → 배송비 "3,400원"으로 복귀(제주 할증 없음)
- 상품금액을 75,000원(70,000원 이상)으로 바꾼 뒤 비제주 우편번호(12345) → 배송비 "무료", 총 결제금액 "75,000원"
- 같은 상품금액 + 제주 우편번호(63100) → 배송비 "4,000원", 총 결제금액 "79,000원"
- 화면에 "상품금액"/"배송비"/"총 결제금액" 3줄이 각각 별도로 표시된다

## 시나리오 3: 주문 생성 → Toss 결제위젯 금액 확인 → 마이페이지 주문상세 breakdown 확인 — 스킵

spec 사용자 시나리오 3~4, 수용 기준 6·9번째 항목(마이페이지 주문상세 3줄 breakdown, Toss 결제위젯 렌더링 금액과 confirm 검증 금액이 배송비 포함 총액과 일치)을 다룬다.

**사전조건**: 로컬 DB에 `soldOut=false`인 실제 상품이 1건 이상 있어야 `POST /orders`로 주문을 생성할 수 있다. 실행 시점 확인 결과 로컬 `product` 테이블이 0건이라(`GET /products` → `totalElements: 0`) 이 사전조건을 충족할 방법이 없다(재시딩은 Docker DB 재생성이 필요해 e2e-tester 권한 밖). Toss 결제위젯 confirm 성공 경로는 별도로 상점(스토어) 미등록 제약(`docs/backlog/2026-08-30-cart-order-payment-phase4-01.md`)으로도 스킵 대상이다.

**판정 기준**: (스킵) 로컬 상품 데이터가 채워진 뒤 재실행 시 다음을 확인해야 한다 — `POST /orders` 성공 후 `/checkout/payment`에서 Toss 위젯에 "상품금액+배송비" 합산 금액이 렌더링되는지, `/mypage/orders/{orderId}`에서 "상품금액/배송비/총 결제금액" 3줄이 서버 응답값과 정확히 일치하는지.

## 실행 스크립트

```javascript
const page = await browser.getPage("shipping-fee-policy");

async function waitForLocatorText(locator, predicate, description, timeoutMs = 5000) {
  const deadline = Date.now() + timeoutMs;
  let last = "";
  while (Date.now() < deadline) {
    last = (await locator.textContent().catch(() => "")) || "";
    if (predicate(last)) return last;
    await page.waitForTimeout(200);
  }
  throw new Error(`${description} (마지막 확인된 값: "${last}")`);
}

// ============================================================
// 시나리오 1: 장바구니 무료배송 안내 전환
// ============================================================
await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });

await page.evaluate(() => {
  const cart = [
    { key: "9101-M", id: 9101, title: "배송비 테스트 상품 A", size: "M", unitPrice: 45000, qty: 1 },
    { key: "9102-L", id: 9102, title: "배송비 테스트 상품 B", size: "L", unitPrice: 30000, qty: 1 },
  ];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
});
await page.reload({ waitUntil: "domcontentloaded" });

const progressText = page.locator("div.bg-surface-soft").locator("span").first();

// 초기: 둘 다 선택 -> 합계 75,000원(70,000원 이상) -> 달성 안내
await waitForLocatorText(
  progressText,
  (t) => t.includes("무료배송 조건을 달성했어요"),
  "시나리오 1 판정 기준 미충족: 합계 75,000원인데 무료배송 달성 안내가 보이지 않음",
);

// B(30,000원) 선택 해제 -> 합계 45,000원 -> 25,000원 더 담으면 무료배송
await page.getByRole("button", { name: "선택 해제" }).nth(1).click();
await waitForLocatorText(
  progressText,
  (t) => t.includes("25,000원 더 담으면 무료배송"),
  "시나리오 1 판정 기준 미충족: B 해제 후(합계 45,000원) 안내 문구가 예상과 다름",
);

// B 다시 선택 -> 합계 75,000원 복귀 -> 달성 안내 복귀
// (getByRole name 매칭은 기본이 부분일치라 "선택"이 "선택 해제"/"전체선택 (N/M)"까지 매칭한다 — exact로 좁힘)
await page.getByRole("button", { name: "선택", exact: true }).click();
await waitForLocatorText(
  progressText,
  (t) => t.includes("무료배송 조건을 달성했어요"),
  "시나리오 1 판정 기준 미충족: B 재선택 후 무료배송 달성 안내로 복귀하지 않음",
);

console.log("PASS: 시나리오 1");
// 실행 결과: 최초 시도에서 "B 다시 선택" 스텝에 getByRole("button", { name: "선택" }).nth(1)을 써서
// FAIL — Playwright의 accessible name 매칭이 기본 부분일치라 "선택 해제"/"전체선택 (N/M)" 버튼까지
// 함께 매칭돼 엉뚱한 버튼이 클릭됐다(테스트 스크립트 이슈, 코드 결함 아님). exact: true로 좁혀 재실행 후 PASS.

// ============================================================
// 시나리오 2: 체크아웃 배송지 전환에 따른 배송비 재계산(클라이언트 미리보기)
// ============================================================
const email = `e2e-shipping-${Date.now()}@example.com`;
await page.goto("http://localhost:3000/signup", { waitUntil: "domcontentloaded" });
await page.getByLabel("이메일").fill(email);
await page.getByRole("textbox", { name: "비밀번호" }).fill("testpass1");
await page.getByLabel("닉네임").fill("배송비테스터");
await page.getByRole("button", { name: "회원가입" }).click();
await page.waitForURL("**/mypage", { timeout: 10000 });

await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [
    { key: "9201-M", id: 9201, title: "체크아웃 미리보기 테스트 상품", size: "M", unitPrice: 20000, qty: 1 },
  ];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["9201-M"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

const subtotalLocator = page.locator("text=상품금액").locator("..").locator("span").last();
const shippingFeeLocator = page.locator("text=배송비").locator("..").locator("span").last();
const totalLocator = page.locator("text=총 결제금액").locator("..").locator("span").last();

// 신규 가입 계정 -> 저장된 배송지 없음 -> 신규 배송지 폼 즉시 노출
// (isVisible({timeout})은 즉시 체크라 GET /addresses 응답 도착 전에 false를 반환할 수 있음 —
//  docs/e2e/2026-08-29-cart-order-payment.md에서 동일하게 관찰된 이슈. waitFor로 폴링)
const zipcodeInput = page.getByLabel("우편번호");
try {
  await zipcodeInput.waitFor({ state: "visible", timeout: 8000 });
} catch {
  throw new Error("시나리오 2 판정 기준 미충족: 저장된 배송지가 없는데 신규 배송지 입력 폼(우편번호 필드)이 보이지 않음");
}

await waitForLocatorText(
  subtotalLocator,
  (t) => t.includes("20,000원"),
  "시나리오 2 판정 기준 미충족: 상품금액이 20,000원으로 표시되지 않음",
);

// 상품금액 20,000원(70,000원 미만) + 비제주(12345) -> 배송비 3,400원, 총액 23,400원
await zipcodeInput.fill("12345");
await waitForLocatorText(
  shippingFeeLocator,
  (t) => t.includes("3,400원"),
  "시나리오 2 판정 기준 미충족: 20,000원+비제주 조합에서 배송비가 3,400원이 아님",
);
await waitForLocatorText(
  totalLocator,
  (t) => t.includes("23,400원"),
  "시나리오 2 판정 기준 미충족: 20,000원+비제주 조합에서 총 결제금액이 23,400원이 아님",
);

// 같은 상품금액 + 제주(63100) -> 배송비 7,400원, 총액 27,400원
await zipcodeInput.fill("63100");
await waitForLocatorText(
  shippingFeeLocator,
  (t) => t.includes("7,400원"),
  "시나리오 2 판정 기준 미충족: 20,000원+제주 조합에서 배송비가 7,400원이 아님",
);
await waitForLocatorText(
  totalLocator,
  (t) => t.includes("27,400원"),
  "시나리오 2 판정 기준 미충족: 20,000원+제주 조합에서 총 결제금액이 27,400원이 아님",
);

// 파싱 불가 우편번호 -> 제주 할증 없이 안전 처리(3,400원으로 복귀)
await zipcodeInput.fill("ABCDE");
await waitForLocatorText(
  shippingFeeLocator,
  (t) => t.includes("3,400원"),
  "시나리오 2 판정 기준 미충족: 우편번호 파싱 실패 시 제주 할증 없이 3,400원으로 처리되지 않음",
);

// 상품금액을 75,000원(70,000원 이상)으로 변경 -> 페이지 재진입 필요(체크아웃은 마운트 시 1회만 장바구니를 읽음)
await page.evaluate(() => {
  const cart = [
    { key: "9201-M", id: 9201, title: "체크아웃 미리보기 테스트 상품", size: "M", unitPrice: 75000, qty: 1 },
  ];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

const subtotalLocator2 = page.locator("text=상품금액").locator("..").locator("span").last();
const shippingFeeLocator2 = page.locator("text=배송비").locator("..").locator("span").last();
const totalLocator2 = page.locator("text=총 결제금액").locator("..").locator("span").last();
const zipcodeInput2 = page.getByLabel("우편번호");
await zipcodeInput2.waitFor({ state: "visible", timeout: 5000 });

await waitForLocatorText(
  subtotalLocator2,
  (t) => t.includes("75,000원"),
  "시나리오 2 판정 기준 미충족: 상품금액이 75,000원으로 갱신되지 않음",
);

// 상품금액 75,000원(70,000원 이상) + 비제주(12345) -> 배송비 무료, 총액 75,000원
await zipcodeInput2.fill("12345");
await waitForLocatorText(
  shippingFeeLocator2,
  (t) => t.includes("무료"),
  "시나리오 2 판정 기준 미충족: 75,000원+비제주 조합에서 배송비가 무료로 표시되지 않음",
);
await waitForLocatorText(
  totalLocator2,
  (t) => t.includes("75,000원"),
  "시나리오 2 판정 기준 미충족: 75,000원+비제주 조합에서 총 결제금액이 75,000원이 아님",
);

// 같은 상품금액 + 제주(63100) -> 배송비 4,000원(할증만), 총액 79,000원
await zipcodeInput2.fill("63100");
await waitForLocatorText(
  shippingFeeLocator2,
  (t) => t.includes("4,000원"),
  "시나리오 2 판정 기준 미충족: 75,000원+제주 조합에서 배송비가 4,000원이 아님",
);
await waitForLocatorText(
  totalLocator2,
  (t) => t.includes("79,000원"),
  "시나리오 2 판정 기준 미충족: 75,000원+제주 조합에서 총 결제금액이 79,000원이 아님",
);

console.log("PASS: 시나리오 2");
// 실행 결과: 최초 시도에서 zipcodeInput.isVisible({ timeout: 5000 })로 신규 배송지 폼 노출을 체크해
// FAIL — isVisible({timeout})은 폴링이 아니라 즉시 체크라 GET /addresses 응답이 오기 전에 false를
// 반환했다(docs/e2e/2026-08-29-cart-order-payment.md에서 동일 패턴 관찰, 테스트 스크립트 이슈 — 코드
// 결함 아님). waitFor({ state: "visible", timeout: 8000 })로 교체해 재실행 후 PASS.

// ============================================================
// 시나리오 3: 주문 생성 -> Toss 결제위젯 금액 확인 -> 마이페이지 주문상세 breakdown 확인 -- 스킵
// ============================================================
console.log(
  "SKIP: 시나리오 3 — 사전조건(로컬 DB 실 상품 데이터) 미충족: GET /products totalElements=0. Docker DB 재시딩은 e2e-tester 권한 밖이라 스킵",
);
```
