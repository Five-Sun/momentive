---
date: 2026-09-07
feature: order-shipping-management
spec: 2026-09-07-order-shipping-management.md
plan: 2026-09-07-order-shipping-management.md
---

# 주문 배송상태·송장 관리 E2E 케이스

관리자 로그인 → 상품 주문(결제 전 `PENDING`) → (Toss 실결제 불가로 SQL 승격) `PAID` → 관리자 목록/상세에서 배송상태·송장 관리(검증 실패 경로 포함) → 고객 주문상세 반영 확인 → 관리자 주문취소 → 권한/상태 오류 API 확인까지 한 유저 플로우(한 탭)로 이어 검증한다.

**Toss 실결제 제약**: 이 spec의 모든 관리 시나리오는 `PAID` 주문을 전제로 하는데, 로컬 환경은 Toss 샌드박스 결제위젯의 실카드 인증을 완료할 수 없어(`docs/backlog/2026-08-30-cart-order-payment-phase4-01.md`와 동일 제약) 브라우저 자동화만으로는 `PENDING`을 넘어설 수 없다. 이 파일은 브라우저로 `PENDING` 주문을 만든 뒤, SQL로 `status='PAID'`, `shipping_status='PREPARING'`을 직접 승격시켜 이어간다(시나리오 2).

**실행 전 로컬에 준비해야 하는 것**
1. 관리자 계정: `admin@momentive.local` / `momentive1234`로 회원가입 후 `UPDATE users SET role='ADMIN' WHERE email='admin@momentive.local';`로 승격(`admin-product-management` E2E와 동일 계정 재사용)
2. 일반 계정: `e2e-shipping-customer@momentive.local` / `customer1234`로 회원가입만(승격 없음) — 시나리오 9의 403 확인용
3. 시드 상품(`V2__seed_product.sql`)이 존재해야 한다 — `product.id = 1`을 그대로 사용한다

**뷰포트**: 1440x900(데스크톱)으로 고정 — `admin-product-management` E2E와 동일 이유(하단 고정 CTA와 본문 CTA 중복 회피).

**셀렉터 노트**
- 로그인 비밀번호 입력은 `#password`로 특정한다(`getByLabel("비밀번호")`가 표시 토글 버튼과 함께 잡혀 strict mode 위반).
- 관리자 주문 목록의 행은 `a[href="/admin/orders/{orderId}"]`의 조상 `tr`로 스코프한다(`AdminOrderTable.tsx`의 반복 렌더 대응, `docs/backlog/2026-08-31-product-review-phase4-02.md` 재발 방지 반영).
- 배송상태 변경 폼의 `<select id="shippingStatus">`/`<select id="courierOption">`는 id 셀렉터로 값을 지정한다(`AdminOrderDetailPage`가 `TextField`처럼 `id`를 그대로 붙임).
- 주문취소는 `window.confirm`을 띄우므로 스크립트 시작 시 `page.on("dialog", ...)`로 자동 수락하도록 등록해둔다.

## 시나리오 1: 관리자 로그인 후 `/admin/orders` 진입, 기본 필터 확인

spec 수용 기준 "관리자가 아닌 로그인 사용자가 `/admin/orders/**`를 호출하면 403"의 반대 경로(관리자는 정상 진입)와 "`/admin/orders` 화면에서 기본 필터가 `PAID`만 보여준다"를 검증한다. 이 시점엔 `PAID` 주문이 하나도 없으므로 빈 상태 문구로 "기본 필터가 PAID 전용"임을 먼저 확인한다.

**사전조건**: 위 준비 1(관리자 계정)이 로컬 DB에 존재.

**판정 기준**: `/admin/orders`에 "주문 관리" 제목이 보이고, 아직 `PAID` 주문이 없으므로 "결제완료 주문이 없어요"가 표시된다(= 기본 필터가 `PAID`로 고정돼 있다는 근거 — 전체 주문 대상이었다면 다른 문구가 뜬다).

## 시나리오 2: 주문 생성(`PENDING`) 후 `PAID` 승격, 기본/전체보기 필터 차이 확인

spec 수용 기준 "`GET /admin/orders`가 파라미터 없이 호출되면 `PAID` 상태 주문만 반환", "`status=...` 전체 지정 시 전체 상태 주문 반환", "주문이 `PAID`로 전환되면 `shippingStatus`가 자동으로 `PREPARING`"을 검증한다.

**사전조건**: 앞 시나리오 상태로 충족(관리자 로그인). 이 시나리오 안에서 결제 전 상태로 상품을 담아 주문을 만든다.

**판정 기준**: 상품(`id=1`)을 장바구니에 담고 결제하기까지 진행하면 `/checkout/payment?orderId=...`로 이동한다(`PENDING` 생성 성공). `/admin/orders`를 "전체보기"로 전환하면 이 주문이 "결제대기" 상태·배송상태 "-"로 보인다. SQL로 `status='PAID'`, `shipping_status='PREPARING'`으로 승격한 뒤 "전체보기"를 끄면(기본 필터) 이 주문이 "결제완료" 상태·배송상태 "배송준비중"으로 보인다.

## 시나리오 3: "배송중" 전환 시 정보 누락 → 인라인 에러

spec 수용 기준 "`shippingStatus=SHIPPING`을 택배사·송장번호 없이 요청하면 400(`SHIPPING_INFO_REQUIRED`)이 반환되고 상태가 바뀌지 않는다"를 검증한다.

**사전조건**: 앞 시나리오 상태로 충족(주문이 `PAID`/`PREPARING`). 관리자 주문 상세(`/admin/orders/{orderId}`)로 진입한 상태에서 시작.

**판정 기준**: 배송상태를 "배송중"으로 바꾸고 택배사·송장번호를 비운 채 "저장하기"를 누르면 폼 안에 에러 메시지("배송중/배송완료로 변경하려면 택배사와 송장번호를 입력해야 합니다.")가 표시되고, 페이지의 주문상태 뱃지는 여전히 배송상태를 "배송준비중"으로 표시한다(저장 실패 = 상태 미변경).

## 시나리오 4: 택배사·송장번호 입력 후 "배송중" 저장 → 고객 화면 반영

spec 수용 기준 "고객용 `GET /orders/{orderId}` 응답에 `shippingStatus`/`courier`/`trackingNumber`가 포함되고 뱃지가 표시된다", "송장번호가 있으면 택배사명과 함께 표시된다"를 검증한다.

**사전조건**: 앞 시나리오 상태로 충족(같은 상세 화면, 폼에 "배송중"이 선택된 채 정보만 비어 있음).

**판정 기준**: 택배사 드롭다운에서 "CJ대한통운"을 고르고 송장번호를 입력해 저장하면 "배송정보를 저장했어요" 토스트가 뜨고 주문 뱃지가 "배송중"으로 바뀐다. `/mypage/orders/{orderId}`로 이동하면 배송상태 뱃지 "배송중"과 "CJ대한통운 {입력한 송장번호}" 텍스트가 함께 보인다.

## 시나리오 5: 택배사 "기타" 자유입력으로 "배송완료" 전환

spec 화면 요구사항 "택배사 드롭다운의 '기타' 선택 시 자유 텍스트 입력이 노출되고, 저장 시 그 값이 `courier`로 전송된다"를 검증한다.

**사전조건**: 앞 시나리오 상태로 충족(관리자 상세 화면으로 복귀).

**판정 기준**: 택배사를 "기타"로 바꾸면 "택배사명 직접 입력" 텍스트 입력이 나타난다. 거기에 임의 택배사명을 입력하고 배송상태를 "배송완료"로 바꿔 저장하면 성공 토스트가 뜨고 주문 뱃지가 "배송완료"로 바뀐다.

## 시나리오 6: "배송완료" → "배송중" 되돌리기 (순서 제약 없음)

spec 수용 기준 "이미 `DELIVERED`인 주문을 `SHIPPING`으로 되돌리는 요청이 성공한다"를 검증한다.

**사전조건**: 앞 시나리오 상태로 충족(배송완료 상태).

**판정 기준**: 배송상태를 다시 "배송중"으로 바꿔 저장하면(택배사·송장번호는 이미 채워져 있어 그대로 제출) 성공 토스트가 뜨고 뱃지가 "배송중"으로 되돌아간다 — 저장이 거부되지 않는다.

## 시나리오 7: 관리자 주문취소 → 배송관리 폼 비활성화

spec 수용 기준 "`POST /admin/orders/{orderId}/cancel`이 `PAID` 주문에 대해 성공하고 재고가 복원되며 `CANCELLED`로 전이", "대상 주문이 `PAID`가 아니면 배송 관리 폼과 취소 버튼이 비활성화된다"를 검증한다.

**사전조건**: 앞 시나리오 상태로 충족(주문이 `PAID`).

**판정 기준**: "주문 취소" 버튼을 누르고 확인 다이얼로그를 수락하면 "주문을 취소했어요" 토스트가 뜨고 주문상태 뱃지가 "취소완료"로 바뀐다. 이후 배송 관리 폼의 모든 입력이 비활성화되고 "결제 완료된 주문만 배송상태를 관리할 수 있어요" 안내가 보이며, 취소 버튼도 비활성화되고 "결제 완료된 주문만 취소할 수 있어요"로 문구가 바뀐다.

## 시나리오 8: (API) 이미 취소된 주문 재취소 시도 → 409

spec 수용 기준 "`PAID`가 아닌 주문에 대해 409(`ORDER_NOT_CANCELLABLE`)를 반환한다"를 검증한다. 화면엔 이미 취소 버튼이 비활성화돼 있으므로, 같은 관리자 세션 쿠키로 API를 직접 호출해 서버 쪽 방어도 확인한다.

**사전조건**: 앞 시나리오 상태로 충족(주문이 `CANCELLED`, 관리자로 로그인된 상태 — 쿠키 유지).

**판정 기준**: `fetch("/admin/orders/{orderId}/cancel", { method: "POST", credentials: "include" })`가 HTTP 409를 반환한다.

## 시나리오 9: (API) 비관리자 계정으로 `/admin/orders` 호출 → 403

spec 수용 기준 "관리자가 아닌 로그인 사용자가 `/admin/orders/**` 엔드포인트를 호출하면 403(`FORBIDDEN`)"을 검증한다.

**사전조건**: 준비 2(일반 계정)가 로컬 DB에 존재. 관리자 세션을 로그아웃하고 일반 계정으로 재로그인한다(화면 조작으로 상태 전환).

**판정 기준**: 일반 계정으로 로그인한 상태에서 `fetch("/admin/orders", { credentials: "include" })`가 HTTP 403을 반환한다.

## 실행 결과 (2026-09-09)

전체 9개 시나리오 PASS. 실행 중 두 가지를 실제로 확인하고 아래 스크립트에 반영했다.

- **Toss 실결제 불가로 인한 분할 실행**: 시나리오 2 중간에 `PENDING` 주문을 SQL로 `PAID`(+`shipping_status='PREPARING'`)로 직접 승격하는 수동 개입이 필요해, 스크립트를 "① 로그인~주문생성" / "② 배송관리~취소~API 검증"의 2단계로 나눠 같은 named page(`order-shipping-management`)로 이어 실행했다. `e2e-format.md`의 "한 파일 = 한 탭" 원칙은 지켰다(탭은 하나로 유지, 중간에 스크립트 프로세스만 재시작).
- **관리자 상세 화면에는 배송상태 뱃지가 없다**: `/admin/orders/[orderId]`는 `<select id="shippingStatus">`의 선택값으로만 현재 배송상태를 보여주고 별도 `Badge`를 렌더링하지 않는다(뱃지는 목록 화면과 고객 화면에만 있음). 처음 작성한 스크립트는 상세 화면에서도 `getByText("배송중")`류로 검증하려다 실패했고, `#shippingStatus`의 `inputValue()`로 검증하도록 고쳤다(아래 스크립트에 반영됨).
- **발견 및 수정**: 시나리오 7에서 이미 "배송중"으로 표시된 주문을 관리자가 취소하면, `shipping_status`는 `SHIPPING`으로 남은 채 `status`만 `CANCELLED`로 바뀐다(도메인상 취소가 배송상태를 초기화하지 않음 — 관리자가 취소 직전의 마지막 배송단계를 볼 수 있어야 해서 의도적으로 그렇게 둠). 그 결과 고객 주문상세는 취소된 주문에도 "배송중" 뱃지를 계속 보여주는 문제가 있었다. `mypage/orders/[orderId]`의 뱃지 노출 조건에 `order.status === "PAID"`를 추가해 고쳤고(관리자 화면은 그대로 둠 — 운영자에게는 마지막 배송단계 정보가 유용), 실제 취소된 주문(#1)으로 뱃지가 사라지는 것을 dev-browser로 재확인했다.

## 실행 스크립트

```javascript
const ADMIN_EMAIL = "admin@momentive.local";
const ADMIN_PASSWORD = "momentive1234";
const CUSTOMER_EMAIL = "e2e-shipping-customer@momentive.local";
const CUSTOMER_PASSWORD = "customer1234";
const PRODUCT_ID = 1;

const page = await browser.getPage("order-shipping-management");
await page.setViewportSize({ width: 1440, height: 900 });
page.on("dialog", (dialog) => dialog.accept());

async function waitFor(predicate, description, timeoutMs = 15000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const v = await predicate().catch(() => null);
    if (v) return v;
    await page.waitForTimeout(200);
  }
  throw new Error(description);
}

async function fail(scenario, message, shot) {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, `order-shipping-management-scenario-${scenario}-${shot}`);
  throw new Error(`시나리오 ${scenario} 판정 기준 미충족: ${message} (스크린샷: ${path})`);
}

async function login(email, password) {
  await page.goto("http://localhost:3000/login", { waitUntil: "load" });
  await page.waitForTimeout(6000); // 하이드레이션 대기 (admin-product-management E2E와 동일 이유)
  await page.locator("#email").fill(email);
  await page.locator("#password").fill(password);
  await page.getByRole("button", { name: "로그인" }).click();
  await page.waitForURL("**/mypage", { timeout: 10000 });
}

async function logout() {
  await page.evaluate(async () => {
    await fetch("/api/auth/logout", { method: "POST", credentials: "include" }).catch(() => {});
  });
  await page.goto("http://localhost:3000/login", { waitUntil: "load" });
}

// ============================================================
// 시나리오 1: 관리자 로그인 후 /admin/orders 진입, 기본 필터 확인
// ============================================================
await login(ADMIN_EMAIL, ADMIN_PASSWORD);

await page.goto("http://localhost:3000/admin/orders", { waitUntil: "domcontentloaded" });
await page
  .getByRole("heading", { name: "주문 관리" })
  .waitFor({ state: "visible", timeout: 10000 })
  .catch(async () => fail(1, '"주문 관리" 제목이 보이지 않음', "no-heading"));

await page
  .getByText("결제완료 주문이 없어요")
  .waitFor({ state: "visible", timeout: 10000 })
  .catch(async () => fail(1, "기본 필터(PAID) 빈 상태 문구가 보이지 않음 — 이미 PAID 주문이 있는 환경일 수 있음", "no-empty-state"));

console.log("PASS: 시나리오 1");

// ============================================================
// 시나리오 2: 주문 생성(PENDING) 후 PAID 승격
// ============================================================
await page.evaluate(() => {
  window.localStorage.removeItem("momentive:cart");
  window.sessionStorage.removeItem("momentive:checkout-selection");
});

await page.goto(`http://localhost:3000/products/${PRODUCT_ID}`, { waitUntil: "domcontentloaded" });
const addToCartButton = page.locator("button:visible", { hasText: "장바구니 담기" });
await addToCartButton.waitFor({ state: "visible", timeout: 10000 }).catch(async () =>
  fail(2, "상품상세에 장바구니 담기 버튼이 보이지 않음", "no-add-to-cart"));
await addToCartButton.click();
await page.getByText("장바구니에 담았어요").waitFor({ state: "visible", timeout: 5000 }).catch(() => {});

await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
const checkoutButton = page.locator("button:visible", { hasText: "구매하기" });
await checkoutButton.waitFor({ state: "visible", timeout: 5000 });
await checkoutButton.click();
await page.waitForURL("**/checkout", { timeout: 10000 });

const recipientInput = page.locator("#recipient");
const hasNewAddressForm = await recipientInput
  .waitFor({ state: "visible", timeout: 10000 })
  .then(() => true)
  .catch(() => false);
if (hasNewAddressForm) {
  await recipientInput.fill("E2E 수령인");
  await page.locator("#phone").fill("01012345678");
  await page.locator("#zipcode").fill("12345");
  await page.locator("#address1").fill("서울시 테스트구 테스트로 1");
}

const payButton = page.locator("button:visible", { hasText: "결제하기" });
await payButton.waitFor({ state: "visible", timeout: 5000 });
await payButton.click();

const orderId = await waitFor(
  async () => {
    const m = page.url().match(/orderId=(\d+)/);
    return m ? m[1] : null;
  },
  "시나리오 2 판정 기준 미충족: 결제하기 후 /checkout/payment?orderId=...로 이동하지 않음",
);

await page.goto("http://localhost:3000/admin/orders", { waitUntil: "domcontentloaded" });
const showAllButton = page.getByRole("button", { name: /^전체보기/ });
await showAllButton.click();

const pendingRowLink = page.locator(`a[href="/admin/orders/${orderId}"]`);
await pendingRowLink.waitFor({ state: "visible", timeout: 10000 }).catch(async () =>
  fail(2, `전체보기에서 방금 만든 주문(#${orderId})이 보이지 않음`, "no-pending-row"));
let rowText = (await pendingRowLink.locator("xpath=ancestor::tr").textContent()) || "";
if (!rowText.includes("결제대기")) {
  await fail(2, `주문이 결제대기 상태로 표시되지 않음 (행 텍스트: ${rowText})`, "not-pending");
}

// Toss 실결제 불가 — SQL로 직접 PAID 승격 (docs/backlog/2026-08-30-cart-order-payment-phase4-01.md와 동일 제약)
console.log(`PENDING orderId=${orderId} 확보. 다음 명령으로 PAID 승격 후 계속 진행:`);
console.log(
  `docker exec -i backend-db-1 psql -U momentive -d momentive -c "UPDATE orders SET status='PAID', shipping_status='PREPARING' WHERE id=${orderId};"`,
);
```

**여기서 위 SQL을 실제로 실행한 뒤**, 같은 named page(`order-shipping-management`)로 두 번째 스크립트를 이어 실행한다(`orderId`는 위에서 확보한 값을 그대로 상수로 채워 넣는다).

```javascript
const CUSTOMER_EMAIL = "e2e-shipping-customer@momentive.local";
const CUSTOMER_PASSWORD = "customer1234";
const orderId = "<앞 스크립트가 출력한 orderId>";

const page = await browser.getPage("order-shipping-management");
page.on("dialog", (dialog) => dialog.accept());

async function fail(scenario, message, shot) {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, `order-shipping-management-scenario-${scenario}-${shot}`);
  throw new Error(`시나리오 ${scenario} 판정 기준 미충족: ${message} (스크린샷: ${path})`);
}

async function login(email, password) {
  await page.goto("http://localhost:3000/login", { waitUntil: "load" });
  await page.waitForTimeout(6000);
  await page.locator("#email").fill(email);
  await page.locator("#password").fill(password);
  await page.getByRole("button", { name: "로그인" }).click();
  await page.waitForURL("**/mypage", { timeout: 10000 });
}

// ============================================================
// 시나리오 2 (뒷부분): PAID 승격 확인 (기본 필터에 노출)
// ============================================================
await page.goto("http://localhost:3000/admin/orders", { waitUntil: "domcontentloaded" });
const isShowAllOn = await page.getByRole("button", { name: "전체보기 · 켜짐" }).isVisible().catch(() => false);
if (isShowAllOn) {
  await page.getByRole("button", { name: "전체보기 · 켜짐" }).click();
}

const paidRowLink = page.locator(`a[href="/admin/orders/${orderId}"]`);
await paidRowLink.waitFor({ state: "visible", timeout: 10000 }).catch(async () =>
  fail(2, `기본 필터(PAID)에서 승격된 주문(#${orderId})이 보이지 않음`, "not-in-default"));
let rowText = (await paidRowLink.locator("xpath=ancestor::tr").textContent()) || "";
if (!rowText.includes("결제완료")) {
  await fail(2, `주문상태가 결제완료로 표시되지 않음 (행 텍스트: ${rowText})`, "not-paid");
}
if (!rowText.includes("배송준비중")) {
  await fail(2, `배송상태가 배송준비중으로 표시되지 않음 (행 텍스트: ${rowText})`, "not-preparing");
}
console.log("PASS: 시나리오 2");

// ============================================================
// 시나리오 3: 정보 없이 "배송중" 저장 → 인라인 에러
// 관리자 상세 화면에는 배송상태 뱃지가 따로 없다 — <select id="shippingStatus">의
// inputValue()로 "저장 실패 후 서버 상태가 그대로인지"를 확인한다(새로고침 후 재확인).
// ============================================================
await paidRowLink.click();
await page.waitForURL(`**/admin/orders/${orderId}`, { timeout: 10000 });
await page.locator("#shippingStatus").waitFor({ state: "visible", timeout: 10000 });

await page.locator("#shippingStatus").selectOption("SHIPPING");
await page.getByRole("button", { name: "저장하기" }).click();

await page
  .getByText("배송중/배송완료로 변경하려면 택배사와 송장번호를 입력해야 합니다.")
  .waitFor({ state: "visible", timeout: 5000 })
  .catch(async () => fail(3, "정보 없이 저장 시 인라인 에러 메시지가 보이지 않음", "no-inline-error"));

await page.goto(`http://localhost:3000/admin/orders/${orderId}`, { waitUntil: "domcontentloaded" });
await page.locator("#shippingStatus").waitFor({ state: "visible", timeout: 10000 });
let currentValue = await page.locator("#shippingStatus").inputValue();
if (currentValue !== "PREPARING") {
  await fail(3, `새로고침 후 서버 상태가 PREPARING이 아님(저장 실패가 서버에 반영 안 됐을 수 있음) — 실제: ${currentValue}`, "server-state-changed");
}
console.log("PASS: 시나리오 3");

// ============================================================
// 시나리오 4: 택배사·송장번호 입력 후 "배송중" 저장 → 고객 화면 반영
// ============================================================
const TRACKING_NUMBER = "123456789012";
await page.locator("#shippingStatus").selectOption("SHIPPING");
await page.locator("#courierOption").selectOption("CJ대한통운");
await page.locator("#trackingNumber").fill(TRACKING_NUMBER);
await page.getByRole("button", { name: "저장하기" }).click();

await page
  .getByText("배송정보를 저장했어요")
  .waitFor({ state: "visible", timeout: 5000 })
  .catch(async () => fail(4, "저장 성공 토스트가 보이지 않음", "no-toast"));
await page.waitForTimeout(500);
currentValue = await page.locator("#shippingStatus").inputValue();
if (currentValue !== "SHIPPING") {
  await fail(4, `저장 후 select 값이 SHIPPING이 아님 (실제: ${currentValue})`, "select-not-shipping");
}

await page.goto(`http://localhost:3000/mypage/orders/${orderId}`, { waitUntil: "domcontentloaded" });
await page
  .getByText("배송중")
  .first()
  .waitFor({ state: "visible", timeout: 10000 })
  .catch(async () => fail(4, "고객 주문상세에 배송중 뱃지가 보이지 않음", "customer-no-badge"));
await page
  .getByText(`CJ대한통운 ${TRACKING_NUMBER}`)
  .waitFor({ state: "visible", timeout: 5000 })
  .catch(async () => fail(4, "고객 주문상세에 택배사+송장번호 텍스트가 보이지 않음", "customer-no-tracking"));
console.log("PASS: 시나리오 4");

// ============================================================
// 시나리오 5: 기타 택배사 자유입력 + "배송완료"
// ============================================================
await page.goto(`http://localhost:3000/admin/orders/${orderId}`, { waitUntil: "domcontentloaded" });
await page.locator("#courierOption").waitFor({ state: "visible", timeout: 10000 });
await page.locator("#courierOption").selectOption("기타");

const courierOtherInput = page.getByLabel("택배사명 직접 입력");
await courierOtherInput.waitFor({ state: "visible", timeout: 5000 }).catch(async () =>
  fail(5, '"기타" 선택 시 자유 텍스트 입력이 나타나지 않음', "no-other-input"));
await courierOtherInput.fill("퀵서비스");
await page.locator("#shippingStatus").selectOption("DELIVERED");
await page.getByRole("button", { name: "저장하기" }).click();

await page
  .getByText("배송정보를 저장했어요")
  .waitFor({ state: "visible", timeout: 5000 })
  .catch(async () => fail(5, "기타 택배사 저장 성공 토스트가 보이지 않음", "no-toast"));
await page.waitForTimeout(500);
currentValue = await page.locator("#shippingStatus").inputValue();
if (currentValue !== "DELIVERED") {
  await fail(5, `저장 후 select 값이 DELIVERED가 아님 (실제: ${currentValue})`, "select-not-delivered");
}
const savedCourierOption = await page.locator("#courierOption").inputValue();
if (savedCourierOption !== "기타") {
  await fail(5, `기타 택배사 저장 후 courierOption이 기타로 유지되지 않음 (실제: ${savedCourierOption})`, "courier-not-other");
}
console.log("PASS: 시나리오 5");

// ============================================================
// 시나리오 6: "배송완료" → "배송중" 되돌리기 (순서 제약 없음)
// ============================================================
await page.locator("#shippingStatus").selectOption("SHIPPING");
await page.getByRole("button", { name: "저장하기" }).click();

await page
  .getByText("배송정보를 저장했어요")
  .waitFor({ state: "visible", timeout: 5000 })
  .catch(async () => fail(6, "되돌리기 저장 성공 토스트가 보이지 않음", "no-toast"));
await page.waitForTimeout(500);
currentValue = await page.locator("#shippingStatus").inputValue();
if (currentValue !== "SHIPPING") {
  await fail(6, `되돌리기 후 select 값이 SHIPPING이 아님 (실제: ${currentValue})`, "select-not-shipping-2");
}
console.log("PASS: 시나리오 6");

// ============================================================
// 시나리오 7: 관리자 주문취소 → 배송관리 폼 비활성화
// ============================================================
await page.getByRole("button", { name: "주문 취소" }).click();

await page
  .getByText("주문을 취소했어요")
  .waitFor({ state: "visible", timeout: 5000 })
  .catch(async () => fail(7, "취소 성공 토스트가 보이지 않음", "no-toast"));
await page
  .getByText("취소완료")
  .first()
  .waitFor({ state: "visible", timeout: 5000 })
  .catch(async () => fail(7, "취소 후 주문상태 뱃지가 취소완료로 바뀌지 않음", "not-cancelled"));

const shippingSelectDisabled = await page.locator("#shippingStatus").isDisabled();
if (!shippingSelectDisabled) {
  await fail(7, "취소 후에도 배송상태 select가 비활성화되지 않음", "select-not-disabled");
}
await page
  .getByText("결제 완료된 주문만 배송상태를 관리할 수 있어요")
  .waitFor({ state: "visible", timeout: 5000 })
  .catch(async () => fail(7, "배송관리 폼 비활성 안내 문구가 보이지 않음", "no-disabled-notice"));
await page
  .getByText("결제 완료된 주문만 취소할 수 있어요")
  .waitFor({ state: "visible", timeout: 5000 })
  .catch(async () => fail(7, "취소 버튼 비활성 안내 문구가 보이지 않음", "no-cancel-disabled-notice"));
console.log("PASS: 시나리오 7");

// ============================================================
// 시나리오 8: (API) 이미 취소된 주문 재취소 시도 → 409
// ============================================================
const recancelStatus = await page.evaluate(async (id) => {
  const res = await fetch(`http://localhost:8081/admin/orders/${id}/cancel`, { method: "POST", credentials: "include" });
  return res.status;
}, orderId);
if (recancelStatus !== 409) {
  await fail(8, `재취소 요청이 409가 아닌 ${recancelStatus}를 반환함`, "not-409");
}
console.log("PASS: 시나리오 8");

// ============================================================
// 시나리오 9: (API) 비관리자 계정으로 /admin/orders 호출 → 403
// ============================================================
await login(CUSTOMER_EMAIL, CUSTOMER_PASSWORD);
const forbiddenStatus = await page.evaluate(async () => {
  const res = await fetch("http://localhost:8081/admin/orders", { credentials: "include" });
  return res.status;
});
if (forbiddenStatus !== 403) {
  await fail(9, `비관리자 계정의 /admin/orders 호출이 403이 아닌 ${forbiddenStatus}를 반환함`, "not-403");
}
console.log("PASS: 시나리오 9");

console.log("ALL SCENARIOS PASSED");
```

`/admin/orders` API 호출(`fetch`)은 프론트(3000)가 아니라 백엔드(8081)를 직접 가리켜야 한다 — `apiFetch`(`src/lib/api/client.ts`)가 `NEXT_PUBLIC_API_BASE_URL`로 절대경로를 쓰는 것과 동일한 이유(상대경로로 쓰면 Next.js 서버로 요청이 가 404가 난다).
