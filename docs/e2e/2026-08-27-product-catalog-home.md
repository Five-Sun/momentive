---
date: 2026-08-27
feature: product-catalog-home
spec: 2026-08-18-product-catalog.md
plan: 2026-08-23-product-catalog-home-fix-1.md
---

# 상품 목록/상세 조회 (홈 화면) E2E 케이스

`docs/specs/2026-08-18-product-catalog.md`(API/데이터 계약)와 `docs/specs/2026-08-23-home-screen.md`(레이아웃/무한스크롤 디테일)를 근거로 도출한 시나리오다.

## 데이터 사전조건 메모

검증 시점에 로컬 dev DB(`docker compose`의 `backend-db-1`)를 확인한 결과 `product` 테이블이 0건이었고, `flyway_schema_history`에 현재 브랜치 체크아웃에는 없는 `V3__add_product_category.sql`(다른 브랜치/세션에서 실행된 것으로 추정)이 기록되어 있어 `product.category` 컬럼(NOT NULL)이 남아있었다. 이는 이번 기능 코드의 결함이 아니라 공유 로컬 DB의 브랜치 간 상태 오염이다.

- **시나리오 6(빈 상태)** 은 이 자연 상태(0건)를 그대로 이용해 먼저 실행했다.
- 이후 시나리오 1~5(상품 존재 전제)를 위해 `docker exec backend-db-1 psql -U momentive -d momentive`로 `V2__seed_product.sql`과 동일한 내용에 `category` 컬럼 값(`'ETC'`, 테스트용 더미)을 추가해 15개 상품 + 이미지, 그리고 무한스크롤(20개 초과) 검증을 위해 추가 상품 10개(생성일시를 더 과거로 설정)를 직접 INSERT했다.
- 모든 시나리오 실행 후 `product`, `product_image`를 다시 비워 검증 시작 시점 상태(0건)로 복원했다.
- 이 메모는 재현을 위한 기록이며, 실제 컬럼/스키마 오염 자체는 코드 변경 대상이 아니므로 backlog에는 기록하지 않았다. 다만 로컬 DB에 다른 브랜치의 잔여 마이그레이션이 섞여 있다는 점은 사용자에게 별도로 알린다.

## 시나리오 1: 홈 접속 시 상품 그리드가 실제 API 데이터로 렌더링된다

`product-catalog` AC "목록 화면의 각 카드가 대표 이미지/상품명/가격/품절 뱃지를 표시" 및 `home-screen` AC 근거.

**사전조건**: `product` 테이블에 상품이 20개 초과 존재해야 함(위 데이터 사전조건 메모의 시드 데이터 적용, 25건).

**판정 기준**: `/` 접속 시 첫 페이지 카드가 20개(`page/size` 기본값) 렌더링되고, 각 카드에 이미지·가격(`.text-price`)이 존재하며 품절 뱃지("품절" 텍스트)가 그리드 어딘가에 노출된다.

```javascript
const page = await browser.getPage("product-catalog-home-scenario-1");
await page.goto("http://localhost:3000/", { waitUntil: "domcontentloaded" });
await page.waitForSelector("main section .grid > a", { timeout: 5000 });
await page.waitForTimeout(300);

const cardCount = await page.locator("main section .grid > a").count();
if (cardCount !== 20) {
  const buf = await page.screenshot();
  await browser.saveScreenshot(buf, "scenario1-cardcount-fail");
  throw new Error(`첫 페이지 카드 개수가 20이 아님: ${cardCount}`);
}

const firstCard = page.locator("main section .grid > a").first();
const hasImage = await firstCard.locator("img").count();
if (hasImage < 1) throw new Error("첫 카드에 이미지가 없음");

const hasPrice = await firstCard.locator(".text-price").count();
if (hasPrice < 1) throw new Error("첫 카드에 가격 표시가 없음");

const soldoutBadge = await page.getByText("품절", { exact: true }).count();
if (soldoutBadge < 1) {
  throw new Error("품절 뱃지가 그리드 어디에도 없음");
}

console.log(`PASS: 시나리오 1 - 카드 ${cardCount}개 렌더링, 이미지/가격/품절뱃지 확인`);
```

## 시나리오 2: 품절 상품이 목록에서 숨겨지지 않고 "품절" 뱃지와 함께 노출된다

`product-catalog` 사용자 시나리오 4 / AC 근거.

**사전조건**: 시나리오 1과 동일한 시드 데이터(품절 상품 3건 포함, id 39/42/48).

**판정 기준**: 첫 페이지 카드 수(20)가 품절 상품이 없다고 가정했을 때보다 줄어들지 않고, "품절" 뱃지가 정확히 3개(시드된 품절 상품 수) 노출된다.

```javascript
const page = await browser.getPage("product-catalog-home-scenario-2");
await page.goto("http://localhost:3000/", { waitUntil: "domcontentloaded" });
await page.waitForSelector("main section .grid > a", { timeout: 5000 });
await page.waitForTimeout(300);

const cardCount = await page.locator("main section .grid > a").count();
const soldoutBadges = await page.getByText("품절", { exact: true }).count();

if (soldoutBadges !== 3) {
  const buf = await page.screenshot();
  await browser.saveScreenshot(buf, "scenario2-badge-count-fail");
  throw new Error(`품절 뱃지 개수가 기대값(3)과 다름: ${soldoutBadges}, 전체 카드 ${cardCount}`);
}
if (cardCount < 15) {
  throw new Error(`품절 상품이 숨겨진 것으로 의심됨: 카드 수 ${cardCount} (기대: 20)`);
}

console.log(`PASS: 시나리오 2 - 품절 상품 ${soldoutBadges}개가 전체 ${cardCount}개 카드 중 숨겨지지 않고 노출됨`);
```

## 시나리오 3: 상품 카드 클릭 → 상세 페이지 이동, 갤러리/이름/설명/가격(할인가 병기)/액션 버튼 없음

`product-catalog` 사용자 시나리오 5~6 / AC, `home-screen` 마지막 AC 근거.

**사전조건**: id=36 상품(이미지 2장, 할인 없음, "강아지 무릎담요")과 id=37 상품(이미지 1장, 정가 12,000원/할인가 9,600원, "연어 트릿 200g")이 존재해야 함. 위 시드 데이터로 충족.

**판정 기준**: 홈에서 id=36 카드를 클릭하면 `/products/36`으로 이동하고 상세 페이지에 상품명·설명·이미지 2장·가격이 표시되며, `main` 영역 안에 "장바구니"/"구매" 텍스트를 가진 버튼이 없다. 별도로 id=37 상세 페이지에 정가(취소선)와 할인가가 함께 표시된다.

```javascript
const page = await browser.getPage("product-catalog-home-scenario-3");
await page.goto("http://localhost:3000/", { waitUntil: "domcontentloaded" });
await page.waitForSelector("main section .grid > a", { timeout: 5000 });
await page.waitForTimeout(300);

const targetLink = page.locator('main section .grid > a[href="/products/36"]');
const linkCount = await targetLink.count();
if (linkCount !== 1) {
  throw new Error(`id=36 카드 링크를 찾지 못함 (개수: ${linkCount})`);
}

await Promise.all([
  page.waitForURL("**/products/36", { timeout: 5000 }),
  targetLink.click(),
]);
await page.waitForTimeout(300);

const nameVisible = await page.getByRole("heading", { name: "강아지 무릎담요" }).isVisible();
if (!nameVisible) throw new Error("상세 페이지에 상품명이 표시되지 않음");

const descVisible = await page.getByText("포근한 극세사 소재의 강아지 전용 무릎담요입니다.").isVisible();
if (!descVisible) throw new Error("상세 페이지에 설명이 표시되지 않음");

const imgCount = await page.locator('img[alt="강아지 무릎담요"]').count();
if (imgCount !== 2) {
  const buf = await page.screenshot();
  await browser.saveScreenshot(buf, "scenario3-gallery-fail");
  throw new Error(`이미지 갤러리 개수가 기대값(2)과 다름: ${imgCount}`);
}

const priceVisible = await page.getByText("18,000원").isVisible();
if (!priceVisible) throw new Error("가격이 표시되지 않음");

const actionButtons = await page.locator("main").getByRole("button", { name: /장바구니|구매/ }).count();
if (actionButtons !== 0) throw new Error("액션 버튼(장바구니/구매)이 상세 페이지 본문에 존재함 - 순수 조회 상세 페이지여야 함");

// 할인가 병기 확인 (product id 37, 정가 12,000원 / 할인가 9,600원)
await page.goto("http://localhost:3000/products/37", { waitUntil: "domcontentloaded" });
await page.waitForTimeout(300);
const originalPriceVisible = await page.getByText("12,000원").isVisible();
const discountPriceVisible = await page.getByText("9,600원").isVisible();
if (!originalPriceVisible || !discountPriceVisible) {
  const buf = await page.screenshot();
  await browser.saveScreenshot(buf, "scenario3-discount-fail");
  throw new Error(`할인가 병기 표시 실패: 정가 표시=${originalPriceVisible}, 할인가 표시=${discountPriceVisible}`);
}

console.log("PASS: 시나리오 3 - 카드 클릭 이동, 갤러리(2장)/이름/설명/가격/할인가 병기, 액션버튼 없음 확인");
```

## 시나리오 4: 그리드 하단 스크롤 시 다음 페이지가 자동으로 이어붙는다 (무한스크롤)

`home-screen` 사용자 시나리오 5 / AC 근거.

**사전조건**: 전체 상품이 20개 초과(25건, 2페이지)여야 함. 위 시드 데이터로 충족.

**판정 기준**: 초기 카드 20개 → 그리드 하단으로 스크롤 후 25개(전체)로 늘어난다.

```javascript
const page = await browser.getPage("product-catalog-home-scenario-4");
await page.goto("http://localhost:3000/", { waitUntil: "domcontentloaded" });
await page.waitForSelector("main section .grid > a", { timeout: 5000 });
await page.waitForTimeout(300);

const initialCount = await page.locator("main section .grid > a").count();
if (initialCount !== 20) {
  throw new Error(`초기 카드 개수가 20이 아님: ${initialCount}`);
}

await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
await page.waitForTimeout(1500);

const afterScrollCount = await page.locator("main section .grid > a").count();
if (afterScrollCount <= initialCount) {
  const buf = await page.screenshot();
  await browser.saveScreenshot(buf, "scenario4-noscroll-fail");
  throw new Error(`스크롤 후에도 카드 개수가 늘지 않음: ${afterScrollCount}`);
}
if (afterScrollCount !== 25) {
  throw new Error(`스크롤 후 카드 개수가 기대값(25)과 다름: ${afterScrollCount}`);
}

console.log(`PASS: 시나리오 4 - 무한스크롤로 카드 개수 ${initialCount} -> ${afterScrollCount}`);
```

## 시나리오 5: 존재하지 않는 상품 id로 상세 페이지 접근 시 404 처리

`product-catalog` 사용자 시나리오 7 / AC 근거.

**사전조건**: 해당 없음 (존재하지 않는 id를 사용하므로 상품 데이터 존재 여부와 무관).

**판정 기준**: `/products/999999` 접근 시 HTTP 응답 상태가 404다.

```javascript
const page = await browser.getPage("product-catalog-home-scenario-5");
const response = await page.goto("http://localhost:3000/products/999999", { waitUntil: "domcontentloaded" });
await page.waitForTimeout(300);

const status = response.status();
if (status !== 404) {
  const buf = await page.screenshot();
  await browser.saveScreenshot(buf, "scenario5-status-fail");
  throw new Error(`존재하지 않는 상품 id 접근 시 상태코드가 404가 아님: ${status}`);
}

console.log(`PASS: 시나리오 5 - 존재하지 않는 상품 id 접근 시 404 처리 확인 (status=${status})`);
```

## 시나리오 6: 상품이 0개일 때 그리드 대신 빈 상태 문구가 노출된다

`product-catalog` 사용자 시나리오 7 / `home-screen` AC 근거. 시나리오 1~4(상품 존재 전제)와 DB 상태가 충돌하므로, 검증 시점에 DB가 자연 상태로 0건이었던 것을 이용해 **시드 데이터 삽입 전에 가장 먼저 실행**했다.

**사전조건**: `product` 테이블이 0건이어야 함. (검증 시작 시점에 이미 0건이었음 — 위 "데이터 사전조건 메모" 참고)

**판정 기준**: `/` 접속 시 "아직 준비된 상품이 없어요" 문구가 노출되고, 그리드 카드가 0개다.

```javascript
const page = await browser.getPage("product-catalog-home-scenario-6");
await page.goto("http://localhost:3000/", { waitUntil: "networkidle" });
await page.waitForTimeout(500);
const emptyText = await page.getByText("아직 준비된 상품이 없어요").isVisible();
if (!emptyText) {
  const buf = await page.screenshot();
  await browser.saveScreenshot(buf, "scenario6-fail");
  throw new Error("빈 상태 문구가 노출되지 않음");
}
const gridCards = await page.locator("main section .grid > a").count();
if (gridCards !== 0) {
  throw new Error(`빈 상태여야 하는데 카드가 ${gridCards}개 존재함`);
}
console.log("PASS: 시나리오 6 - 빈 상태 문구 노출");
```
