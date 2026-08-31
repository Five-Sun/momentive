---
date: 2026-08-31
feature: shipping-fee-policy
spec: 2026-08-31-shipping-fee-policy.md
plan: 2026-08-31-shipping-fee-policy.md
---

# 배송비 정책 반영 E2E 케이스

최초 실행 시점(2026-08-31 1차)에는 로컬 DB의 `product` 테이블이 비어 있어(`GET /products` → `totalElements: 0`, `GET /products/1` → `PRODUCT_NOT_FOUND`) 시나리오 3(실제 `POST /orders` 필요)을 사전조건 미충족으로 스킵했다. 이후 사용자가 `docker compose down -v` 후 `./dev.sh`로 로컬 dev DB를 완전히 재시딩(상품 15건 정상 확인)하면서 이 제약이 해소돼, 같은 날 2차로 시나리오 3만 재검증했다. 시나리오 1·2는 클라이언트 로컬스토리지 직접 주입(가상 productId)으로 백엔드 상품 데이터 의존 없이 검증 가능해 1차 실행에서 이미 PASS했고, 이번 2차 실행 범위(시나리오 3)에서는 재실행하지 않았다.

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

## 시나리오 3: 주문 생성 → Toss 결제위젯 금액 확인(렌더링만) → 마이페이지 주문상세 breakdown 확인

spec 사용자 시나리오 3~4, 수용 기준 6·9번째 항목(마이페이지 주문상세 3줄 breakdown, Toss 결제위젯 렌더링 금액이 배송비 포함 총액과 일치)을 다룬다. 2026-08-31 최초 시도에서는 로컬 `product` 테이블이 0건이라 스킵했으나, 이후 `docker compose down -v` + `./dev.sh`로 DB를 재시딩해(`GET /products` 상품 15건 정상 확인) 재검증한다. 실제 백엔드 상품(`productId=1`, "강아지 무릎담요", 18,000원, `soldOut=false`)을 장바구니에 담아 `POST /orders`로 실제 주문을 생성하고, 신규 배송지(비제주 우편번호)를 입력해 서버가 계산한 `itemsSubtotal`/`shippingFee`/`totalAmount`(18,000원+3,400원=21,400원)를 확인한다. Toss 결제위젯 confirm 성공 경로는 상점(스토어) 미등록 제약(`docs/backlog/2026-08-30-cart-order-payment-phase4-01.md`)으로 계속 스킵하되, `/checkout/payment` 진입 시 결제 버튼에 렌더링되는 금액(주문 생성 응답의 `totalAmount`)까지는 확인한다.

**사전조건**: 로그인 상태(시나리오 2에서 가입한 계정 재사용 가능, 주소록 0건이면 신규 배송지 폼 사용), 로컬 DB에 `productId=1`이 `soldOut=false`로 존재.

**판정 기준**:
- 장바구니에 상품 A(productId=1, 18,000원, 수량 1) 담고 체크아웃 진입 시 "상품금액 18,000원 / 배송비 3,400원 / 총 결제금액 21,400원" 3줄이 표시된다(70,000원 미만 + 비제주 조합)
- "결제하기" 클릭 후 `POST /orders` 성공 시 `/checkout/payment?orderId=...`로 이동하고, 결제 버튼에 "21,400원 결제하기"가 렌더링된다(서버가 계산한 `totalAmount`와 일치)
- `/mypage/orders/{orderId}`로 이동하면 "상품금액 18,000원 / 배송비 3,400원 / 총 결제금액 21,400원" 3줄이 표시된다(주문 상태는 PENDING이며, 서버 응답값을 그대로 표시하는지 확인 — 클라이언트 재계산 아님)

## 시나리오 4: 체크아웃 신규 배송지 입력 시 제주 ↔ 비제주 우편번호 전환에 따른 배송비 즉시 재계산

plan Phase 2의 미체크 수동 검증 항목("체크아웃에서 배송지를 제주 ↔ 비제주로 바꿔가며 배송비/총액이 즉시 갱신되는지")을 대체 검증한다. 시나리오 2와 동일하게 `checkout/page.tsx`의 신규 배송지 입력 폼에서 우편번호(`zipcode`)를 `useWatch`로 즉시 감시해 서버 왕복 없이 재계산하는 구조이므로, 입력값만 바꿔가며 배송비 표시가 즉시 갱신되는지 확인한다. 시나리오 2가 이미 20,000원/75,000원 두 상품금액 구간에서 각각 제주 전환을 검증했으므로, 이번 시나리오는 **하나의 상품금액(70,000원 미만 고정)에서 제주 → 비제주 → 제주로 왕복 전환**하며 매 전환마다 즉시 재계산되는지에 집중한다(왕복 전환은 시나리오 2에 없던 케이스).

**사전조건**: 로그인 상태(신규 가입), 장바구니에 선택된 가상 상품 1개(30,000원, 70,000원 미만 고정)

**판정 기준**:
- 비제주 우편번호(54321) → 배송비 "3,400원", 총 결제금액 "33,400원"
- 제주 우편번호(63644, 제주 범위 상한 경계값)로 변경 → 배송비 "7,400원", 총 결제금액 "37,400원"으로 즉시 갱신
- 다시 비제주 우편번호(54321)로 변경 → 배송비 "3,400원", 총 결제금액 "33,400원"으로 즉시 복귀
- 화면에 "상품금액"/"배송비"/"총 결제금액" 3줄이 각각 별도로 표시된다(마이페이지 주문상세 breakdown은 시나리오 3에서 이미 PASS 확인됨 — 이번 시나리오는 체크아웃 재계산에 집중)

## 시나리오 5: 장바구니 선택 상품 금액이 70,000원 기준을 넘나들 때 안내 문구/진행바 전환

plan Phase 3의 미체크 수동 검증 항목("장바구니에서 상품 선택/해제로 선택 금액이 70,000원 기준을 넘나들 때 안내 문구와 진행바가 올바르게 전환되는지")을 대체 검증한다. 시나리오 1이 이미 두 상품 모두 선택(달성) → 하나 해제(미달성) → 재선택(달성 복귀)을 검증했으므로, 이번 시나리오는 **미달성 상태에서 시작해 선택 추가로 달성 상태로 넘어가는 반대 방향 전환**과 **진행바(progress bar) 자체의 시각적 갱신**에 집중한다.

**사전조건**: 해당 없음(localStorage 직접 주입)

**판정 기준**: 상품 C(40,000원)만 선택된 초기 상태(70,000원 미만)에서 "30,000원 더 담으면 무료배송" 안내와 진행바(`role="progressbar"` 또는 대응 요소)가 100% 미만 값으로 표시된다. 상품 D(35,000원)를 추가 선택하면(합계 75,000원, 70,000원 이상) 안내 문구가 "무료배송 조건을 달성했어요"로 전환되고 진행바가 가득 찬 상태(100%)로 갱신된다.

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
// 시나리오 3: 주문 생성 -> Toss 결제위젯 금액 확인(렌더링만) -> 마이페이지 주문상세 breakdown 확인
// (2026-08-31 2차 재검증: DB 재시딩 후 사전조건 해소돼 재실행. 신규 계정으로 별도 실행)
// ============================================================
const email3 = `e2e-shipping3-${Date.now()}@example.com`;
await page.goto("http://localhost:3000/signup", { waitUntil: "domcontentloaded" });
await page.getByLabel("이메일").fill(email3);
await page.getByRole("textbox", { name: "비밀번호" }).fill("testpass1");
await page.getByLabel("닉네임").fill("배송비테스터3");
await page.getByRole("button", { name: "회원가입" }).click();
await page.waitForURL("**/mypage", { timeout: 10000 });

// 실제 백엔드 상품(productId=1, 강아지 무릎담요, 18,000원, soldOut=false)을 장바구니에 직접 주입
await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [
    { key: "1-", id: 1, title: "강아지 무릎담요", size: "", unitPrice: 18000, qty: 1 },
  ];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["1-"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

const zipcodeInput3 = page.getByLabel("우편번호");
try {
  await zipcodeInput3.waitFor({ state: "visible", timeout: 8000 });
} catch {
  throw new Error("시나리오 3 판정 기준 미충족: 저장된 배송지가 없는데 신규 배송지 입력 폼(우편번호 필드)이 보이지 않음");
}
await page.getByLabel("받는 사람").fill("배송비테스터3");
await page.getByLabel("연락처").fill("01000000000");
await zipcodeInput3.fill("12345");
await page.getByLabel("주소", { exact: true }).fill("테스트로 1길 1");

const subtotalLocator3 = page.locator("text=상품금액").locator("..").locator("span").last();
const shippingFeeLocator3 = page.locator("text=배송비").locator("..").locator("span").last();
const totalLocator3 = page.locator("text=총 결제금액").locator("..").locator("span").last();

await waitForLocatorText(
  subtotalLocator3,
  (t) => t.includes("18,000원"),
  "시나리오 3 판정 기준 미충족: 체크아웃 상품금액이 18,000원으로 표시되지 않음",
);
await waitForLocatorText(
  shippingFeeLocator3,
  (t) => t.includes("3,400원"),
  "시나리오 3 판정 기준 미충족: 18,000원+비제주 조합에서 배송비가 3,400원이 아님",
);
await waitForLocatorText(
  totalLocator3,
  (t) => t.includes("21,400원"),
  "시나리오 3 판정 기준 미충족: 체크아웃 총 결제금액이 21,400원이 아님",
);

// 결제하기 -> POST /orders 실제 호출 -> /checkout/payment로 이동
await page.getByRole("button", { name: "결제하기" }).click();
await page.waitForURL(/\/checkout\/payment\?orderId=\d+/, { timeout: 10000 });

const orderIdMatch = page.url().match(/orderId=(\d+)/);
if (!orderIdMatch) {
  throw new Error("시나리오 3 판정 기준 미충족: 주문 생성 후 orderId를 포함한 결제 페이지로 이동하지 않음");
}
const orderId = orderIdMatch[1];

const payButton = page.getByRole("button", { name: /결제하기/ });
await waitForLocatorText(
  payButton,
  (t) => t.includes("21,400원"),
  "시나리오 3 판정 기준 미충족: 결제위젯 버튼에 렌더링된 금액이 21,400원이 아님(서버 totalAmount와 불일치)",
);

console.log("PASS: 시나리오 3 (결제하기 버튼 금액 확인, Toss confirm은 스토어 미등록으로 스킵)");

// 마이페이지 주문상세 breakdown 확인 (서버 응답값 그대로 표시, 클라이언트 재계산 아님)
await page.goto(`http://localhost:3000/mypage/orders/${orderId}`, { waitUntil: "domcontentloaded" });

const detailSubtotal = page.locator("text=상품금액").locator("..").locator("span").last();
const detailShipping = page.locator("text=배송비").locator("..").locator("span").last();
const detailTotal = page.locator("text=총 결제금액").locator("..").locator("span").last();

await waitForLocatorText(
  detailSubtotal,
  (t) => t.includes("18,000원"),
  "시나리오 3 판정 기준 미충족: 마이페이지 주문상세 상품금액이 18,000원이 아님",
);
await waitForLocatorText(
  detailShipping,
  (t) => t.includes("3,400원"),
  "시나리오 3 판정 기준 미충족: 마이페이지 주문상세 배송비가 3,400원이 아님",
);
await waitForLocatorText(
  detailTotal,
  (t) => t.includes("21,400원"),
  "시나리오 3 판정 기준 미충족: 마이페이지 주문상세 총 결제금액이 21,400원이 아님",
);

console.log("PASS: 시나리오 3 — 마이페이지 주문상세 breakdown 확인");

// ============================================================
// 시나리오 4: 체크아웃 신규 배송지 입력 시 제주 <-> 비제주 우편번호 왕복 전환에 따른 배송비 즉시 재계산
// (plan Phase 2 미체크 수동 검증 항목 대체 검증)
// ============================================================
const email4 = `e2e-shipping4-${Date.now()}@example.com`;
await page.goto("http://localhost:3000/signup", { waitUntil: "domcontentloaded" });
await page.getByLabel("이메일").fill(email4);
await page.getByRole("textbox", { name: "비밀번호" }).fill("testpass1");
await page.getByLabel("닉네임").fill("배송비테스터4");
await page.getByRole("button", { name: "회원가입" }).click();
await page.waitForURL("**/mypage", { timeout: 10000 });

await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [
    { key: "9401-M", id: 9401, title: "체크아웃 왕복전환 테스트 상품", size: "M", unitPrice: 30000, qty: 1 },
  ];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["9401-M"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

const zipcodeInput4 = page.getByLabel("우편번호");
try {
  await zipcodeInput4.waitFor({ state: "visible", timeout: 8000 });
} catch {
  throw new Error("시나리오 4 판정 기준 미충족: 저장된 배송지가 없는데 신규 배송지 입력 폼(우편번호 필드)이 보이지 않음");
}

const subtotalLocator4 = page.locator("text=상품금액").locator("..").locator("span").last();
const shippingFeeLocator4 = page.locator("text=배송비").locator("..").locator("span").last();
const totalLocator4 = page.locator("text=총 결제금액").locator("..").locator("span").last();

await waitForLocatorText(
  subtotalLocator4,
  (t) => t.includes("30,000원"),
  "시나리오 4 판정 기준 미충족: 상품금액이 30,000원으로 표시되지 않음",
);

// 비제주(54321) -> 배송비 3,400원, 총액 33,400원
await zipcodeInput4.fill("54321");
await waitForLocatorText(
  shippingFeeLocator4,
  (t) => t.includes("3,400원"),
  "시나리오 4 판정 기준 미충족: 30,000원+비제주 조합에서 배송비가 3,400원이 아님",
);
await waitForLocatorText(
  totalLocator4,
  (t) => t.includes("33,400원"),
  "시나리오 4 판정 기준 미충족: 30,000원+비제주 조합에서 총 결제금액이 33,400원이 아님",
);

// 제주(63644, 제주 범위 상한 경계값) -> 배송비 7,400원, 총액 37,400원
await zipcodeInput4.fill("63644");
await waitForLocatorText(
  shippingFeeLocator4,
  (t) => t.includes("7,400원"),
  "시나리오 4 판정 기준 미충족: 제주(63644) 전환 후 배송비가 7,400원으로 즉시 갱신되지 않음",
);
await waitForLocatorText(
  totalLocator4,
  (t) => t.includes("37,400원"),
  "시나리오 4 판정 기준 미충족: 제주(63644) 전환 후 총 결제금액이 37,400원으로 즉시 갱신되지 않음",
);

// 다시 비제주(54321) -> 배송비 3,400원, 총액 33,400원으로 복귀
await zipcodeInput4.fill("54321");
await waitForLocatorText(
  shippingFeeLocator4,
  (t) => t.includes("3,400원"),
  "시나리오 4 판정 기준 미충족: 비제주로 재전환 후 배송비가 3,400원으로 복귀하지 않음",
);
await waitForLocatorText(
  totalLocator4,
  (t) => t.includes("33,400원"),
  "시나리오 4 판정 기준 미충족: 비제주로 재전환 후 총 결제금액이 33,400원으로 복귀하지 않음",
);

console.log("PASS: 시나리오 4");

// ============================================================
// 시나리오 5: 장바구니 선택 상품 금액이 70,000원 기준을 넘나들 때 안내 문구/진행바 전환
// (plan Phase 3 미체크 수동 검증 항목 대체 검증. CSS 셀렉터 fallback 사용 — 컴포넌트 구조 변경 시 갱신 필요)
// ============================================================
await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [
    { key: "9501-M", id: 9501, title: "배송비 진행바 테스트 상품 C", size: "M", unitPrice: 40000, qty: 1 },
    { key: "9502-L", id: 9502, title: "배송비 진행바 테스트 상품 D", size: "L", unitPrice: 35000, qty: 1 },
  ];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
});
await page.reload({ waitUntil: "domcontentloaded" });

// 초기 상태: D는 선택 해제(상품 C만 선택, 합계 40,000원 < 70,000원)
await page.getByRole("button", { name: "선택 해제" }).nth(1).click();

const progressText5 = page.locator("div.bg-surface-soft").locator("span").first();
const progressBar5 = page.locator("div.bg-surface-soft").locator("div.bg-brand-pink");

await waitForLocatorText(
  progressText5,
  (t) => t.includes("30,000원 더 담으면 무료배송"),
  "시나리오 5 판정 기준 미충족: 상품 C만 선택(합계 40,000원)인데 '30,000원 더 담으면 무료배송' 안내가 보이지 않음",
);
const widthBefore = await progressBar5.evaluate((el) => parseFloat(el.style.width || "0"));
if (!(widthBefore < 100)) {
  throw new Error(`시나리오 5 판정 기준 미충족: 미달성 상태인데 진행바 width가 100% 이상임(width=${widthBefore}%)`);
}

// 상품 D(35,000원) 선택 -> 합계 75,000원(70,000원 이상) -> 달성 안내 + 진행바 100%
await page.getByRole("button", { name: "선택", exact: true }).click();
await waitForLocatorText(
  progressText5,
  (t) => t.includes("무료배송 조건을 달성했어요"),
  "시나리오 5 판정 기준 미충족: D 추가 선택 후(합계 75,000원) 무료배송 달성 안내로 전환되지 않음",
);
const widthAfter = await progressBar5.evaluate((el) => parseFloat(el.style.width || "0"));
if (widthAfter !== 100) {
  throw new Error(`시나리오 5 판정 기준 미충족: 달성 상태인데 진행바 width가 100%가 아님(width=${widthAfter}%)`);
}

console.log("PASS: 시나리오 5");
```
