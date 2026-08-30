---
date: 2026-08-30
feature: product-review
spec: 2026-08-30-product-review.md
plan: 2026-08-30-product-review.md
---

# 상품 리뷰 (조회 + 작성) E2E 케이스

사전 시딩 상태(테스트 실행 전 준비됨):
- 로그인 계정: `e2e-review-buyer@momentive.test` / 비밀번호 `Test1234!`(닉네임: 리뷰테스터)
- 이 계정의 `PAID` 주문(`orderId=158`)에 상품 `id=279`("강아지 무릎담요")가 포함되어 있고, 이 상품은 현재 리뷰 0개(`averageRating=null`, `reviewCount=0`) 상태

## 시나리오 1: 로그인 + 리뷰 없는 상품상세 진입 (빈 상태 + 리뷰 쓰기 버튼)

spec "사용자 시나리오 1(리뷰 조회)"의 3번(리뷰 0개 빈 상태 안내), "사용자 시나리오 2"의 1번(구매 이력 있으면 리뷰 쓰기 버튼 활성화)에서 도출. 상품 279는 리뷰 0개이므로 평점 요약 영역이 생략되고, 로그인한 구매자에게는 "리뷰 쓰기" 버튼이 노출되어야 한다.

**사전조건**: 비로그인 상태에서 시작. 로그인 후 상품 279(`/products/279`) 상세로 진입.

**판정 기준**: 로그인 성공 후 `/mypage`로 이동. 상품상세 진입 시 평점 요약(`Rating` 컴포넌트, 별점 숫자) 요소가 보이지 않음(리뷰 0개). "아직 작성된 리뷰가 없어요." 텍스트가 보임. "리뷰 쓰기" 버튼(구매 확인 완료 텍스트)이 보임.

## 시나리오 2: 리뷰 작성 → 목록/평점 즉시 반영

spec "사용자 시나리오 2"의 4번(별점+텍스트 입력 후 제출 시 리뷰 등록 및 averageRating/reviewCount 즉시 갱신), AC "리뷰 작성/수정/삭제 시 averageRating/reviewCount가 즉시 재계산되어 반영된다"에서 도출.

**사전조건**: 시나리오 1 상태(상품 279 상세, 로그인됨, 리뷰 폼 진입 전) 이어받음.

**판정 기준**: "리뷰 쓰기" 클릭 → 별점 5점 선택 + 텍스트("정말 좋은 상품이에요 강아지가 좋아해요") 입력 후 "리뷰 등록" 제출 → "리뷰를 등록했어요" 토스트 노출 → 리뷰 목록에 방금 작성한 리뷰 카드(작성자 닉네임 "리뷰테스터", 텍스트) 노출 → 평점 요약 영역에 "5.0" 표시.

## 시나리오 3: 이미 작성한 상품 재진입 시 수정 폼으로 전환 + 리뷰 수정

spec "사용자 시나리오 2"의 3번(이미 작성한 경우 버튼 클릭 시 기존 값 채운 수정 폼), "사용자 시나리오 4"의 2번(수정 시 텍스트/별점 갱신 및 평점 재계산), AC "이미 리뷰를 작성한 상품에서 리뷰 쓰기 버튼을 누르면 기존 내용이 채워진 수정 폼으로 진입한다"에서 도출.

**사전조건**: 시나리오 2에서 리뷰를 작성해 버튼이 "리뷰 수정"으로 바뀐 상태 이어받음.

**판정 기준**: "리뷰 수정" 버튼 클릭 시 별점 5점이 이미 선택되어 있고 텍스트란에 기존 문구가 채워져 있음. 별점을 3점으로 변경, 텍스트를 다른 문구("생각보다 보통이었어요 그래도 만족합니다")로 변경 후 "수정 완료" 제출 → "리뷰를 수정했어요" 토스트 노출 → 리뷰 카드에 변경된 텍스트 반영 → 평점 요약이 "3.0"으로 갱신.

## 시나리오 4: 마이페이지 주문내역 상세에서 리뷰 진입점 노출

spec "사용자 시나리오 3"(마이페이지 주문내역에서 PAID 주문 상품마다 리뷰 버튼), AC "마이페이지 주문내역 상세에서 PAID 주문에 포함된 상품마다 리뷰 작성/수정 버튼이 개별로 노출된다"에서 도출.

**사전조건**: 시나리오 3에서 이미 리뷰를 작성한 상태 이어받음. `/mypage/orders/158`로 직접 이동(주문 158은 PAID, 상품 279 포함).

**판정 기준**: 상품 279 카드 아래에 "리뷰 수정" 버튼(이미 리뷰 작성됨을 반영)이 노출됨. 버튼 클릭 시 기존 값(별점 3, 방금 수정한 텍스트)이 채워진 폼이 인라인으로 펼쳐짐. 하단 고정 취소 버튼 영역과 겹치지 않고 별도로 렌더링됨(주문 158이 PAID이므로 "주문 취소" 버튼도 함께 존재).

## 시나리오 5: 목록 화면(홈)에서 실제 평점 반영

spec "사용자 시나리오 5"(목록 화면 실제 averageRating 표시), AC "홈/카테고리/검색/상품상세/위시리스트 5개 화면의 상품 카드가 하드코딩된 4.5 대신 실제 averageRating을 표시"에서 도출.

**사전조건**: 시나리오 4까지 이어받은 상태에서 홈(`/`)으로 이동.

**판정 기준**: 홈 화면에서 "강아지 무릎담요" 카드에 표시된 평점이 "3.0"(시나리오 3에서 갱신한 값)이며, 하드코딩된 "4.5"가 아님.

## 시나리오 6: 리뷰 삭제 후 재작성 가능

spec "사용자 시나리오 4"의 3번(삭제 후 같은 상품 재작성 가능), AC "리뷰 삭제 후에는 같은 사용자가 같은 상품에 다시 리뷰를 작성할 수 있다"에서 도출.

**사전조건**: 상품 279 상세(`/products/279`)로 다시 이동. 시나리오 3에서 작성한 본인 리뷰가 목록에 존재하는 상태.

**판정 기준**: 본인 리뷰 카드의 "삭제" 버튼 클릭 → 확인 다이얼로그 수락 → "리뷰를 삭제했어요" 토스트 노출 → 리뷰 목록에서 해당 카드 사라지고 "아직 작성된 리뷰가 없어요." 노출 → "리뷰 쓰기" 버튼 클릭 시 빈 폼(별점 미선택, 텍스트 빈칸)으로 진입 → 별점 4점 + 텍스트("삭제 후 재작성 테스트 리뷰입니다") 입력 후 제출 → "리뷰를 등록했어요" 토스트 및 목록에 새 리뷰 반영.

## 실행 스크립트

```javascript
const page = await browser.getPage("product-review");

// --- 시나리오 1 ---
await page.goto("http://localhost:3000/login", { waitUntil: "domcontentloaded" });
await page.getByLabel("이메일").fill("e2e-review-buyer@momentive.test");
await page.getByRole("textbox", { name: "비밀번호" }).fill("Test1234!");
await page.getByRole("button", { name: "로그인" }).click();
await page.waitForURL("**/mypage", { timeout: 10000 });

await page.goto("http://localhost:3000/products/279", { waitUntil: "domcontentloaded" });
await page.waitForTimeout(1500);

if (await page.getByText("아직 작성된 리뷰가 없어요.").isVisible().catch(() => false) === false) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-1-empty-state");
  throw new Error("시나리오 1 판정 기준 미충족: 빈 리뷰 상태 안내 문구가 보이지 않음");
}
if (!(await page.getByRole("button", { name: "리뷰 쓰기" }).isVisible())) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-1-no-write-button");
  throw new Error("시나리오 1 판정 기준 미충족: 구매 확인된 사용자에게 '리뷰 쓰기' 버튼이 보이지 않음");
}
console.log("PASS: 시나리오 1");

// --- 시나리오 2 ---
await page.getByRole("button", { name: "리뷰 쓰기" }).click();
await page.getByRole("button", { name: "별점 5점" }).click();
await page.locator("#review-text").fill("정말 좋은 상품이에요 강아지가 좋아해요");
await page.getByRole("button", { name: "리뷰 등록" }).click();
await page.waitForTimeout(1200);

if (!(await page.getByText("리뷰를 등록했어요").isVisible().catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-2-no-toast");
  throw new Error("시나리오 2 판정 기준 미충족: '리뷰를 등록했어요' 토스트가 보이지 않음");
}
if (!(await page.getByText("정말 좋은 상품이에요 강아지가 좋아해요").isVisible())) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-2-no-card");
  throw new Error("시나리오 2 판정 기준 미충족: 작성한 리뷰가 목록에 보이지 않음");
}
if (!(await page.getByText("5.0").isVisible())) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-2-no-rating");
  throw new Error("시나리오 2 판정 기준 미충족: 평점 요약이 5.0으로 갱신되지 않음");
}
console.log("PASS: 시나리오 2");

// --- 시나리오 3 ---
await page.getByRole("button", { name: "리뷰 수정" }).click();
await page.waitForTimeout(300);
const ratingFilled = await page.getByRole("button", { name: "별점 5점" }).evaluate((el) => el.className.includes("text-brand-pink"));
if (!ratingFilled) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-3-not-prefilled-rating");
  throw new Error("시나리오 3 판정 기준 미충족: 수정 폼 진입 시 기존 별점이 채워지지 않음");
}
const textValue = await page.locator("#review-text").inputValue();
if (textValue !== "정말 좋은 상품이에요 강아지가 좋아해요") {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-3-not-prefilled-text");
  throw new Error("시나리오 3 판정 기준 미충족: 수정 폼 진입 시 기존 텍스트가 채워지지 않음");
}
await page.getByRole("button", { name: "별점 3점" }).click();
await page.locator("#review-text").fill("생각보다 보통이었어요 그래도 만족합니다");
await page.getByRole("button", { name: "수정 완료" }).click();
await page.waitForTimeout(1200);

if (!(await page.getByText("리뷰를 수정했어요").isVisible().catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-3-no-toast");
  throw new Error("시나리오 3 판정 기준 미충족: '리뷰를 수정했어요' 토스트가 보이지 않음");
}
if (!(await page.getByText("생각보다 보통이었어요 그래도 만족합니다").isVisible())) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-3-no-updated-text");
  throw new Error("시나리오 3 판정 기준 미충족: 수정된 리뷰 텍스트가 목록에 반영되지 않음");
}
if (!(await page.getByText("3.0").isVisible())) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-3-no-updated-rating");
  throw new Error("시나리오 3 판정 기준 미충족: 평점 요약이 3.0으로 갱신되지 않음");
}
console.log("PASS: 시나리오 3");

// --- 시나리오 4 ---
await page.goto("http://localhost:3000/mypage/orders/158", { waitUntil: "domcontentloaded" });
await page.waitForTimeout(1500);

if (!(await page.getByRole("button", { name: "리뷰 수정" }).isVisible())) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-4-no-button");
  throw new Error("시나리오 4 판정 기준 미충족: 주문내역 상세에서 '리뷰 수정' 버튼이 보이지 않음");
}
await page.getByRole("button", { name: "리뷰 수정" }).click();
await page.waitForTimeout(300);
const orderPageTextValue = await page.locator("#review-text").inputValue();
if (orderPageTextValue !== "생각보다 보통이었어요 그래도 만족합니다") {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-4-not-prefilled");
  throw new Error("시나리오 4 판정 기준 미충족: 마이페이지 진입점의 수정 폼에 기존 리뷰 값이 채워지지 않음");
}
if (!(await page.getByRole("button", { name: "주문 취소" }).isVisible())) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-4-no-cancel-button");
  throw new Error("시나리오 4 판정 기준 미충족: PAID 주문의 하단 취소 버튼이 함께 노출되지 않음");
}
console.log("PASS: 시나리오 4");

// --- 시나리오 5 ---
await page.goto("http://localhost:3000/", { waitUntil: "domcontentloaded" });
await page.waitForTimeout(1500);

const gridItem = page.getByText("강아지 무릎담요").first();
if (!(await gridItem.isVisible())) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-5-no-product-card");
  throw new Error("시나리오 5 판정 기준 미충족: 홈 화면에서 상품 카드를 찾을 수 없음");
}
const cardContainer = await gridItem.locator("xpath=ancestor::a[1]");
const cardText = await cardContainer.innerText();
if (!cardText.includes("3.0")) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-5-wrong-rating");
  throw new Error("시나리오 5 판정 기준 미충족: 홈 화면 카드에 실제 평점(3.0)이 반영되지 않음. 카드 내용: " + cardText);
}
console.log("PASS: 시나리오 5");

// --- 시나리오 6 ---
await page.goto("http://localhost:3000/products/279", { waitUntil: "domcontentloaded" });
await page.waitForTimeout(1500);

page.once("dialog", (dialog) => dialog.accept());
await page.getByRole("button", { name: "삭제" }).click();
await page.waitForTimeout(1200);

if (!(await page.getByText("리뷰를 삭제했어요").isVisible().catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-6-no-delete-toast");
  throw new Error("시나리오 6 판정 기준 미충족: '리뷰를 삭제했어요' 토스트가 보이지 않음");
}
if (!(await page.getByText("아직 작성된 리뷰가 없어요.").isVisible().catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-6-not-empty-after-delete");
  throw new Error("시나리오 6 판정 기준 미충족: 삭제 후 리뷰 목록이 비어있지 않음");
}
if (!(await page.getByRole("button", { name: "리뷰 쓰기" }).isVisible())) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-6-no-rewrite-button");
  throw new Error("시나리오 6 판정 기준 미충족: 삭제 후 '리뷰 쓰기' 버튼이 다시 보이지 않음");
}
await page.getByRole("button", { name: "리뷰 쓰기" }).click();
await page.waitForTimeout(300);
const reWriteTextValue = await page.locator("#review-text").inputValue();
if (reWriteTextValue !== "") {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-6-not-empty-form");
  throw new Error("시나리오 6 판정 기준 미충족: 재작성 폼이 빈 값으로 시작하지 않음");
}
await page.getByRole("button", { name: "별점 4점" }).click();
await page.locator("#review-text").fill("삭제 후 재작성 테스트 리뷰입니다");
await page.getByRole("button", { name: "리뷰 등록" }).click();
await page.waitForTimeout(1200);

if (!(await page.getByText("리뷰를 등록했어요").isVisible().catch(() => false))) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-6-no-final-toast");
  throw new Error("시나리오 6 판정 기준 미충족: 재작성 후 등록 토스트가 보이지 않음");
}
if (!(await page.getByText("삭제 후 재작성 테스트 리뷰입니다").isVisible())) {
  const buf = await page.screenshot();
  await saveScreenshot(buf, "product-review-scenario-6-no-final-card");
  throw new Error("시나리오 6 판정 기준 미충족: 재작성한 리뷰가 목록에 반영되지 않음");
}
console.log("PASS: 시나리오 6");
```
