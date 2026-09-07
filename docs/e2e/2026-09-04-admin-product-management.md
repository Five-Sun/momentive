---
date: 2026-09-04
feature: admin-product-management
spec: 2026-09-04-admin-product-management.md
plan: 2026-09-04-admin-product-management.md
---

# 관리자 기반 및 상품 관리 E2E 케이스

관리자 등록 → 고객 검색 → 사이즈별 품절 확인 → 장바구니 → 주문 생성 → 판매 중단까지를 한 유저 플로우(한 탭)로 이어 검증한다. 이미지 업로드는 Cloudinary 자격증명이 없는 환경이므로 **이미지 0장**으로 등록한다(스펙 시나리오 B의 "이미지가 0장이어도 저장은 가능하다" 예외 경로가 곧 검증 대상이 된다).

**실행 전 로컬에 E2E 전용 관리자 계정을 준비해야 한다.** 스크립트 상단의 `ADMIN_EMAIL`(`admin@momentive.local`)로 회원가입한 뒤 `UPDATE users SET role='ADMIN' WHERE email='admin@momentive.local';`로 승격한다. **실제 운영 관리자 이메일은 이 파일에 넣지 않는다** — 리포지토리가 public이며, 그것이 승격을 마이그레이션으로 자동화하지 않은 이유이기도 하다. 이 값이 실제 로컬 DB의 관리자 계정과 다르면 시나리오 1이 "사전조건 미충족"으로 즉시 중단되며, 이는 코드 결함이 아니다.

**뷰포트**: 스크립트 시작 시 1440x900(데스크톱)으로 고정한다. 상품상세·장바구니·체크아웃의 CTA는 데스크톱 폭에서 하단 고정 바(`lg:hidden`)가 숨겨지고 우측/본문 CTA만 남으므로, 같은 텍스트의 버튼이 DOM에 둘 존재한다. 그래서 CTA는 전부 `button:visible`로 좁혀 잡는다.

**셀렉터 노트 (CSS 셀렉터 fallback 사용, 컴포넌트 구조 변경 시 갱신 필요)**
- 상품 카드는 이름 텍스트가 랭킹 캐러셀·메인 그리드에 중복 렌더링될 수 있으므로 `getByText().first()`를 쓰지 않고, `ProductGridItem`이 감싸는 링크의 `a[href="/products/{id}"]`로 **상품 id를 기준으로** 특정한다.
- 관리자 폼의 variant 행은 사이즈/재고 label이 행마다 반복되므로 `getByLabel`을 쓸 수 없다. `TextField`가 `id = name`으로 id를 붙이는 구조(`frontend/src/components/forms/TextField.tsx`)를 근거로 `[id="variants.0.size"]` 형태의 속성 셀렉터를 쓴다.
- 로그인 폼의 비밀번호도 `getByLabel("비밀번호")`가 입력창과 "비밀번호 표시" 토글 버튼을 함께 잡아 strict mode 위반이 된다. `#password`로 특정한다.

## 시나리오 1: 관리자 로그인 후 `/admin` 진입

스펙 사용자 시나리오 A-4("다시 로그인하면 `/admin` 접근이 가능해진다")와 수용 기준 "인가" 항목을 검증한다. `AdminGuard`(`frontend/src/app/admin/AdminGuard.tsx`)가 `user.role === "ADMIN"`일 때만 하위 화면을 렌더하므로, 관리자 목록 화면이 실제로 그려지는지로 판정한다.

**사전조건**: `UPDATE users SET role = 'ADMIN' WHERE email = '...'`으로 승격한 관리자 계정이 로컬 DB에 존재하고, 그 이메일/비밀번호가 스크립트 상단 상수와 일치해야 한다. 승격 이후 재로그인해야 access token에 `role` 클레임이 실린다.

**판정 기준**: `/login`에서 관리자 계정으로 로그인해 `/mypage`로 이동한 뒤 `/admin`에 진입하면 "상품 관리" 제목과 "상품 등록" 버튼이 보이고, `AdminGuard`의 차단 문구 "관리자만 접근할 수 있는 화면이에요"가 보이지 않는다.

## 시나리오 2: 사이즈 있는 상품 등록 (이미지 0장)

스펙 사용자 시나리오 B-1~B-5와 수용 기준 "관리자 API"의 "상품을 등록하면 variant와 이미지가 함께 저장되고 고객 목록에 즉시 노출된다"를 검증한다. 사이즈 `S`(재고 5)와 `M`(재고 0) 두 행을 넣어 다음 시나리오에서 "사이즈별 품절"을 확인할 수 있는 데이터를 만든다. 이미지는 0장으로 두어 B-5 예외("이미지가 0장이어도 저장은 가능하다") 경로를 함께 태운다.

**사전조건**: 앞 시나리오 상태로 충족(관리자 로그인 + `/admin`).

**판정 기준**: `/admin/products/new`에서 이미지 없이 저장 가능하다는 안내("이미지 없이 저장해도 괜찮아요"로 시작하는 문구)가 보이고, 저장 후 `/admin/products/{id}`로 이동한다. `/admin` 목록으로 돌아오면 해당 상품 행의 수정 링크(`/admin/products/{id}`)가 존재하고, 상태 뱃지가 "판매중"이며 재고 합이 `5`로 표시된다.

## 시나리오 3: 고객 화면에서 검색으로 새 상품 찾기

스펙 사용자 시나리오 F-1과 수용 기준 "검색"의 "`GET /products?q=...`가 `name` 부분일치로 검색" / "`/search` 화면이 서버 검색으로 동작"을 검증한다. 방금 등록한 상품이 고객 검색에 즉시 잡히는지가 핵심이다(운영 병목 해소 지점).

**사전조건**: 앞 시나리오 상태로 충족(상품이 `ON_SALE`로 등록됨).

**판정 기준**: `/search`에서 등록한 상품명을 입력하고 Enter를 치면 결과 영역에 `a[href="/products/{id}"]` 링크가 나타난다. "검색 결과가 없어요"와 "검색에 실패했어요"는 모두 보이지 않는다.

## 시나리오 4: 상품상세 — 품절 사이즈 선택 불가 확인 후 재고 있는 사이즈로 장바구니 담기

스펙 사용자 시나리오 E-1/E-2와 수용 기준 "고객 화면"의 "상품상세가 해당 상품에 등록된 사이즈만 표시한다(S/M/L/XL 하드코딩 제거)", "재고 0인 사이즈가 선택 불가로 표시된다", "장바구니 항목이 `variantId`를 갖는다"를 검증한다.

**사전조건**: 앞 시나리오 상태로 충족. 단 장바구니는 시나리오 1에서 비운 상태에서 시작한다(이전 세션 잔여 항목이 뒤 시나리오의 주문 금액·구성을 흔들지 않게 하기 위함).

**판정 기준**: 상세 화면에 사이즈 버튼이 정확히 `S`, `M` 두 개만 있고(`L`/`XL` 버튼 없음 = 하드코딩 제거 확인), 재고 0인 `M` 버튼은 `disabled`다. `S`를 선택하면 "장바구니 담기" 버튼이 활성화되고, 클릭 시 "장바구니에 담았어요" 토스트가 뜬다. `/cart`에 해당 상품명과 "사이즈 S"가 보이고, localStorage `momentive:cart` 항목의 키가 `variant-` 접두사(`cartKeyOf`)이며 숫자 `variantId`를 갖는다.

## 시나리오 5: 주문 생성

스펙 수용 기준 "고객 화면"의 "주문 생성이 `variantId`로 이루어진다"와 "재고 모델"의 재고 차감이 variant 기준으로 동작함을 화면 흐름으로 검증한다. Toss confirm 성공 경로는 상점 미등록 제약(`docs/backlog/2026-08-30-cart-order-payment-phase4-01.md`)으로 이번에도 다루지 않고, `POST /orders` 성공(= `PENDING` 주문 생성)까지만 확인한다.

**사전조건**: 앞 시나리오 상태로 충족(장바구니에 사이즈 S 1개). 배송지가 없는 계정이면 체크아웃에서 새 배송지 입력 폼이 열리므로 스크립트가 값을 채운다.

**판정 기준**: `/cart`에서 "구매하기" → `/checkout`에서 "결제하기"를 누르면 `/checkout/payment?orderId=...`로 이동한다(= 주문 생성 성공). `/mypage/orders/{orderId}` 주문상세에 등록한 상품명과 "사이즈 S"가 표시된다. 이후 관리자 수정 폼에서 `S` 행 재고가 4로 줄어 있다(variant 기준 차감).

## 시나리오 6: 관리자가 `HIDDEN`으로 전환 → 고객 화면에서 사라짐

스펙 사용자 시나리오 D-1/D-3과 수용 기준 "관리자 API"의 "`HIDDEN` 또는 `DELETED` 상품이 고객 목록·검색·상세에서 제외되고, 상세는 404를 반환한다", "`HIDDEN`/`DELETED` 상품이 포함된 기존 주문 상세가 정상적으로 표시된다"를 검증한다.

**사전조건**: 앞 시나리오 상태로 충족(등록한 상품이 `ON_SALE`이고, 그 상품으로 만든 주문 1건이 존재).

**판정 기준**: `/admin/products/{id}`에서 판매 상태를 "숨김"(`HIDDEN`)으로 바꿔 저장하면 "저장했어요" 토스트가 뜨고, `/admin` 목록에서 상태 뱃지가 "숨김"으로 바뀐다. 고객 `/search`에서 같은 검색어로 검색하면 해당 상품 링크가 사라지고 "검색 결과가 없어요"가 표시된다. `/products/{id}`는 상품 상세가 렌더되지 않는다(404). 반면 `/mypage/orders/{orderId}` 주문상세에는 상품명이 그대로 남아 있다.

## 실행 스크립트

```javascript
// ============================================================
// 로컬 E2E 전용 관리자 계정. 실제 운영 관리자 이메일을 여기 넣지 않는다
// (리포지토리가 public이며, 그것이 승격을 마이그레이션으로 자동화하지 않은 이유이기도 하다).
//
// 실행 전 로컬에서 아래 두 가지를 준비한다.
//   1) 이 이메일/비밀번호로 회원가입
//   2) docker exec -it backend-db-1 psql -U momentive -d momentive \
//        -c "UPDATE users SET role='ADMIN' WHERE email='admin@momentive.local';"
// 준비가 안 되어 있으면 시나리오 1에서 "사전조건 미충족"으로 중단된다(코드 결함 아님).
// ============================================================
const ADMIN_EMAIL = "admin@momentive.local";
const ADMIN_PASSWORD = "momentive1234";

const page = await browser.getPage("admin-product-management");
await page.setViewportSize({ width: 1440, height: 900 });

const PRODUCT_NAME = `E2E사이즈상품${Date.now()}`;

async function waitFor(predicate, description, timeoutMs = 10000) {
  const deadline = Date.now() + timeoutMs;
  let last = null;
  while (Date.now() < deadline) {
    last = await predicate().catch((e) => {
      return null;
    });
    if (last) return last;
    await page.waitForTimeout(200);
  }
  throw new Error(description);
}

async function fail(scenario, message, shot) {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, `admin-product-management-scenario-${scenario}-${shot}`);
  throw new Error(`시나리오 ${scenario} 판정 기준 미충족: ${message} (스크린샷: ${path})`);
}

// ============================================================
// 시나리오 1: 관리자 로그인 후 /admin 진입
// ============================================================
await page.goto("http://localhost:3000/login", { waitUntil: "load" });

// 이전 세션이 남긴 장바구니를 비워 뒤 시나리오의 주문 구성이 흔들리지 않게 한다.
await page.evaluate(() => {
  window.localStorage.removeItem("momentive:cart");
  window.sessionStorage.removeItem("momentive:checkout-selection");
});

// 하이드레이션이 끝나기 전에 클릭하면 React의 onSubmit이 아직 안 붙어 폼이 네이티브 GET으로
// 전송되고(비밀번호가 쿼리스트링에 실린 채 /login에 머문다) 로그인이 조용히 실패한다.
// dev 서버는 하이드레이션이 느리므로 명시적으로 기다린다.
await page.waitForTimeout(6000);

// `getByLabel("비밀번호")`는 입력창과 "비밀번호 표시" 토글 버튼을 함께 잡아 strict mode에
// 걸린다. `TextField`/`PasswordField`가 `id = name`으로 id를 붙이므로 id 셀렉터를 쓴다.
await page.locator("#email").fill(ADMIN_EMAIL);
await page.locator("#password").fill(ADMIN_PASSWORD);
await page.getByRole("button", { name: "로그인" }).click();

await page.waitForURL("**/mypage", { timeout: 10000 }).catch(async () => {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, "admin-product-management-scenario-1-login-failed");
  throw new Error(
    `시나리오 1 사전조건 미충족: 관리자 계정(${ADMIN_EMAIL}) 로그인에 실패했다. ` +
      `ADMIN으로 승격한 계정과 비밀번호가 스크립트 상수와 일치하는지 확인이 필요하다 (스크린샷: ${path})`,
  );
});

await page.goto("http://localhost:3000/admin", { waitUntil: "domcontentloaded" });

const guardBlocked = await page
  .getByText("관리자만 접근할 수 있는 화면이에요")
  .isVisible()
  .catch(() => false);
if (guardBlocked) {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, "admin-product-management-scenario-1-guard-blocked");
  throw new Error(
    `시나리오 1 사전조건 미충족: 로그인은 됐으나 AdminGuard가 차단했다(role !== ADMIN). ` +
      `role 수동 승격 여부와 재로그인(access token의 role 클레임) 여부 확인이 필요하다 (스크린샷: ${path})`,
  );
}

await page
  .getByRole("heading", { name: "상품 관리" })
  .waitFor({ state: "visible", timeout: 10000 })
  .catch(async () => {
    await fail(1, '/admin에 "상품 관리" 제목이 보이지 않음', "no-heading");
  });

const newProductButton = page.getByRole("button", { name: "상품 등록" });
if (!(await newProductButton.isVisible().catch(() => false))) {
  await fail(1, '"상품 등록" 버튼이 보이지 않음', "no-new-button");
}

console.log("PASS: 시나리오 1");

// ============================================================
// 시나리오 2: 사이즈 있는 상품 등록 (이미지 0장)
// ============================================================
await newProductButton.click();
await page.waitForURL("**/admin/products/new", { timeout: 10000 });

const noImageNotice = page.getByText("이미지 없이 저장해도 괜찮아요", { exact: false });
if (!(await noImageNotice.isVisible().catch(() => false))) {
  await fail(2, "이미지 0장 상태의 안내 문구가 보이지 않음", "no-image-notice");
}

await page.getByLabel("상품명").fill(PRODUCT_NAME);
await page.getByLabel("상품 설명").fill("E2E 검증용 상품입니다. 사이즈별 재고 확인에 사용합니다.");
await page.getByLabel("정가", { exact: true }).fill("29000");
await page.locator("#category").selectOption("OUTER");
await page.locator("#status").selectOption("ON_SALE");

// variant 1행: S / 재고 5
await page.locator('[id="variants.0.size"]').fill("S");
await page.locator('[id="variants.0.stock"]').fill("5");

// variant 2행 추가: M / 재고 0 (품절 사이즈)
await page.getByRole("button", { name: "사이즈 추가" }).click();
await page.locator('[id="variants.1.size"]').waitFor({ state: "visible", timeout: 5000 });
await page.locator('[id="variants.1.size"]').fill("M");
await page.locator('[id="variants.1.stock"]').fill("0");

await page.getByRole("button", { name: "등록하기" }).click();

const productId = await waitFor(
  async () => {
    const m = page.url().match(/\/admin\/products\/(\d+)/);
    return m ? m[1] : null;
  },
  "시나리오 2 판정 기준 미충족: 등록 후 /admin/products/{id}로 이동하지 않음 (서버가 저장을 거부했을 가능성 — 폼 인라인 에러 확인 필요)",
  15000,
);

await page.goto("http://localhost:3000/admin", { waitUntil: "domcontentloaded" });

const editLink = page.locator(`a[href="/admin/products/${productId}"]`);
await editLink.waitFor({ state: "visible", timeout: 10000 }).catch(async () => {
  await fail(2, `/admin 목록에 등록한 상품(id=${productId}) 행이 보이지 않음`, "not-in-list");
});

const row = editLink.locator("xpath=ancestor::tr");
const rowText = (await row.textContent().catch(() => "")) || "";
if (!rowText.includes(PRODUCT_NAME)) {
  await fail(2, `목록 행에 상품명이 없음 (행 텍스트: ${rowText})`, "row-name-mismatch");
}
if (!rowText.includes("판매중")) {
  await fail(2, `목록 행 상태가 "판매중"이 아님 (행 텍스트: ${rowText})`, "row-status-mismatch");
}
if (!rowText.includes("5")) {
  await fail(2, `목록 행 재고 합이 5로 표시되지 않음 (행 텍스트: ${rowText})`, "row-stock-mismatch");
}

console.log("PASS: 시나리오 2 (productId=" + productId + ")");

// ============================================================
// 시나리오 3: 고객 화면에서 검색으로 새 상품 찾기
// ============================================================
await page.goto("http://localhost:3000/search", { waitUntil: "load" });

// 데스크톱 폭에서는 `TopNav`의 검색창과 `/search` 페이지의 입력창이 같은 placeholder를 쓴다.
// `main` 안으로 스코프해 페이지 자체의 입력창만 잡는다.
const searchBox = page.getByRole("main").getByPlaceholder("브랜드, 상품 검색");
await searchBox.waitFor({ state: "visible", timeout: 10000 });
// 하이드레이션 전에는 onChange/onKeyDown이 붙지 않아 입력이 React 상태에 반영되지 않고
// Enter가 무시된다(검색이 조용히 실행되지 않음). 로그인과 동일한 이유로 기다린다.
await page.waitForTimeout(6000);
await searchBox.fill(PRODUCT_NAME);
await searchBox.press("Enter");

const searchFailed = await page.getByText("검색에 실패했어요").isVisible().catch(() => false);
if (searchFailed) {
  await fail(3, "검색 API 호출이 실패해 실패 안내가 표시됨", "search-api-failed");
}

const resultLink = page.locator(`a[href="/products/${productId}"]`);
await resultLink.waitFor({ state: "visible", timeout: 10000 }).catch(async () => {
  await fail(3, `검색 결과에 등록한 상품 링크(/products/${productId})가 없음`, "not-found-in-search");
});

const emptyResult = await page.getByText("검색 결과가 없어요").isVisible().catch(() => false);
if (emptyResult) {
  await fail(3, '결과가 있는데 "검색 결과가 없어요"가 함께 표시됨(상태 조건 비배타)', "state-overlap");
}

console.log("PASS: 시나리오 3");

// ============================================================
// 시나리오 4: 품절 사이즈 선택 불가 → 재고 있는 사이즈로 장바구니 담기
// ============================================================
await resultLink.click();
await page.waitForURL(`**/products/${productId}`, { timeout: 10000 });

const sizeS = page.getByRole("button", { name: "S", exact: true });
const sizeM = page.getByRole("button", { name: "M", exact: true });
await sizeS.waitFor({ state: "visible", timeout: 10000 }).catch(async () => {
  await fail(4, "사이즈 S 버튼이 상세 화면에 보이지 않음", "no-size-s");
});

if (!(await sizeM.isVisible().catch(() => false))) {
  await fail(4, "사이즈 M 버튼이 상세 화면에 보이지 않음", "no-size-m");
}

// 하드코딩 제거 확인 — 등록하지 않은 L/XL은 나타나면 안 된다.
const sizeL = await page.getByRole("button", { name: "L", exact: true }).count();
const sizeXL = await page.getByRole("button", { name: "XL", exact: true }).count();
if (sizeL > 0 || sizeXL > 0) {
  await fail(4, `등록하지 않은 사이즈 버튼이 노출됨 (L=${sizeL}, XL=${sizeXL}) — SIZES 하드코딩 잔존 의심`, "hardcoded-sizes");
}

if (!(await sizeM.isDisabled())) {
  await fail(4, "재고 0인 사이즈 M이 선택 가능 상태임", "m-not-disabled");
}
if (await sizeS.isDisabled()) {
  await fail(4, "재고 5인 사이즈 S가 선택 불가 상태임", "s-disabled");
}

await sizeS.click();

const addToCartButton = page.locator("button:visible", { hasText: "장바구니 담기" });
await addToCartButton.waitFor({ state: "visible", timeout: 5000 });
if (await addToCartButton.isDisabled()) {
  await fail(4, "재고 있는 사이즈를 선택했는데 장바구니 담기 버튼이 비활성 상태임", "cta-disabled");
}
await addToCartButton.click();

await page
  .getByText("장바구니에 담았어요")
  .waitFor({ state: "visible", timeout: 5000 })
  .catch(async () => {
    await fail(4, "장바구니 담기 후 토스트가 노출되지 않음", "no-toast");
  });

const cartRaw = await page.evaluate(() => window.localStorage.getItem("momentive:cart"));
const cartItems = JSON.parse(cartRaw || "[]");
if (cartItems.length !== 1) {
  await fail(4, `장바구니 항목이 1건이어야 하는데 ${cartItems.length}건임 (raw: ${cartRaw})`, "cart-count");
}
if (typeof cartItems[0].variantId !== "number" || !String(cartItems[0].key).startsWith("variant-")) {
  await fail(4, `장바구니 항목이 variantId 기반 키를 갖지 않음 (raw: ${cartRaw})`, "cart-no-variant");
}

await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page
  .getByText(PRODUCT_NAME)
  .waitFor({ state: "visible", timeout: 10000 })
  .catch(async () => {
    await fail(4, "/cart에 담은 상품명이 보이지 않음", "cart-no-name");
  });
if (!(await page.getByText("사이즈 S").isVisible().catch(() => false))) {
  await fail(4, '/cart에 "사이즈 S"가 표시되지 않음', "cart-no-size");
}

console.log("PASS: 시나리오 4");

// ============================================================
// 시나리오 5: 주문 생성
// ============================================================
const checkoutButton = page.locator("button:visible", { hasText: "구매하기" });
await checkoutButton.waitFor({ state: "visible", timeout: 5000 });
await checkoutButton.click();
await page.waitForURL("**/checkout", { timeout: 10000 });

// 배송지 폼은 `getAddresses()` 응답이 온 뒤에야 렌더링된다(저장된 배송지가 없으면
// `showNewAddressForm`이 true가 된다). 대기 없이 isVisible()을 보면 false로 읽혀
// 폼을 비운 채 제출하게 되고, 검증 에러만 뜬 채 주문이 생성되지 않는다.
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
  "시나리오 5 판정 기준 미충족: 결제하기 후 /checkout/payment?orderId=...로 이동하지 않음 (POST /orders 실패 의심)",
  15000,
);

await page.goto(`http://localhost:3000/mypage/orders/${orderId}`, { waitUntil: "domcontentloaded" });
await page
  .getByText(PRODUCT_NAME)
  .waitFor({ state: "visible", timeout: 10000 })
  .catch(async () => {
    await fail(5, `주문상세(/mypage/orders/${orderId})에 상품명이 보이지 않음`, "order-no-name");
  });
if (!(await page.getByText("사이즈 S").isVisible().catch(() => false))) {
  await fail(5, '주문상세에 "사이즈 S"가 표시되지 않음', "order-no-size");
}

// variant 기준 재고 차감 확인 — S 행 재고가 5 → 4
await page.goto(`http://localhost:3000/admin/products/${productId}`, { waitUntil: "domcontentloaded" });
await page.locator('[id="variants.0.stock"]').waitFor({ state: "visible", timeout: 10000 });
const stockAfterOrder = await page.locator('[id="variants.0.stock"]').inputValue();
if (stockAfterOrder !== "4") {
  await fail(5, `주문 후 S 사이즈 재고가 4여야 하는데 ${stockAfterOrder}임 (variant 기준 차감 실패 의심)`, "stock-not-deducted");
}
const stockM = await page.locator('[id="variants.1.stock"]').inputValue();
if (stockM !== "0") {
  await fail(5, `주문하지 않은 M 사이즈 재고가 0에서 ${stockM}으로 변경됨`, "wrong-variant-deducted");
}

console.log("PASS: 시나리오 5 (orderId=" + orderId + ")");

// ============================================================
// 시나리오 6: HIDDEN 전환 → 고객 화면에서 사라짐
// ============================================================
await page.locator("#status").selectOption("HIDDEN");
await page.getByRole("button", { name: "저장하기" }).click();

await page
  .getByText("저장했어요")
  .waitFor({ state: "visible", timeout: 10000 })
  .catch(async () => {
    await fail(6, "HIDDEN 전환 저장 후 저장 완료 토스트가 보이지 않음", "no-save-toast");
  });

await page.goto("http://localhost:3000/admin", { waitUntil: "domcontentloaded" });
const hiddenRow = page.locator(`a[href="/admin/products/${productId}"]`).locator("xpath=ancestor::tr");
await hiddenRow.waitFor({ state: "visible", timeout: 10000 });
const hiddenRowText = (await hiddenRow.textContent().catch(() => "")) || "";
if (!hiddenRowText.includes("숨김")) {
  await fail(6, `관리자 목록 상태가 "숨김"으로 바뀌지 않음 (행 텍스트: ${hiddenRowText})`, "status-not-hidden");
}

// 고객 검색에서 사라졌는지
await page.goto("http://localhost:3000/search", { waitUntil: "load" });
const searchBox6 = page.getByRole("main").getByPlaceholder("브랜드, 상품 검색");
await searchBox6.waitFor({ state: "visible", timeout: 10000 });
await page.waitForTimeout(6000);
await searchBox6.fill(PRODUCT_NAME);
await searchBox6.press("Enter");

await page
  .getByText("검색 결과가 없어요")
  .waitFor({ state: "visible", timeout: 10000 })
  .catch(async () => {
    await fail(6, 'HIDDEN 전환 후에도 검색 결과가 비지 않음("검색 결과가 없어요" 미표시)', "still-searchable");
  });

const hiddenLinkCount = await page.locator(`a[href="/products/${productId}"]`).count();
if (hiddenLinkCount > 0) {
  await fail(6, `HIDDEN 상품 링크가 검색 결과에 ${hiddenLinkCount}건 남아 있음`, "link-still-present");
}

// 고객 상세는 404
await page.goto(`http://localhost:3000/products/${productId}`, { waitUntil: "domcontentloaded" });
const detailHeadingCount = await page.locator("h1", { hasText: PRODUCT_NAME }).count();
if (detailHeadingCount > 0) {
  await fail(6, "HIDDEN 상품의 고객 상세 화면이 여전히 렌더됨(404가 아님)", "detail-still-rendered");
}

// 기존 주문 이력에는 그대로 보여야 한다
await page.goto(`http://localhost:3000/mypage/orders/${orderId}`, { waitUntil: "domcontentloaded" });
await page
  .getByText(PRODUCT_NAME)
  .waitFor({ state: "visible", timeout: 10000 })
  .catch(async () => {
    await fail(6, "HIDDEN 전환 후 기존 주문상세에서 상품명이 사라짐", "order-history-broken");
  });

console.log("PASS: 시나리오 6");
```
