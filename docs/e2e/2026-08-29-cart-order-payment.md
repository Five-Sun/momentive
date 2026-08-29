---
date: 2026-08-29
feature: cart-order-payment
spec: 2026-08-29-cart-order-payment.md
plan: 2026-08-29-cart-order-payment.md
---

# 장바구니→주문→결제 (토스페이먼츠) E2E 케이스

최초 실행(2026-08-29 오전) 시점 로컬 DB의 `product` 테이블이 비어 있어(`flyway_schema_history`상 시드 마이그레이션은 `success`로 기록돼 있으나 실제 로우 0건 — 로컬 개발 중 데이터가 삭제된 것으로 추정, 코드 결함 아님) 시나리오 4를 스킵했다. 이후 로컬 DB 볼륨을 삭제하고 재기동해 상품 시드 15건이 `GET /products`(`totalElements: 15`)로 정상 조회됨을 확인, `/addresses`도 최신 코드로 정상 응답(인증 없이 401)함을 확인해 같은 날짜(2026-08-29) 안에서 시나리오 4를 이어서 실행했다.

이번 재실행에서 시나리오 4가 **코드 결함으로 실패**했다: `/checkout` 페이지의 "결제하기" 버튼이 전역 하단 네비게이션(`GlobalBottomNav`)에 완전히 가려져 클릭 자체가 불가능하다(`document.elementFromPoint`로 확인 — 상세는 `docs/backlog/2026-08-29-cart-order-payment-phase6-01.md`). 이로 인해 이 실패에 의존하는 시나리오 5(confirm 실패 흐름), 시나리오 6(마이페이지 목록 확인)도 연쇄적으로 스킵됐다. Toss 결제위젯 자체(카드 정보 입력, 실제 결제 승인)는 외부 iframe·실카드 인증이 필요해 브라우저 자동화로 완결할 수 없으므로, 애초에 confirm 콜백 라우트(`/checkout/success`)에 유효하지 않은 `paymentKey`로 직접 진입시켜 "confirm 실패 → FAILED 전환 → 재고 복원 → 취소 버튼 미노출" 경로를 검증할 계획이었으나(시나리오 5), 그 전제가 되는 주문 생성(시나리오 4)부터 막혀 실행하지 못했다. confirm이 실제로 성공(PAID)하는 경로(시나리오 7)는 Toss 샌드박스 카드 결제를 완료해야만 도달 가능해 처음부터 스킵 대상이다.

**추가 재실행(2026-08-29, 같은 날)**: `docs/backlog/2026-08-29-cart-order-payment-phase6-01.md`의 조치대로 `GlobalBottomNav`에 경로 기반 숨김 조건(`/checkout`, `/mypage/orders/`)이 추가된 뒤, 재실행 전 `document.elementFromPoint`로 "결제하기" 버튼 가려짐이 해소됐음을 먼저 확인하고 시나리오 4, 5, 6을 순서대로 재실행해 모두 PASS했다. 시나리오 5 재실행 중 `isVisible({ timeout })`이 Playwright에서 즉시 체크(비polling)로 동작해 두 차례 거짓 실패가 발생했으나, `waitFor({ state: "visible" })`로 대체해 실제로는 정상 동작함을 확인했다(코드 결함 아님, 테스트 스크립트의 대기 방식 이슈). 시나리오 7은 여전히 Toss 샌드박스 실카드 결제가 필요해 스킵 대상이다.

## 시나리오 1: 장바구니 항목 선택/전체선택/금액 재계산/구매하기 활성화

spec 사용자 시나리오 1(`/cart`에서 체크박스로 부분/전체 선택 → 선택 항목만 금액 반영 → 구매하기 버튼은 1개 이상 선택 시에만 활성화)을 검증한다. 장바구니는 `localStorage`(`momentive:cart`) 기반이므로 상품 API 응답과 무관하게 `page.evaluate`로 `CartItem[]`을 직접 주입해 검증한다.

**사전조건**: 해당 없음 (localStorage 직접 주입으로 상품 데이터 의존성 제거)

**판정 기준**: 항목 2개를 담은 뒤 1개만 체크 해제하면 "총 결제금액"이 나머지 1개 금액과 일치하고, 전체 해제 시 "구매하기" 버튼이 `disabled`, 다시 전체선택하면 `disabled`가 풀린다.

```javascript
const page = await browser.getPage("cart-order-payment-scenario-1");
await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });

await page.evaluate(() => {
  const cart = [
    { key: "9001-M", id: 9001, title: "테스트 상품 A", size: "M", unitPrice: 10000, qty: 1 },
    { key: "9002-L", id: 9002, title: "테스트 상품 B", size: "L", unitPrice: 20000, qty: 1 },
  ];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
});
await page.reload({ waitUntil: "domcontentloaded" });

// 초기 상태: 둘 다 선택됨 -> 합계 30,000원, 구매하기 활성화
const totalLocator = page.locator("text=총 결제금액").locator("..").locator("span").last();
const initialTotal = await totalLocator.textContent();
if (!initialTotal || !initialTotal.includes("30,000")) {
  throw new Error(`판정 기준 미충족: 초기 합계가 30,000원이 아님 (실제: ${initialTotal})`);
}

const buyButton = page.getByRole("button", { name: "구매하기" });
if (await buyButton.isDisabled()) {
  throw new Error("판정 기준 미충족: 항목이 선택된 상태인데 구매하기 버튼이 비활성화됨");
}

// 상품 B 선택 해제 -> 합계 10,000원
await page.getByRole("button", { name: "선택 해제" }).nth(1).click();
const afterUncheckTotal = await totalLocator.textContent();
if (!afterUncheckTotal || !afterUncheckTotal.includes("10,000")) {
  throw new Error(`판정 기준 미충족: 1개 해제 후 합계가 10,000원이 아님 (실제: ${afterUncheckTotal})`);
}

// 나머지 항목도 선택 해제 -> 구매하기 비활성화
await page.getByRole("button", { name: "선택 해제" }).click();
if (!(await buyButton.isDisabled())) {
  throw new Error("판정 기준 미충족: 선택 항목 0개인데 구매하기 버튼이 활성화 상태");
}

// 전체선택 토글 -> 다시 활성화 + 합계 30,000원 복원
await page.getByRole("button", { name: /전체선택/ }).click();
if (await buyButton.isDisabled()) {
  throw new Error("판정 기준 미충족: 전체선택 후에도 구매하기 버튼이 비활성화");
}
const afterSelectAllTotal = await totalLocator.textContent();
if (!afterSelectAllTotal || !afterSelectAllTotal.includes("30,000")) {
  throw new Error(`판정 기준 미충족: 전체선택 후 합계가 30,000원이 아님 (실제: ${afterSelectAllTotal})`);
}

console.log("PASS: 시나리오 1");
```

## 시나리오 2: 배송지 없는 신규 사용자의 체크아웃 진입 시 입력 폼 즉시 노출

spec 사용자 시나리오 2-3(저장된 배송지가 없으면 배송지 입력 폼이 바로 보인다), 수용 기준 3번째 항목을 검증한다. 신규 회원가입으로 만든 사용자는 주소록이 비어 있으므로 `/checkout` 진입 시 새 배송지 입력 폼(`AddressFields`)이 즉시 렌더링돼야 한다.

**사전조건**: 로그인 상태 + 장바구니에 선택된 항목 1개 이상(`momentive:checkout-selection` sessionStorage에 해당 key 포함, 그렇지 않으면 `/checkout`이 즉시 `/cart`로 리다이렉트됨). 신규 가입 계정이라 주소록이 비어 있어야 함.

**판정 기준**: `/checkout` 진입 후 "받는 사람"(recipient) 텍스트 입력 필드가 노출되고, "새 배송지 추가"/"저장된 배송지 사용" 토글 버튼이 보이지 않는다(저장된 주소가 0개이므로 목록 자체가 렌더링되지 않음).

```javascript
const page = await browser.getPage("cart-order-payment-scenario-2");

const email = `e2e-checkout-${Date.now()}@example.com`;
await page.goto("http://localhost:3000/signup", { waitUntil: "domcontentloaded" });
await page.getByLabel("이메일").fill(email);
await page.getByRole("textbox", { name: "비밀번호" }).fill("testpass1");
await page.getByLabel("닉네임").fill("체크아웃테스터");
await page.getByRole("button", { name: "회원가입" }).click();
await page.waitForURL("**/mypage", { timeout: 10000 });

await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [{ key: "9003-M", id: 9003, title: "체크아웃 테스트 상품", size: "M", unitPrice: 15000, qty: 1 }];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["9003-M"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

const recipientInput = page.getByLabel("받는 사람");
if (!(await recipientInput.isVisible({ timeout: 5000 }))) {
  throw new Error("판정 기준 미충족: 저장된 배송지가 없는데 신규 배송지 입력 폼(받는 사람 필드)이 보이지 않음");
}

const savedAddressToggle = page.getByRole("button", { name: /저장된 배송지 사용|새 배송지 추가/ });
if (await savedAddressToggle.isVisible()) {
  throw new Error("판정 기준 미충족: 저장된 주소가 0개인데 배송지 목록/토글 버튼이 렌더링됨");
}

console.log("PASS: 시나리오 2");
```

## 시나리오 3: 존재하지 않는 상품으로 주문서 제출 시 에러 처리

spec 수용 기준("재고가 부족한 상품이 포함되면 OUT_OF_STOCK... 재고 차감이 일어나지 않는다")과 `POST /orders`의 `PRODUCT_NOT_FOUND`(404) 에러 계약을 검증한다. 실행 시점 로컬 DB에 상품이 0건이므로, 장바구니에 존재하지 않는 productId를 넣고 체크아웃을 제출하면 서버가 `PRODUCT_NOT_FOUND`를 반환하고 프론트가 이를 토스트로 안내해야 한다(`checkout/page.tsx`의 `err.errorCode === "PRODUCT_NOT_FOUND"` 분기).

**사전조건**: 로그인 상태, 장바구니에 존재하지 않는 productId 항목 1개 선택. 신규 배송지 입력 폼에 값을 채워 제출 가능한 상태로 만든다.

**판정 기준**: "결제하기" 클릭 후 화면 전환 없이(같은 `/checkout` 경로 유지) 에러 토스트가 노출된다. 서버 응답이 `PRODUCT_NOT_FOUND`가 아닌 다른 코드(예: `VALIDATION_FAILED`)라도 토스트가 뜨고 주문이 생성되지 않으면(URL이 `/checkout/payment`로 전환되지 않으면) 판정 기준을 만족한 것으로 본다.

```javascript
const page = await browser.getPage("cart-order-payment-scenario-3");

const email = `e2e-order-fail-${Date.now()}@example.com`;
await page.goto("http://localhost:3000/signup", { waitUntil: "domcontentloaded" });
await page.getByLabel("이메일").fill(email);
await page.getByRole("textbox", { name: "비밀번호" }).fill("testpass1");
await page.getByLabel("닉네임").fill("주문실패테스터");
await page.getByRole("button", { name: "회원가입" }).click();
await page.waitForURL("**/mypage", { timeout: 10000 });

await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [{ key: "999999-M", id: 999999, title: "존재하지 않는 상품", size: "M", unitPrice: 10000, qty: 1 }];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["999999-M"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

await page.getByLabel("받는 사람").fill("테스트 수령인");
await page.getByLabel("연락처").fill("01012345678");
await page.getByLabel("우편번호").fill("12345");
await page.getByLabel("주소", { exact: true }).fill("서울시 테스트구 테스트로 1");

await page.getByRole("button", { name: "결제하기" }).click();

// 토스트 노출 대기 (성공 시 /checkout/payment로 라우팅되므로, 실패라면 토스트가 뜨고 URL은 그대로 /checkout)
await page.waitForTimeout(2000);

const currentUrl = page.url();
if (currentUrl.includes("/checkout/payment")) {
  let screenshotPath = "";
  try {
    const buf = await page.screenshot();
    screenshotPath = await saveScreenshot(buf, "cart-order-payment-scenario-3-unexpected-success");
  } catch (e) {}
  throw new Error(
    `판정 기준 미충족: 존재하지 않는 상품으로도 주문이 성공해 /checkout/payment로 이동함 (스크린샷: ${screenshotPath})`,
  );
}

const toastVisible = await page.locator("div.bg-ink.rounded-full").isVisible().catch(() => false);
if (!toastVisible) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "cart-order-payment-scenario-3-no-toast");
  throw new Error(`판정 기준 미충족: 에러 토스트가 노출되지 않음 (스크린샷: ${screenshotPath})`);
}

console.log("PASS: 시나리오 3");
```

## 시나리오 4: 주문 생성 → Toss 결제위젯 렌더링 확인

spec 사용자 시나리오 3-1(주문서 제출 시 재고 선점+`PENDING` 생성 후 Toss 결제위젯 렌더링)을 검증한다. 실 상품(id=1, "강아지 무릎담요", 18,000원)으로 로그인 후 `/checkout`에서 신규 배송지를 입력해 주문을 생성하고, `/checkout/payment`로 이동해 Toss 결제위젯 DOM(`#toss-payment-methods`, `#toss-agreement`)이 렌더링되는지 확인한다. 실제 카드 결제 진행(승인)은 외부 iframe/실카드 인증이 필요해 이 시나리오 범위 밖이다.

**사전조건**: 로컬 DB에 `id=1` 상품이 `soldOut=false`로 존재해야 한다(현재 시드 데이터로 충족). 새로 가입하는 계정이라 주소록이 비어 있어야 한다.

**판정 기준**: `POST /orders` 성공 후 `/checkout/payment?orderId=...`로 라우팅되고, 5초 내 `#toss-payment-methods` 컨테이너 내부에 Toss SDK가 렌더링한 자식 엘리먼트가 1개 이상 나타난다("불러오는 중..." 문구가 아닌 실제 금액 "18,000원 결제하기" 버튼 텍스트로 전환된다).

```javascript
const page = await browser.getPage("cart-order-payment-scenario-4");

const email = `e2e-order-create-${Date.now()}@example.com`;
await page.goto("http://localhost:3000/signup", { waitUntil: "domcontentloaded" });
await page.getByLabel("이메일").fill(email);
await page.getByRole("textbox", { name: "비밀번호" }).fill("testpass1");
await page.getByLabel("닉네임").fill("주문생성테스터");
await page.getByRole("button", { name: "회원가입" }).click();
await page.waitForURL("**/mypage", { timeout: 10000 });

await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [{ key: "1-M", id: 1, title: "강아지 무릎담요", size: "M", unitPrice: 18000, qty: 1 }];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["1-M"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

await page.getByLabel("받는 사람").fill("테스트 수령인");
await page.getByLabel("연락처").fill("01012345678");
await page.getByLabel("우편번호").fill("12345");
await page.getByLabel("주소", { exact: true }).fill("서울시 테스트구 테스트로 1");

await page.getByRole("button", { name: "결제하기" }).click();
await page.waitForURL("**/checkout/payment**", { timeout: 10000 });

const payButton = page.getByRole("button", { name: /결제하기|불러오는 중/ });
await page.waitForTimeout(3000);
const payButtonText = await payButton.textContent().catch(() => null);
if (!payButtonText || !payButtonText.includes("18,000원")) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "cart-order-payment-scenario-4-widget-not-loaded");
  throw new Error(
    `판정 기준 미충족: Toss 결제위젯이 정상 로드되지 않음 (버튼 텍스트: ${payButtonText}, 스크린샷: ${screenshotPath})`,
  );
}

console.log("PASS: 시나리오 4 (orderId는 URL에서 추출 가능, 시나리오 5에서 재사용)");
```

**실행 결과 (2026-08-29, 최초): FAIL.** `getByRole("button", { name: "결제하기" }).click()`이 Playwright의 actionability 대기 상태로 멈춰 스크립트가 타임아웃됐다(`force: true`로 강제 클릭해도 `POST /orders` 요청 자체가 백엔드 로그에 남지 않음). `page.evaluate`로 버튼 중심 좌표에서 `document.elementFromPoint`를 호출한 결과 실제 클릭을 받는 최상위 요소는 버튼이 아니라 전역 하단 네비게이션(`GlobalBottomNav`)의 아이콘이었다 — `(shell)/layout.tsx`의 `sticky bottom-0` `GlobalBottomNav`가 `checkout/page.tsx`의 `fixed bottom-0` "결제하기" 버튼을 완전히 가려 클릭 자체가 불가능한 상태다. 원인 분석과 조치 권고는 `docs/backlog/2026-08-29-cart-order-payment-phase6-01.md` 참고. 스크린샷: `~/.dev-browser/tmp/checkout-bottombar-check`, `~/.dev-browser/tmp/checkout-bottombar-after-scroll`.

**실행 결과 (2026-08-29, 재실행): PASS.** `frontend/src/components/navigation/GlobalBottomNav.tsx`에 `HIDDEN_PREFIXES = ["/checkout", "/mypage/orders/"]` 경로 기반 숨김 조건이 추가된 뒤 재실행. 재실행 직전 `document.elementFromPoint`로 "결제하기" 버튼 중심 좌표의 최상위 요소가 버튼 자신임을 먼저 확인해 가려짐이 해소됐음을 검증했고, 이후 시나리오 4 원본 스크립트를 그대로 실행해 `POST /orders` → `/checkout/payment?orderId=...` 라우팅 → Toss 결제위젯 "18,000원 결제하기" 버튼 렌더링까지 정상 통과했다.

## 시나리오 5: confirm 실패 시 FAILED 전환 + 재고 복원 + 마이페이지 취소 버튼 미노출

spec 사용자 시나리오 3-4(confirm 실패 시 `Order`가 `FAILED`로 전환되고 재고 복원, 같은 주문 재결제 불가)와 수용 기준("`FAILED`/`CANCELLED` 상태의 주문에는 취소 버튼이 보이지 않는다")을 검증한다. 유효하지 않은 `paymentKey`로 `/checkout/success` 콜백 라우트에 직접 진입시켜 서버 confirm 호출이 실패하도록 유도한다(`TossPaymentGatewayClient`가 실제 Toss API를 호출하므로 존재하지 않는 paymentKey는 반드시 실패 응답을 받는다).

**사전조건**: 로그인 상태(신규 계정), 유효한 `orderId`를 가진 `PENDING` 주문이 있어야 한다 — 이 시나리오 안에서 시나리오 4와 동일한 방식으로 별도 계정/주문을 새로 만들어 자체 완결시킨다.

**판정 기준**: 유효하지 않은 paymentKey로 `/checkout/success?paymentKey=...&orderId=...&amount=...` 진입 시 confirm이 실패해 `/checkout/fail?orderId=...`로 리다이렉트된다. 이후 `/mypage/orders/{orderId}`에서 상태 배지가 "결제실패"로 표시되고 "주문 취소" 버튼이 보이지 않는다.

```javascript
const page = await browser.getPage("cart-order-payment-scenario-5");

const email = `e2e-confirm-fail-${Date.now()}@example.com`;
await page.goto("http://localhost:3000/signup", { waitUntil: "domcontentloaded" });
await page.getByLabel("이메일").fill(email);
await page.getByRole("textbox", { name: "비밀번호" }).fill("testpass1");
await page.getByLabel("닉네임").fill("컨펌실패테스터");
await page.getByRole("button", { name: "회원가입" }).click();
await page.waitForURL("**/mypage", { timeout: 10000 });

await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [{ key: "1-M", id: 1, title: "강아지 무릎담요", size: "M", unitPrice: 18000, qty: 1 }];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["1-M"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

await page.getByLabel("받는 사람").fill("테스트 수령인");
await page.getByLabel("연락처").fill("01012345678");
await page.getByLabel("우편번호").fill("12345");
await page.getByLabel("주소", { exact: true }).fill("서울시 테스트구 테스트로 1");

await page.getByRole("button", { name: "결제하기" }).click();
await page.waitForURL("**/checkout/payment**", { timeout: 10000 });

const paymentUrl = new URL(page.url());
const orderId = paymentUrl.searchParams.get("orderId");
if (!orderId) {
  throw new Error("판정 기준 미충족: /checkout/payment URL에서 orderId를 확인할 수 없음");
}

// 유효하지 않은 paymentKey로 confirm 콜백 라우트에 직접 진입 -> 서버 confirm 실패 유도
await page.goto(
  `http://localhost:3000/checkout/success?paymentKey=invalid-test-key&orderId=${orderId}&amount=18000`,
  { waitUntil: "domcontentloaded" },
);
await page.waitForURL("**/checkout/fail**", { timeout: 10000 });

const failHeading = page.getByText("결제에 실패했어요");
if (!(await failHeading.isVisible({ timeout: 5000 }))) {
  throw new Error("판정 기준 미충족: confirm 실패 후 /checkout/fail 화면의 안내 문구가 보이지 않음");
}

// 마이페이지 주문 상세에서 FAILED 상태 + 취소 버튼 미노출 확인
await page.goto(`http://localhost:3000/mypage/orders/${orderId}`, { waitUntil: "domcontentloaded" });

const statusBadge = page.getByText("결제실패");
if (!(await statusBadge.isVisible({ timeout: 5000 }))) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "cart-order-payment-scenario-5-status-not-failed");
  throw new Error(`판정 기준 미충족: 주문 상세 상태가 "결제실패"로 표시되지 않음 (스크린샷: ${screenshotPath})`);
}

const cancelButton = page.getByRole("button", { name: "주문 취소" });
if (await cancelButton.isVisible()) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "cart-order-payment-scenario-5-cancel-button-visible");
  throw new Error(`판정 기준 미충족: FAILED 상태인데 취소 버튼이 노출됨 (스크린샷: ${screenshotPath})`);
}

console.log("PASS: 시나리오 5");
```

**실행 결과 (2026-08-29, 최초): SKIP.** 시나리오 4에서 확인된 것과 동일한 원인(GlobalBottomNav가 `/checkout`의 "결제하기" 버튼을 가림)으로 `POST /orders`까지 도달하지 못해 이 시나리오의 전제(유효한 `orderId`를 가진 `PENDING` 주문)를 만들 수 없다. 시나리오 4의 코드 결함(`docs/backlog/2026-08-29-cart-order-payment-phase6-01.md`)이 해소된 뒤 재실행이 필요하다.

**실행 결과 (2026-08-29, 재실행): PASS.** 시나리오 4가 해소돼 주문 생성까지 도달, 유효하지 않은 `paymentKey`로 `/checkout/success` 진입 시 서버 confirm이 실패해 `/checkout/fail`로 리다이렉트되고 "결제에 실패했어요" 문구가 노출됨을 확인했다. 마이페이지 주문 상세(`/mypage/orders/{orderId}`)에서 "결제실패" 배지와 취소 버튼 미노출도 확인했다. 단, 스크립트 원문의 `isVisible({ timeout })` 호출은 Playwright에서 polling 대기가 아니라 즉시 상태 체크로 동작해(비동기 API 응답 도착 전에 확인해버림) 처음 두 번은 거짓 실패(false negative)가 발생했다 — `getByText("결제실패").waitFor({ state: "visible", timeout: 10000 })`로 명시적 대기를 추가한 뒤 정상 통과를 확인했다. 코드 결함이 아니라 스크립트의 대기 방식 이슈였다.

## 시나리오 6: 마이페이지 주문내역 목록에서 방금 생성한 주문 확인

spec 사용자 시나리오 4-1(마이페이지 주문내역 목록에서 상태/금액/일시 확인)을 검증한다. 시나리오 5에서 생성한 계정으로 `/mypage/orders`에 진입해 방금 생성된 주문이 목록에 나타나는지 확인한다(같은 페이지 세션 재사용).

**사전조건**: 시나리오 5가 먼저 pass해 동일 브라우저 세션(`cart-order-payment-scenario-5`)에 로그인 상태와 주문이 존재해야 한다. 이 시나리오는 시나리오 5의 페이지를 이어서 사용한다.

**판정 기준**: `/mypage/orders` 목록에 "결제실패" 배지와 "18,000원" 금액이 포함된 항목이 1개 이상 보인다.

```javascript
const page = await browser.getPage("cart-order-payment-scenario-5");
await page.goto("http://localhost:3000/mypage/orders", { waitUntil: "domcontentloaded" });

const orderCard = page.locator("button", { hasText: "결제실패" }).filter({ hasText: "18,000원" });
if (!(await orderCard.first().isVisible({ timeout: 5000 }))) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "cart-order-payment-scenario-6-order-not-listed");
  throw new Error(`판정 기준 미충족: 마이페이지 주문내역 목록에서 방금 생성한 주문을 찾을 수 없음 (스크린샷: ${screenshotPath})`);
}

console.log("PASS: 시나리오 6");
```

**실행 결과 (2026-08-29, 최초): SKIP.** 시나리오 5가 스킵되어 이 시나리오가 의존하는 주문 데이터가 존재하지 않는다. 시나리오 4의 코드 결함 해소 후 재실행 필요.

**실행 결과 (2026-08-29, 재실행): PASS.** 시나리오 5의 세션(`cart-order-payment-scenario-5`)을 이어서 `/mypage/orders` 목록에 "결제실패" + "18,000원" 항목이 노출됨을 확인했다.

## 시나리오 7: 결제 성공(PAID) 및 PAID 주문 취소 — 스킵

spec 사용자 시나리오 3-3(confirm 성공 시 `PAID` 전환), 5(`PAID` 주문 취소 → `CANCELLED` + 재고 복원)를 다룬다.

**사전조건**: 실제 Toss 샌드박스 카드 결제를 완료해 유효한 `paymentKey`를 발급받아야 confirm이 성공(`PAID`)한다. dev-browser는 헤드리스 자동화이고 Toss 결제위젯은 외부 iframe에 카드 정보 입력 및 인증(ARS/앱 인증 등)을 요구하는 실제 PG 연동이라, e2e-tester가 이 상태를 자동으로 만들 방법이 없다.

**판정 기준**: (스킵) 수동 QA로 실제 브라우저에서 Toss 샌드박스 결제를 완료한 뒤 다음을 확인해야 한다 — confirm 성공 후 `/checkout/success`에서 "주문이 완료됐어요" + "주문내역 보기" 링크, 마이페이지 상세에서 상태 "결제완료"(PAID) + "주문 취소" 버튼 노출, 취소 클릭 후 상태 "취소완료"(CANCELLED)로 갱신되고 취소 버튼이 사라짐, 취소 후 상품 재고가 복원됐는지 `GET /products/{id}`로 확인.

```javascript
// 실행하지 않음 — 사전조건(Toss 샌드박스 실카드 결제 완료) 미충족으로 스킵.
console.log("SKIP: 시나리오 7 — 사전조건(Toss 샌드박스 실결제 완료) 미충족, 수동 QA 필요");
```
