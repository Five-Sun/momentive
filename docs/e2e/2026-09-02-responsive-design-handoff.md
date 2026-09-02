---
date: 2026-09-02
feature: responsive-design-handoff
spec: 2026-09-02-responsive-design-handoff.md
plan: 2026-09-02-responsive-design-handoff.md
---

# 디자인 핸드오프 2차 이관 (폰트·모션·데스크톱 반응형) E2E 케이스

DB는 `./dev.sh` 재기동으로 재시딩된 상태(상품 15건, id 1~15, 리뷰/주문/유저 0건)다. 리뷰 작성/수정은 `PAID` 주문(구매 확인)이 있어야 리뷰 폼이 노출되는데, `PAID` 전환은 Toss 샌드박스 실카드 결제가 필요해(`docs/e2e/2026-08-29-cart-order-payment.md` 시나리오 7과 동일한 제약) 이 세션에서 만들 수 없다. 따라서 리뷰 플로우는 "구매 이력 없는 상태에서 리뷰 쓰기 버튼 비노출 + 빈 상태 문구가 두 폭에서 모두 정상 렌더"까지만 반응형 회귀로 다루고, 실제 작성/수정 제출은 사전조건 미충족으로 스킵한다.

**실행 결과 (2026-09-02)**: 시나리오 1~7 전부 PASS. 시나리오 7의 리뷰 작성/수정 자체는 위 제약으로 스킵(실패 아님). 스크립트 작성 과정에서 두 가지를 그 자리에서 고쳤다(둘 다 코드 결함 아님) — (1) 시나리오 4 최초 작성 시 상품 id=9(할인가 33,600원)를 정가 42,000원으로 착각해 장바구니에 주입, 서버가 실제 할인가로 재계산해 무료배송 임계값(70,000원) 검증이 어긋남 → 할인가 없는 정가 상품 4종(id=1,3,8,11 합계 70,000원)으로 교체해 해결. (2) Tailwind arbitrary-value 클래스(`lg:grid-cols-[1fr_340px]`)를 포함한 다중 클래스 결합 CSS 셀렉터(`div.lg\:grid.lg\:grid-cols-\[1fr_340px\]`)가 Playwright에서 매칭되지 않아(단일 클래스 셀렉터는 매칭됨) 판정 실패 분기로 빠지고, 그 안의 `page.screenshot()` 호출이 QuickJS 샌드박스에서 폰트 로딩 대기로 30초 액션 타임아웃까지 걸려 스크립트가 중단됨 → 단일 클래스 셀렉터(`.lg\:grid-cols-\[1fr_340px\]`)로 교체해 해결. 또한 dev-browser 기본 스크립트 타임아웃(30초)이 7개 시나리오 전체를 순차 실행하기엔 부족해 `--timeout 300`으로 늘려 실행했다.

## 시나리오 1: 모바일 폭(390px) — 하단 탭바 노출, 상단 네비 미노출

spec 사용자 시나리오 1-1, 수용 기준 "<1024px에서 하단 탭바 5탭이 보이고 상단 네비가 보이지 않는다"에서 도출.

**사전조건**: 없음(첫 진입).

**판정 기준**: 뷰포트 390x844에서 `/` 진입 시 `GlobalBottomNav`(5탭: 홈/카테고리/검색/위시/마이) 컨테이너가 보이고, `TopNav`(로고+링크4개+검색창) 컨테이너는 보이지 않는다.

## 시나리오 2: 데스크톱 폭(1440px) — 상단 네비 노출, 하단 탭바 미노출

spec 사용자 시나리오 2-1, 수용 기준 "≥1024px에서 상단 네비가 보이고 하단 탭바가 보이지 않는다"에서 도출. 같은 탭에서 뷰포트만 1440x900으로 바꿔 전환을 확인한다(1024px 경계 전환 시나리오).

**사전조건**: 시나리오 1 상태 이어받음(같은 `/` 화면).

**판정 기준**: 뷰포트를 1440x900으로 바꾸면 `TopNav`가 보이고 `GlobalBottomNav`는 사라진다. `TopNav` 안에 로고 링크, "홈"/"카테고리"/"위시"/"마이" 링크, 검색 입력창(placeholder "브랜드, 상품 검색"), 장바구니 아이콘 링크(`/cart`)가 모두 보인다.

## 시나리오 3: 장바구니 → 체크아웃, 배송비 임계값 미달 + 제주 할증 (모바일 390px)

spec 사용자 시나리오 1(모바일 경험 유지), 수용 기준 "무료배송 임계값이 70,000원이고 제주 4,000원 할증이 유지된다"를 모바일 폭에서 회귀 확인한다. 상품금액을 32,000원(id=3 강아지 하네스 M, 70,000원 미달)으로 담고 제주 우편번호(63100)를 입력해 기본 배송비(3,400원) + 제주 할증(4,000원) = 7,400원이 반영되는지 확인한다.

**사전조건**: 뷰포트 390x844. 신규 회원가입.

**판정 기준**: `/checkout`에서 상품금액 "32,000원" 확인 후, 제주 우편번호(63100) 입력 시 배송비가 "7,400원"으로 표시되고 총 결제금액이 "39,400원"으로 계산된다. 모바일 fixed 하단 CTA("결제하기")가 화면에 보이고, `TopNav`는 보이지 않는다.

## 시나리오 4: 장바구니 → 체크아웃 → 주문 생성, 무료배송 임계값 충족 (데스크톱 1440px)

spec 사용자 시나리오 2(데스크톱 경험), 수용 기준 "데스크톱 `/cart`·`/checkout`에 우측 340px 요약 컬럼이 있고 결제 CTA가 그 안에 sticky로 있다", "데스크톱에서 `fixed` 하단 CTA 바가 남아 있는 화면이 없고, 어느 폭에서도 CTA가 네비게이션에 가려지지 않는다"를 검증한다. 상품금액을 정확히 70,000원(id=1 강아지 무릎담요 18,000원 + id=3 강아지 하네스 M 32,000원 + id=8 오리 육포 100g 9,000원 + id=11 강아지 칫솔세트 11,000원, 전부 할인가 없는 정가 상품)으로 담아 무료배송이 표시되는지, 그리고 주문 생성까지 완료해 결제위젯 진입이 정상 동작하는지 확인한다.

**사전조건**: 뷰포트 1440x900. 신규 회원가입.

**판정 기준**: `/checkout`에서 상품금액 "70,000원", 배송비 "무료" 확인. 요약 컬럼 안(`lg:sticky lg:top-8` 영역)의 "결제하기" 버튼으로 제출 시 `/checkout/payment?orderId=...`로 이동하고 결제위젯 버튼에 "70,000원"이 반영된다. 데스크톱 폭에서 하단 고정(`fixed bottom-0`) CTA 바가 보이지 않고 `GlobalBottomNav`도 보이지 않는다(TopNav만 보임).

## 시나리오 5: 쿠폰 등록 · 체크아웃 선택 · 할인 반영 (모바일 390px)

spec 사용자 시나리오 1(쿠폰 등록 → 체크아웃 선택 → 할인 반영)의 모바일 회귀. 쿠폰 시스템(`docs/e2e/2026-09-01-coupon-system.md`)에서 이미 검증된 플로우를 이번 반응형 변경 이후에도 모바일 폭에서 깨지지 않는지만 얕게 확인한다.

**사전조건**: 시나리오 3 상태(같은 계정, 로그인 유지) 이어받음. 뷰포트 390x844 유지.

**판정 기준**: `/mypage/coupons`에서 쿠폰 코드 `WELCOME3000`(3,000원 정액, 최소주문금액 30,000원) 등록 시 "웰컴 3,000원 할인" 카드가 "사용 가능한 쿠폰" 목록에 나타나고 "쿠폰을 등록했어요" 토스트가 뜬다. 상품금액 32,000원 장바구니로 `/checkout` 진입 후 해당 쿠폰 선택 시 "쿠폰 할인" 줄에 "-3,000원"이 나타나고 총 결제금액이 3,000원 감소한다.

## 시나리오 6: 쿠폰 선택 · 할인 반영 (데스크톱 1440px)

spec 사용자 시나리오 2(데스크톱 체크아웃 2단 레이아웃) + 쿠폰 할인 반영을 데스크톱 폭에서 회귀 확인한다. 쿠폰 선택 UI가 요약 컬럼이 아닌 좌측 본문 영역에 있어도 데스크톱 2단 레이아웃에서 정상 동작하는지 확인한다.

**사전조건**: 시나리오 4 상태(같은 계정, 로그인 유지) 이어받음. 뷰포트 1440x900 유지.

**판정 기준**: `/mypage/coupons`에서 `FIRSTORDER5000`(5,000원 정액, 최소주문금액 0) 등록 후, 상품 1개(id=1 강아지 무릎담요 18,000원)로 `/checkout` 진입 시 데스크톱 2단 레이아웃(`lg:grid-cols-[1fr_340px]`)에서 쿠폰 선택 버튼이 좌측 영역에 보이고, 선택 시 우측 요약 컬럼의 "쿠폰 할인" 줄에 "-5,000원"이 즉시 반영된다.

## 시나리오 7: 구매 이력 없는 상품상세에서 리뷰 쓰기 버튼 미노출 (모바일 → 데스크톱 전환)

spec 수용 기준(리뷰 작성/수정 폼)의 반응형 회귀 범위 축소판. `PAID` 주문을 만들 수 없는 이 환경 제약상, 비로그인/미구매 상태의 리뷰 영역(빈 상태 문구)이 두 폭 모두에서 레이아웃 붕괴 없이 렌더되는지만 확인한다.

**사전조건**: 시나리오 6에서 이어지는 로그인 세션. 뷰포트는 이 시나리오 안에서 390x844 → 1440x900 순서로 전환.

**판정 기준**: `/products/1` 진입 시(구매 이력 없는 상품) 390px 폭에서 "아직 작성된 리뷰가 없어요." 문구가 보이고 "리뷰 쓰기" 버튼은 보이지 않는다(미구매 상태). 뷰포트를 1440px로 전환해도 같은 문구가 유지되고 레이아웃이 깨지지 않는다(좌 이미지/우 정보 2단 구조, 하단 `fixed` CTA 미노출).

**리뷰 작성/수정 자체 시나리오는 스킵**: `PAID` 주문 생성이 Toss 샌드박스 실카드 결제를 요구해 이 환경에서 사전조건을 만들 수 없다(사전조건 미충족).

## 실행 스크립트

```javascript
const page = await browser.getPage("responsive-design-handoff");

async function signup(nickname) {
  const email = `e2e-responsive-${Date.now()}-${Math.floor(Math.random() * 10000)}@example.com`;
  await page.goto("http://localhost:3000/signup", { waitUntil: "domcontentloaded" });
  await page.getByLabel("이메일").fill(email);
  await page.getByRole("textbox", { name: "비밀번호" }).fill("testpass1");
  await page.getByLabel("닉네임").fill(nickname);
  await page.getByRole("button", { name: "회원가입" }).click();
  await page.waitForURL("**/mypage", { timeout: 10000 });
  return email;
}

async function waitForLocatorText(locator, predicate, description, timeoutMs = 6000) {
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
// 시나리오 1: 모바일 폭(390px) — 하단 탭바 노출, 상단 네비 미노출
// ============================================================
await page.setViewportSize({ width: 390, height: 844 });
await page.goto("http://localhost:3000/", { waitUntil: "domcontentloaded" });
await page.waitForTimeout(500);

const bottomNavMobile = page.locator("div.lg\\:hidden", { has: page.getByRole("button", { name: "마이" }) }).first();
if (!(await bottomNavMobile.isVisible({ timeout: 5000 }).catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "responsive-design-handoff-scenario-1-no-bottomnav");
  throw new Error("시나리오 1 판정 기준 미충족: 모바일 폭에서 하단 탭바가 보이지 않음");
}

const topNavMobile = page.locator("div.lg\\:flex").first();
if (await topNavMobile.isVisible({ timeout: 2000 }).catch(() => false)) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "responsive-design-handoff-scenario-1-topnav-visible");
  throw new Error("시나리오 1 판정 기준 미충족: 모바일 폭에서 상단 네비가 보임");
}

console.log("PASS: 시나리오 1");

// ============================================================
// 시나리오 2: 데스크톱 폭(1440px) — 상단 네비 노출, 하단 탭바 미노출
// ============================================================
await page.setViewportSize({ width: 1440, height: 900 });
await page.waitForTimeout(500);

const topNavDesktop = page.locator("div.lg\\:flex").first();
if (!(await topNavDesktop.isVisible({ timeout: 5000 }).catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "responsive-design-handoff-scenario-2-no-topnav");
  throw new Error("시나리오 2 판정 기준 미충족: 데스크톱 폭에서 상단 네비가 보이지 않음");
}

for (const label of ["홈", "카테고리", "위시", "마이"]) {
  const link = topNavDesktop.getByRole("link", { name: label, exact: true });
  if (!(await link.isVisible({ timeout: 3000 }).catch(() => false))) {
    const buf = await page.screenshot();
    await saveScreenshot(buf, "responsive-design-handoff-scenario-2-missing-link");
    throw new Error(`시나리오 2 판정 기준 미충족: 상단 네비에 "${label}" 링크가 보이지 않음`);
  }
}

const searchInput = page.getByPlaceholder("브랜드, 상품 검색");
if (!(await searchInput.isVisible({ timeout: 3000 }).catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "responsive-design-handoff-scenario-2-no-search");
  throw new Error("시나리오 2 판정 기준 미충족: 상단 네비 검색 입력창이 보이지 않음");
}

const bottomNavDesktop = page.locator("div.lg\\:hidden", { has: page.getByRole("button", { name: "마이" }) }).first();
if (await bottomNavDesktop.isVisible({ timeout: 2000 }).catch(() => false)) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "responsive-design-handoff-scenario-2-bottomnav-visible");
  throw new Error("시나리오 2 판정 기준 미충족: 데스크톱 폭에서 하단 탭바가 보임");
}

console.log("PASS: 시나리오 2");

// ============================================================
// 시나리오 3: 장바구니 -> 체크아웃, 배송비 임계값 미달 + 제주 할증 (모바일 390px)
// ============================================================
await page.setViewportSize({ width: 390, height: 844 });
await signup("반응형모바일테스터");

await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [{ key: "3-M", id: 3, title: "강아지 하네스 M", size: "M", unitPrice: 32000, qty: 1 }];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["3-M"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

const recipientInput3 = page.getByLabel("받는 사람");
if (await recipientInput3.isVisible({ timeout: 3000 }).catch(() => false)) {
  await recipientInput3.fill("테스트 수령인");
  await page.getByLabel("연락처").fill("01012345678");
}

await waitForLocatorText(
  page.locator("text=상품금액").locator("..").locator("span").last(),
  (t) => t.includes("32,000"),
  "시나리오 3 사전 확인 실패: 체크아웃 상품금액이 32,000원으로 반영되지 않음",
);

await page.getByLabel("우편번호").fill("63100");
await page.getByLabel("주소", { exact: true }).fill("제주특별자치도 제주시 테스트로 1");

const shippingLine3 = page.locator("text=배송비").locator("..").locator("span").last();
await waitForLocatorText(
  shippingLine3,
  (t) => t.includes("7,400"),
  "시나리오 3 판정 기준 미충족: 제주 우편번호 입력 후 배송비가 7,400원(기본 3,400 + 할증 4,000)으로 표시되지 않음",
);

const totalLine3 = page.locator("text=총 결제금액").locator("..").locator("span").last();
await waitForLocatorText(
  totalLine3,
  (t) => t.includes("39,400"),
  "시나리오 3 판정 기준 미충족: 총 결제금액이 39,400원(32,000 + 7,400)으로 반영되지 않음",
);

const mobileFixedCta3 = page.locator("div.fixed.bottom-0.lg\\:hidden", { hasText: "결제하기" });
if (!(await mobileFixedCta3.isVisible({ timeout: 3000 }).catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "responsive-design-handoff-scenario-3-no-mobile-cta");
  throw new Error("시나리오 3 판정 기준 미충족: 모바일 폭에서 하단 고정 결제하기 CTA가 보이지 않음");
}

if (await page.locator("div.lg\\:flex").first().isVisible({ timeout: 2000 }).catch(() => false)) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "responsive-design-handoff-scenario-3-topnav-visible");
  throw new Error("시나리오 3 판정 기준 미충족: 모바일 폭 체크아웃에서 상단 네비가 보임");
}

console.log("PASS: 시나리오 3");

// ============================================================
// 시나리오 4: 장바구니 -> 체크아웃 -> 주문 생성, 무료배송 임계값 충족 (데스크톱 1440px)
// ============================================================
await page.setViewportSize({ width: 1440, height: 900 });
await signup("반응형데스크톱테스터");

await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [
    { key: "1-M", id: 1, title: "강아지 무릎담요", size: "M", unitPrice: 18000, qty: 1 },
    { key: "3-M", id: 3, title: "강아지 하네스 M", size: "M", unitPrice: 32000, qty: 1 },
    { key: "8-M", id: 8, title: "오리 육포 100g", size: "M", unitPrice: 9000, qty: 1 },
    { key: "11-M", id: 11, title: "강아지 칫솔세트", size: "M", unitPrice: 11000, qty: 1 },
  ];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["1-M", "3-M", "8-M", "11-M"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

const recipientInput4 = page.getByLabel("받는 사람");
if (await recipientInput4.isVisible({ timeout: 3000 }).catch(() => false)) {
  await recipientInput4.fill("테스트 수령인");
  await page.getByLabel("연락처").fill("01012345678");
  await page.getByLabel("우편번호").fill("12345");
  await page.getByLabel("주소", { exact: true }).fill("서울시 테스트구 테스트로 1");
}

await waitForLocatorText(
  page.locator("text=상품금액").locator("..").locator("span").last(),
  (t) => t.includes("70,000"),
  "시나리오 4 사전 확인 실패: 체크아웃 상품금액이 70,000원으로 반영되지 않음",
);

const shippingLine4 = page.locator("text=배송비").locator("..").locator("span").last();
await waitForLocatorText(
  shippingLine4,
  (t) => t.includes("무료"),
  "시나리오 4 판정 기준 미충족: 상품금액 70,000원인데 배송비가 무료로 표시되지 않음",
);

// 데스크톱: fixed 하단 CTA 없어야 하고, GlobalBottomNav도 없어야 함
const mobileFixedCta4 = page.locator("div.fixed.bottom-0.lg\\:hidden", { hasText: "결제하기" });
if (await mobileFixedCta4.isVisible({ timeout: 2000 }).catch(() => false)) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "responsive-design-handoff-scenario-4-mobile-cta-visible");
  throw new Error("시나리오 4 판정 기준 미충족: 데스크톱 폭에서 fixed 하단 CTA가 여전히 보임");
}
const bottomNav4 = page.locator("div.lg\\:hidden", { has: page.getByRole("button", { name: "마이" }) }).first();
if (await bottomNav4.isVisible({ timeout: 2000 }).catch(() => false)) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "responsive-design-handoff-scenario-4-bottomnav-visible");
  throw new Error("시나리오 4 판정 기준 미충족: 데스크톱 폭 체크아웃에서 하단 탭바가 보임");
}

// 요약 컬럼 안의 결제하기 버튼(sticky, lg:block)으로 제출
const summaryColumn = page.locator("section.lg\\:sticky.lg\\:top-8.lg\\:col-start-2");
await summaryColumn.getByRole("button", { name: "결제하기" }).click();
await page.waitForURL("**/checkout/payment**", { timeout: 10000 });

const payButton4 = page.getByRole("button", { name: /결제하기|불러오는 중/ });
await page.waitForTimeout(3000);
const payButtonText4 = await payButton4.textContent().catch(() => null);
if (!payButtonText4 || !payButtonText4.includes("70,000")) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "responsive-design-handoff-scenario-4-widget-mismatch");
  throw new Error(
    `시나리오 4 판정 기준 미충족: 결제위젯 금액이 70,000원과 일치하지 않음 (버튼 텍스트: ${payButtonText4}, 스크린샷: ${screenshotPath})`,
  );
}

console.log("PASS: 시나리오 4");

// ============================================================
// 시나리오 5: 쿠폰 등록 -> 체크아웃 선택 -> 할인 반영 (모바일 390px)
// ============================================================
await page.setViewportSize({ width: 390, height: 844 });
await signup("반응형쿠폰모바일테스터");

await page.goto("http://localhost:3000/mypage/coupons", { waitUntil: "domcontentloaded" });
const codeInput5 = page.getByLabel("쿠폰 코드");
await codeInput5.waitFor({ state: "visible", timeout: 8000 });
await codeInput5.fill("WELCOME3000");
await page.getByRole("button", { name: "등록하기" }).click();

const welcomeCard5 = page.getByText("웰컴 3,000원 할인");
await welcomeCard5.waitFor({ state: "visible", timeout: 5000 }).catch(async () => {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "responsive-design-handoff-scenario-5-not-registered");
  throw new Error(`시나리오 5 판정 기준 미충족: WELCOME3000 등록 후 카드가 보이지 않음 (스크린샷: ${screenshotPath})`);
});

const successToast5 = page.getByText("쿠폰을 등록했어요");
if (!(await successToast5.isVisible({ timeout: 3000 }).catch(() => false))) {
  throw new Error("시나리오 5 판정 기준 미충족: 쿠폰 등록 성공 토스트가 보이지 않음");
}

await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [{ key: "3-M", id: 3, title: "강아지 하네스 M", size: "M", unitPrice: 32000, qty: 1 }];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["3-M"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

const recipientInput5 = page.getByLabel("받는 사람");
if (await recipientInput5.isVisible({ timeout: 3000 }).catch(() => false)) {
  await recipientInput5.fill("테스트 수령인");
  await page.getByLabel("연락처").fill("01012345678");
  await page.getByLabel("우편번호").fill("12345");
  await page.getByLabel("주소", { exact: true }).fill("서울시 테스트구 테스트로 1");
}

await waitForLocatorText(
  page.locator("text=상품금액").locator("..").locator("span").last(),
  (t) => t.includes("32,000"),
  "시나리오 5 사전 확인 실패: 체크아웃 상품금액이 32,000원으로 반영되지 않음",
);

const totalBefore5 = await page.locator("text=총 결제금액").locator("..").locator("span").last().textContent();
const welcomeButton5 = page.locator("button", { hasText: "웰컴 3,000원 할인" });
await welcomeButton5.waitFor({ state: "visible", timeout: 5000 });
await welcomeButton5.click();

const discountLine5 = page.locator("text=쿠폰 할인").locator("..").locator("span").last();
await waitForLocatorText(
  discountLine5,
  (t) => t.includes("3,000"),
  "시나리오 5 판정 기준 미충족: 쿠폰 선택 후 쿠폰 할인 -3,000원 줄이 나타나지 않음",
);

const totalAfter5 = await page.locator("text=총 결제금액").locator("..").locator("span").last().textContent();
const beforeNum5 = Number((totalBefore5 || "").replace(/[^0-9]/g, ""));
const afterNum5 = Number((totalAfter5 || "").replace(/[^0-9]/g, ""));
if (beforeNum5 - afterNum5 !== 3000) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "responsive-design-handoff-scenario-5-amount-mismatch");
  throw new Error(
    `시나리오 5 판정 기준 미충족: 쿠폰 선택 전후 차액이 3,000원이 아님 (before=${beforeNum5}, after=${afterNum5}, 스크린샷: ${screenshotPath})`,
  );
}

console.log("PASS: 시나리오 5");

// ============================================================
// 시나리오 6: 쿠폰 선택 -> 할인 반영 (데스크톱 1440px)
// ============================================================
await page.setViewportSize({ width: 1440, height: 900 });
await signup("반응형쿠폰데스크톱테스터");

await page.goto("http://localhost:3000/mypage/coupons", { waitUntil: "domcontentloaded" });
const codeInput6 = page.getByLabel("쿠폰 코드");
await codeInput6.waitFor({ state: "visible", timeout: 8000 });
await codeInput6.fill("FIRSTORDER5000");
await page.getByRole("button", { name: "등록하기" }).click();

const firstOrderCard6 = page.getByText("첫 구매 5,000원 할인");
await firstOrderCard6.waitFor({ state: "visible", timeout: 5000 }).catch(async () => {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "responsive-design-handoff-scenario-6-not-registered");
  throw new Error(`시나리오 6 판정 기준 미충족: FIRSTORDER5000 등록 후 카드가 보이지 않음 (스크린샷: ${screenshotPath})`);
});

await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [{ key: "1-M", id: 1, title: "강아지 무릎담요", size: "M", unitPrice: 18000, qty: 1 }];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["1-M"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

const recipientInput6 = page.getByLabel("받는 사람");
if (await recipientInput6.isVisible({ timeout: 3000 }).catch(() => false)) {
  await recipientInput6.fill("테스트 수령인");
  await page.getByLabel("연락처").fill("01012345678");
  await page.getByLabel("우편번호").fill("12345");
  await page.getByLabel("주소", { exact: true }).fill("서울시 테스트구 테스트로 1");
}

await waitForLocatorText(
  page.locator("text=상품금액").locator("..").locator("span").last(),
  (t) => t.includes("18,000"),
  "시나리오 6 사전 확인 실패: 체크아웃 상품금액이 18,000원으로 반영되지 않음",
);

const firstOrderButton6 = page.locator("button", { hasText: "첫 구매 5,000원 할인" });
await firstOrderButton6.waitFor({ state: "visible", timeout: 5000 });

// 좌측 본문 영역(쿠폰 선택 UI)에 있는지 확인 - 2단 그리드 왼쪽 컬럼
const leftColumnVisible = await page
  .locator(".lg\\:grid-cols-\\[1fr_340px\\]")
  .isVisible({ timeout: 3000 })
  .catch(() => false);
if (!leftColumnVisible) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "responsive-design-handoff-scenario-6-no-two-column");
  throw new Error("시나리오 6 판정 기준 미충족: 데스크톱 체크아웃에서 2단 그리드 레이아웃이 보이지 않음");
}

await firstOrderButton6.click();

const discountLine6 = page.locator("text=쿠폰 할인").locator("..").locator("span").last();
await waitForLocatorText(
  discountLine6,
  (t) => t.includes("5,000"),
  "시나리오 6 판정 기준 미충족: 쿠폰 선택 후 우측 요약 컬럼에 쿠폰 할인 -5,000원이 반영되지 않음",
);

console.log("PASS: 시나리오 6");

// ============================================================
// 시나리오 7: 구매 이력 없는 상품상세에서 리뷰 쓰기 버튼 미노출 (모바일 -> 데스크톱 전환)
// ============================================================
await page.setViewportSize({ width: 390, height: 844 });
await page.goto("http://localhost:3000/products/1", { waitUntil: "domcontentloaded" });
await page.waitForTimeout(1500);

const emptyReviewText7 = page.getByText("아직 작성된 리뷰가 없어요.");
if (!(await emptyReviewText7.isVisible({ timeout: 5000 }).catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "responsive-design-handoff-scenario-7-mobile-no-empty-text");
  throw new Error("시나리오 7 판정 기준 미충족: 모바일 폭에서 리뷰 빈 상태 문구가 보이지 않음");
}

const writeReviewButton7Mobile = page.getByRole("button", { name: "리뷰 쓰기" });
if (await writeReviewButton7Mobile.isVisible({ timeout: 2000 }).catch(() => false)) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "responsive-design-handoff-scenario-7-mobile-write-button-visible");
  throw new Error("시나리오 7 판정 기준 미충족: 구매 이력 없는데 모바일 폭에서 리뷰 쓰기 버튼이 보임");
}

await page.setViewportSize({ width: 1440, height: 900 });
await page.waitForTimeout(500);

if (!(await emptyReviewText7.isVisible({ timeout: 5000 }).catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "responsive-design-handoff-scenario-7-desktop-no-empty-text");
  throw new Error("시나리오 7 판정 기준 미충족: 데스크톱 폭 전환 후 리뷰 빈 상태 문구가 보이지 않음");
}

const mobileFixedCta7 = page.locator("div.fixed.lg\\:hidden");
if (await mobileFixedCta7.first().isVisible({ timeout: 2000 }).catch(() => false)) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "responsive-design-handoff-scenario-7-desktop-fixed-cta-visible");
  throw new Error("시나리오 7 판정 기준 미충족: 데스크톱 폭 상품상세에서 fixed 하단 CTA가 보임");
}

console.log("PASS: 시나리오 7");
console.log("SKIP: 시나리오 7-리뷰작성/수정 — PAID 주문(구매 이력)을 이 환경에서 만들 수 없어(Toss 샌드박스 실카드 결제 필요) 사전조건 미충족");
```
