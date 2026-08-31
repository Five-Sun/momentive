---
date: 2026-08-31
feature: product-review
spec: 2026-08-30-product-review.md
plan: 2026-08-30-product-review.md
---

# 상품 리뷰 (조회 + 작성) E2E 케이스 — 추가 검증(더보기/마스킹/목록 평점/위시리스트)

`docs/e2e/2026-08-30-product-review.md`에서 이미 pass 확인된 로그인/작성/수정/삭제/마이페이지 진입점 흐름은 재검증하지 않는다. 이번 파일은 그 파일에서 다루지 않은 spec 수용 기준(더보기 페이지네이션의 실제 다건 데이터 확인, 작성자 닉네임 마스킹 없음, 카테고리/검색 화면의 실제 평점 반영, 위시리스트 화면의 실제 평점 반영)만 다룬다.

사전 시딩 상태(테스트 실행 전 준비됨, 수정하지 않음):
- 상품 `id=61`("강아지 무릎담요", category=`ACCESSORY`)에 리뷰 6개, `averageRating=3.8`, `reviewCount=6`
- 리뷰 목록 API 기본 페이지 크기 5 → 최신순 정렬 시 1페이지(5개): `리뷰테스터`(가장 최신, "삭제 후 재작성 테스트 리뷰입니다"), `멍멍이맘`, `초코아빠`, `뽀삐사랑`, `강아지집사` / 2페이지(더보기, 1개): `털뭉치엄마`(가장 오래됨, "생각보다 얇아서 조금 아쉬웠던 상품입니다")
- 모든 시나리오는 비로그인 상태로 진행 가능(로그인 불필요)

## 시나리오 1: 리뷰 목록 최신순 5개 + "더보기"로 6번째 로드

spec "사용자 시나리오 1"의 2번(리뷰 목록 최신순 일부(5개) 노출, "더보기"로 다음 페이지 로드), AC "상품상세 화면에서 리뷰 목록이 최신순으로 노출되고, '더보기'로 다음 페이지를 불러올 수 있다"에서 도출. 실제로 6개 이상의 리뷰가 있는 상품에서 더보기 클릭 전/후 리뷰 개수 변화를 다건 데이터로 검증한다(기존 2026-08-30 파일은 리뷰 0→1개 상태에서만 검증해 다건 페이지네이션은 다루지 않았음).

**사전조건**: 비로그인 상태에서 `/products/61` 진입.

**판정 기준**: 진입 직후 1페이지 리뷰 5개(`리뷰테스터`, `멍멍이맘`, `초코아빠`, `뽀삐사랑`, `강아지집사`의 리뷰 텍스트)가 모두 보이고, 6번째 리뷰(`털뭉치엄마`, "생각보다 얇아서 조금 아쉬웠던 상품입니다")는 보이지 않음. "더보기" 버튼 클릭 후 `털뭉치엄마`의 리뷰 텍스트가 추가로 나타남.

## 시나리오 2: 리뷰 작성자 닉네임 마스킹 없이 노출

spec "In Scope"의 "리뷰 작성자 표시: `User.nickname` 그대로 노출(마스킹 없음)", AC "리뷰 목록/작성 폼에 표시되는 작성자 이름은 `User.nickname` 그대로 노출된다(마스킹 없음)"에서 도출.

**사전조건**: 시나리오 1 상태(상품 61 상세, 더보기까지 로드된 상태) 이어받음.

**판정 기준**: 리뷰 목록에 `멍멍이맘` 닉네임이 마스킹 없이 정확한 전체 텍스트로 노출됨(예: "멍**" 같은 마스킹 형태가 아님).

## 시나리오 3: 카테고리 화면에서 실제 평점 반영

spec "사용자 시나리오 5"(목록 화면 실제 `averageRating` 표시), AC "홈/카테고리/검색/상품상세/위시리스트 5개 화면의 상품 카드가 하드코딩된 4.5 대신 실제 `averageRating`을 표시"에서 도출. `docs/e2e/2026-08-30-product-review.md`는 홈 화면만 검증했고 카테고리(`/search?category=...`)와 검색 화면은 다루지 않았다.

**사전조건**: `/search?category=ACCESSORY`로 이동.

CSS 셀렉터 fallback 사용(`.grid.grid-cols-2 div.flex.w-full.flex-col.gap-2`, `ProductCard.tsx`의 카드 루트 요소 클래스 기준. 컴포넌트 구조 변경 시 갱신 필요): 그리드 컨테이너 안에서 카드 루트 단위로 좁혀 특정 상품 카드 하나만 텍스트를 확인한다(2026-08-31 backlog 02번 재발 방지 사항 반영 — 그리드 컨테이너 전체 텍스트로 판정하면 다른 카드의 값과 섞일 수 있음).

**판정 기준**: "강아지 무릎담요" 카드(카드 루트 요소) 안에 "3.8"이 표시됨(하드코딩 "4.5" 아님).

## 시나리오 4: 검색 화면에서 실제 평점 반영

spec "사용자 시나리오 5", AC 동일 항목에서 도출. 검색어로 상품을 찾았을 때도 카드 평점이 실제 값으로 보이는지 확인.

**사전조건**: `/search`로 이동, 검색창에 "무릎담요" 입력 후 Enter로 검색 제출.

CSS 셀렉터 fallback 사용(시나리오 3과 동일한 `.grid.grid-cols-2 div.flex.w-full.flex-col.gap-2` 카드 루트 기준).

**판정 기준**: 검색 결과 그리드에 "강아지 무릎담요" 카드가 노출되고 그 카드 안에 "3.8"이 표시됨.

## 시나리오 5: 위시리스트 추가 후 위시리스트 화면에서 실제 평점 반영

spec "사용자 시나리오 5", AC 동일 항목 + 위시리스트 토글 기능 자체(상품상세 하단 "위시 담기" 버튼)에서 도출.

**사전조건**: `/products/61` 상세로 이동(로컬스토리지 위시리스트는 스크립트 시작 시 초기화됨). 하단 고정 영역의 "위시 담기" 버튼으로 위시리스트에 추가.

CSS 셀렉터 fallback 사용(시나리오 3과 동일한 `.grid.grid-cols-2 div.flex.w-full.flex-col.gap-2` 카드 루트 기준. 위시리스트 화면은 카드가 `<a>`가 아닌 `onClick` div로 감싸여 있어 카드 루트 자체를 기준으로 삼음).

**판정 기준**: "위시 담기" 클릭 후 버튼 라벨이 "위시 완료"로 바뀜. `/wishlist`로 이동하면 "강아지 무릎담요" 카드가 보이고 그 카드 안에 "3.8"이 표시됨.

## 실행 스크립트

```javascript
const page = await browser.getPage("product-review");

// 위시리스트 로컬스토리지 초기화 (시나리오 5 결정성 확보)
await page.goto("http://localhost:3000/", { waitUntil: "domcontentloaded" });
await page.evaluate(() => localStorage.removeItem("momentive:wishlist"));

// --- 시나리오 1 ---
await page.goto("http://localhost:3000/products/61", { waitUntil: "domcontentloaded" });
await page.waitForTimeout(1500);

const firstPageTexts = [
  "삭제 후 재작성 테스트 리뷰입니다",
  "겨울에 정말 따뜻하고 좋아요 강추합니다",
  "두께감이 적당하고 세탁도 편해서 만족합니다",
  "무난하게 쓸만한 담요입니다 재구매 의사 있어요",
  "강아지가 이 담요만 찾아요 최고의 선택이었습니다",
];
const oldestReviewText = "생각보다 얇아서 조금 아쉬웠던 상품입니다";

for (const t of firstPageTexts) {
  if (!(await page.getByText(t).isVisible().catch(() => false))) {
    const buf = await page.screenshot();
    await saveScreenshot(buf, "product-review-scenario-1-missing-first-page-review");
    throw new Error(`시나리오 1 판정 기준 미충족: 1페이지 리뷰("${t}")가 보이지 않음`);
  }
}
if (await page.getByText(oldestReviewText).isVisible().catch(() => false)) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-1-oldest-shown-early");
  throw new Error("시나리오 1 판정 기준 미충족: 더보기 클릭 전인데 6번째 리뷰가 이미 보임");
}

await page.getByRole("button", { name: "더보기" }).click();
await page.waitForTimeout(1200);

if (!(await page.getByText(oldestReviewText).isVisible().catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-1-loadmore-failed");
  throw new Error("시나리오 1 판정 기준 미충족: '더보기' 클릭 후 6번째 리뷰가 로드되지 않음");
}
console.log("PASS: 시나리오 1");

// --- 시나리오 2 ---
if (!(await page.getByText("멍멍이맘", { exact: true }).isVisible().catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-2-nickname-not-visible");
  throw new Error("시나리오 2 판정 기준 미충족: 작성자 닉네임 '멍멍이맘'이 마스킹 없이 노출되지 않음");
}
console.log("PASS: 시나리오 2");

// --- 시나리오 3 ---
await page.goto("http://localhost:3000/search?category=ACCESSORY", { waitUntil: "domcontentloaded" });
await page.waitForTimeout(1500);

const categoryCard = page
  .locator(".grid.grid-cols-2 div.flex.w-full.flex-col.gap-2", { hasText: "강아지 무릎담요" })
  .first();
if (!(await categoryCard.isVisible().catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-3-no-grid");
  throw new Error("시나리오 3 판정 기준 미충족: 카테고리 화면 상품 그리드를 찾을 수 없음");
}
const categoryCardText = await categoryCard.innerText();
if (!categoryCardText.includes("3.8")) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-3-wrong-rating");
  throw new Error("시나리오 3 판정 기준 미충족: 카테고리 화면 카드에 실제 평점(3.8)이 반영되지 않음. 그리드 내용: " + categoryCardText);
}
console.log("PASS: 시나리오 3");

// --- 시나리오 4 ---
await page.goto("http://localhost:3000/search", { waitUntil: "domcontentloaded" });
await page.waitForTimeout(500);
await page.getByPlaceholder("브랜드, 상품 검색").fill("무릎담요");
await page.getByPlaceholder("브랜드, 상품 검색").press("Enter");
await page.waitForTimeout(1500);

const searchCard = page
  .locator(".grid.grid-cols-2 div.flex.w-full.flex-col.gap-2", { hasText: "강아지 무릎담요" })
  .first();
if (!(await searchCard.isVisible().catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-4-no-grid");
  throw new Error("시나리오 4 판정 기준 미충족: 검색 결과 그리드에서 상품 카드를 찾을 수 없음");
}
const searchCardText = await searchCard.innerText();
if (!searchCardText.includes("3.8")) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-4-wrong-rating");
  throw new Error("시나리오 4 판정 기준 미충족: 검색 결과 카드에 실제 평점(3.8)이 반영되지 않음. 그리드 내용: " + searchCardText);
}
console.log("PASS: 시나리오 4");

// --- 시나리오 5 ---
await page.goto("http://localhost:3000/products/61", { waitUntil: "domcontentloaded" });
await page.waitForTimeout(1500);

await page.getByRole("button", { name: "위시 담기" }).click();
await page.waitForTimeout(500);
if (!(await page.getByRole("button", { name: "위시 완료" }).isVisible().catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-5-toggle-failed");
  throw new Error("시나리오 5 판정 기준 미충족: '위시 담기' 클릭 후 '위시 완료'로 상태가 바뀌지 않음");
}

await page.goto("http://localhost:3000/wishlist", { waitUntil: "domcontentloaded" });
await page.waitForTimeout(1500);

const wishlistCard = page
  .locator(".grid.grid-cols-2 div.flex.w-full.flex-col.gap-2", { hasText: "강아지 무릎담요" })
  .first();
if (!(await wishlistCard.isVisible().catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-5-no-card");
  throw new Error("시나리오 5 판정 기준 미충족: 위시리스트 화면에서 상품 카드를 찾을 수 없음");
}
const wishlistCardText = await wishlistCard.innerText();
if (!wishlistCardText.includes("3.8")) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-5-wrong-rating");
  throw new Error("시나리오 5 판정 기준 미충족: 위시리스트 화면 카드에 실제 평점(3.8)이 반영되지 않음. 그리드 내용: " + wishlistCardText);
}
console.log("PASS: 시나리오 5");
```
