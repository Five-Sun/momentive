---
date: 2026-08-31
feature: mypage-menu-cleanup
spec: 2026-08-31-mypage-menu-cleanup.md
plan: 2026-08-31-mypage-menu-cleanup.md
---

# 마이페이지 메뉴 정리 (반려견 프로필 관리 + 고객센터) E2E 케이스

사전 시딩 없이 스크립트 안에서 타임스탬프 기반 신규 계정을 회원가입해 로그인 상태를 만든 뒤 진행한다. 반려견 소유권 검증(`FORBIDDEN`)은 UI에서 접근할 경로 자체가 없고(다른 사용자의 petId를 노출하는 화면이 없음) `PetServiceTest`(Phase 1)로 이미 자동 검증되었으므로 이 파일에서는 다루지 않는다.

## 시나리오 1: 마이페이지에서 "반려견 프로필 관리" 클릭 → `/mypage/pets` 이동, 빈 상태 확인

spec 사용자 시나리오(반려견 프로필 관리) 1-2번, AC "마이페이지에서 '반려견 프로필 관리'를 누르면 `/mypage/pets`로 이동한다", "등록된 반려견이 없으면 빈 상태 안내와 등록 버튼이 보인다"를 검증한다.

**사전조건**: 신규 계정으로 회원가입해 로그인 상태(=반려견 미등록 상태)여야 한다 — 스크립트 시작 시 회원가입한다.

**판정 기준**: 메뉴 클릭 후 URL이 `/mypage/pets`를 포함하고, "등록된 반려견이 없어요" 안내와 "반려견 등록" 버튼이 보인다.

## 시나리오 2: 이름 없이 저장 시도 → 검증 에러

spec 사용자 시나리오 7번, AC "이름을 입력하지 않고 저장하면 검증 에러로 거부된다"를 검증한다.

**사전조건**: 시나리오 1 상태 이어받음(반려견 등록 폼이 열려 있는 상태).

**판정 기준**: 이름을 비운 채 "등록하기"를 누르면 "이름을 입력해주세요" 에러 메시지가 표시되고, 반려견 카드가 생성되지 않는다.

## 시나리오 3: 이름만 입력해 등록 → 목록 즉시 반영

spec 사용자 시나리오 3-4번, AC "이름만 입력해도 반려견을 등록할 수 있다", "등록한 반려견이 목록에 즉시 반영된다"를 검증한다.

**사전조건**: 시나리오 2 상태 이어받음(폼이 열려 있음).

**판정 기준**: 이름 "몽이"만 입력해 저장하면 목록에 "몽이" 카드가 즉시 나타난다.

## 시나리오 4: 반려견 수정 → 기존 값 채워진 폼 진입, 저장 후 카드 갱신

spec 사용자 시나리오 5번, AC "반려견 카드의 수정 버튼을 누르면 기존 값이 채워진 폼으로 진입해 수정할 수 있다"를 검증한다.

**사전조건**: 시나리오 3에서 등록한 "몽이" 카드가 목록에 있어야 한다.

CSS 셀렉터 fallback 사용(`div.border-hairline.bg-surface-card.flex.gap-3.rounded-md.border`, `mypage/pets/page.tsx`의 반려견 카드 루트 클래스 기준. 수정/삭제 아이콘 버튼은 `IconButton`에 접근 가능한 이름이 없어 role/text로 구분할 수 없으므로, 카드 컨테이너 안에서 버튼 순서(첫 번째=수정 연필, 두 번째=삭제 휴지통, JSX 선언 순서 기준)로 특정한다. 컴포넌트 구조 변경 시 갱신 필요).

**판정 기준**: "몽이" 카드의 수정(첫 번째) 버튼을 누르면 이름 입력란에 기존 값 "몽이"가 채워진 채로 폼이 열리고, 품종 "말티즈"/몸무게 "3.2"를 입력해 저장하면 카드에 "말티즈"와 "3.2kg"이 반영된다.

## 시나리오 5: 반려견 삭제 → 목록에서 제거, 삭제 후 재등록 가능

spec 사용자 시나리오 6번, AC "반려견을 삭제하면 목록에서 제거되고, 삭제 후 같은 사용자가 새 반려견을 다시 등록할 수 있다"를 검증한다.

**사전조건**: 시나리오 4에서 수정한 "몽이" 카드(현재 유일한 반려견)가 목록에 있어야 한다.

**판정 기준**: 삭제(두 번째) 버튼 클릭 → 확인 다이얼로그 수락 후 "몽이" 카드가 사라지고(반려견이 더 없으므로) 빈 상태 안내가 다시 보인다. 이어서 "보리"라는 이름으로 새 반려견을 등록하면 정상적으로 카드가 생성된다.

## 시나리오 6: 마이페이지에서 "고객센터" 클릭 → `/mypage/support` 이동, FAQ 아코디언 토글

spec 사용자 시나리오(고객센터) 1-2번, AC "마이페이지에서 '고객센터'를 누르면 `/mypage/support`로 이동한다", "FAQ 4개 항목이 노출되고, 클릭하면 답변이 펼쳐진다"를 검증한다.

**사전조건**: `/mypage`로 돌아간 상태여야 한다 — 상단 "뒤로가기" 버튼으로 이동한다.

**판정 기준**: "고객센터" 클릭 후 URL이 `/mypage/support`를 포함하고, "배송비는 얼마인가요?" FAQ 항목을 클릭하면 답변("배송비는 3,400원이며...")이 펼쳐지며, 다시 클릭하면 접힌다.

## 시나리오 7: 인스타그램 연락처 링크 확인

spec 사용자 시나리오(고객센터) 3번, AC "인스타그램 연락처를 누르면 실제 인스타그램 프로필(`https://instagram.com/momentive_official`)이 새 탭에서 열린다"를 검증한다. 실제 instagram.com 로딩 성공 여부는 테스트 환경의 외부 네트워크 접근성에 좌우되므로(코드 결함과 무관), `href`/`target` 속성이 정확한지와 클릭 시 실제로 새 탭(page)이 열리는지까지만 판정한다.

**사전조건**: 시나리오 6 상태 이어받음(`/mypage/support` 화면).

**판정 기준**: 인스타그램 링크의 `href`가 정확히 `https://instagram.com/momentive_official`이고 `target="_blank"`이며, 클릭 시 새 탭(page)이 열린다.

## 시나리오 8: 회귀 확인 — 배송조회/쿠폰함/적립금 메뉴는 여전히 무동작

Phase 3 plan의 수동 검증 항목("나머지 3개 메뉴가 여전히 무동작인지 확인(회귀 없음)")과 spec "배송조회"/"쿠폰함"/"적립금" 항목은 이번 spec에서 변경하지 않는다"를 검증한다.

**사전조건**: `/mypage`로 돌아간 상태여야 한다.

**판정 기준**: "배송조회"/"쿠폰함"/"적립금" 버튼을 각각 클릭해도 URL이 `/mypage`에서 변하지 않는다.

## 실행 스크립트

```javascript
const page = await browser.getPage("mypage-menu-cleanup");
const email = `e2e-pets-${Date.now()}@momentive.test`;
const password = "password123";
const nickname = `펫테스터${Date.now() % 10000}`;

// 사전 준비: 신규 계정 회원가입 (= 로그인 상태)
await page.goto("http://localhost:3000/signup", { waitUntil: "domcontentloaded" });
await page.getByLabel("이메일").fill(email);
await page.getByRole("textbox", { name: "비밀번호" }).fill(password);
await page.getByLabel("닉네임").fill(nickname);
await Promise.all([
  page.waitForURL("**/mypage", { timeout: 10000 }),
  page.getByRole("button", { name: "회원가입" }).click(),
]);

// --- 시나리오 1 ---
await Promise.all([
  page.waitForURL("**/mypage/pets", { timeout: 10000 }),
  page.getByRole("button", { name: "반려견 프로필 관리" }).click(),
]);
await page.waitForTimeout(500);

const emptyStateVisible = await page.getByText("등록된 반려견이 없어요").isVisible().catch(() => false);
const registerButtonVisible = await page.getByRole("button", { name: "반려견 등록" }).isVisible().catch(() => false);
if (!page.url().includes("/mypage/pets") || !emptyStateVisible || !registerButtonVisible) {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, "mypage-menu-cleanup-scenario-1-fail.png");
  throw new Error(`시나리오 1 판정 기준 미충족: /mypage/pets 이동 또는 빈 상태 UI 미노출 (url=${page.url()}, screenshot: ${path})`);
}
console.log("PASS: 시나리오 1");

// --- 시나리오 2 ---
await page.getByRole("button", { name: "반려견 등록" }).click();
await page.waitForTimeout(300);
await page.getByRole("button", { name: "등록하기" }).click();
await page.waitForTimeout(500);

const validationErrorVisible = await page.getByText("이름을 입력해주세요").isVisible().catch(() => false);
if (!validationErrorVisible) {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, "mypage-menu-cleanup-scenario-2-fail.png");
  throw new Error(`시나리오 2 판정 기준 미충족: 이름 미입력 시 검증 에러 미노출 (screenshot: ${path})`);
}
console.log("PASS: 시나리오 2");

// --- 시나리오 3 ---
await page.locator("input#name").fill("몽이");
await page.getByRole("button", { name: "등록하기" }).click();
await page.waitForTimeout(800);

const mongyCardVisible = await page.getByText("몽이", { exact: true }).isVisible().catch(() => false);
if (!mongyCardVisible) {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, "mypage-menu-cleanup-scenario-3-fail.png");
  throw new Error(`시나리오 3 판정 기준 미충족: 이름만 입력해 등록한 반려견이 목록에 반영되지 않음 (screenshot: ${path})`);
}
console.log("PASS: 시나리오 3");

// --- 시나리오 4 ---
const petCardSelector = "div.border-hairline.bg-surface-card.flex.gap-3.rounded-md.border";
const mongyCard = page.locator(petCardSelector, { hasText: "몽이" }).first();
await mongyCard.locator("button").nth(0).click(); // 수정(연필) 버튼
await page.waitForTimeout(300);

const nameFieldValue = await page.locator("input#name").inputValue().catch(() => "");
if (nameFieldValue !== "몽이") {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, "mypage-menu-cleanup-scenario-4-prefill-fail.png");
  throw new Error(`시나리오 4 판정 기준 미충족: 수정 폼에 기존 값이 채워지지 않음 (name=${nameFieldValue})`);
}

await page.locator("input#breed").fill("말티즈");
await page.locator("input#weightKg").fill("3.2");
await page.getByRole("button", { name: "수정 완료" }).click();
await page.waitForTimeout(800);

const updatedCard = page.locator(petCardSelector, { hasText: "몽이" }).first();
const updatedCardText = await updatedCard.innerText().catch(() => "");
if (!updatedCardText.includes("말티즈") || !updatedCardText.includes("3.2kg")) {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, "mypage-menu-cleanup-scenario-4-fail.png");
  throw new Error(`시나리오 4 판정 기준 미충족: 수정 후 카드에 변경 내용 미반영 (card text=${updatedCardText}, screenshot: ${path})`);
}
console.log("PASS: 시나리오 4");

// --- 시나리오 5 ---
page.once("dialog", (dialog) => dialog.accept());
const cardToDelete = page.locator(petCardSelector, { hasText: "몽이" }).first();
await cardToDelete.locator("button").nth(1).click(); // 삭제(휴지통) 버튼
await page.waitForTimeout(800);

const mongyGoneAfterDelete = !(await page.getByText("몽이", { exact: true }).isVisible().catch(() => false));
const emptyStateAfterDelete = await page.getByText("등록된 반려견이 없어요").isVisible().catch(() => false);
if (!mongyGoneAfterDelete || !emptyStateAfterDelete) {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, "mypage-menu-cleanup-scenario-5-delete-fail.png");
  throw new Error(`시나리오 5 판정 기준 미충족: 삭제 후 카드 미제거 또는 빈 상태 미노출 (screenshot: ${path})`);
}

await page.getByRole("button", { name: "반려견 등록" }).click();
await page.waitForTimeout(300);
await page.locator("input#name").fill("보리");
await page.getByRole("button", { name: "등록하기" }).click();
await page.waitForTimeout(800);

const boriCardVisible = await page.getByText("보리", { exact: true }).isVisible().catch(() => false);
if (!boriCardVisible) {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, "mypage-menu-cleanup-scenario-5-reregister-fail.png");
  throw new Error(`시나리오 5 판정 기준 미충족: 삭제 후 재등록 실패 (screenshot: ${path})`);
}
console.log("PASS: 시나리오 5");

// --- 시나리오 6 ---
await page.getByRole("button", { name: "뒤로가기" }).click();
await page.waitForTimeout(500);

await Promise.all([
  page.waitForURL("**/mypage/support", { timeout: 10000 }),
  page.getByRole("button", { name: "고객센터" }).click(),
]);
await page.waitForTimeout(500);

if (!page.url().includes("/mypage/support")) {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, "mypage-menu-cleanup-scenario-6-nav-fail.png");
  throw new Error(`시나리오 6 판정 기준 미충족: /mypage/support 이동 실패 (url=${page.url()}, screenshot: ${path})`);
}

await page.getByRole("button", { name: "배송비는 얼마인가요?" }).click();
await page.waitForTimeout(300);
const faqAnswerVisible = await page.getByText("배송비는 3,400원이며").isVisible().catch(() => false);
if (!faqAnswerVisible) {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, "mypage-menu-cleanup-scenario-6-faq-open-fail.png");
  throw new Error(`시나리오 6 판정 기준 미충족: FAQ 클릭 후 답변 미노출 (screenshot: ${path})`);
}

await page.getByRole("button", { name: "배송비는 얼마인가요?" }).click();
await page.waitForTimeout(300);
const faqAnswerHiddenAfterCollapse = !(await page.getByText("배송비는 3,400원이며").isVisible().catch(() => false));
if (!faqAnswerHiddenAfterCollapse) {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, "mypage-menu-cleanup-scenario-6-faq-collapse-fail.png");
  throw new Error(`시나리오 6 판정 기준 미충족: FAQ 재클릭 후 답변이 접히지 않음 (screenshot: ${path})`);
}
console.log("PASS: 시나리오 6");

// --- 시나리오 7 ---
const instaLink = page.locator('a[href="https://instagram.com/momentive_official"]');
const instaHref = await instaLink.getAttribute("href").catch(() => null);
const instaTarget = await instaLink.getAttribute("target").catch(() => null);
if (instaHref !== "https://instagram.com/momentive_official" || instaTarget !== "_blank") {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, "mypage-menu-cleanup-scenario-7-attr-fail.png");
  throw new Error(`시나리오 7 판정 기준 미충족: 인스타그램 링크 href/target 불일치 (href=${instaHref}, target=${instaTarget}, screenshot: ${path})`);
}

const [instaPage] = await Promise.all([
  page.context().waitForEvent("page", { timeout: 5000 }).catch(() => null),
  instaLink.click(),
]);
if (!instaPage) {
  const buf = await page.screenshot();
  const path = await saveScreenshot(buf, "mypage-menu-cleanup-scenario-7-newtab-fail.png");
  throw new Error(`시나리오 7 판정 기준 미충족: 인스타그램 링크 클릭 시 새 탭이 열리지 않음 (screenshot: ${path})`);
}
await instaPage.close().catch(() => {});
console.log("PASS: 시나리오 7");

// --- 시나리오 8 ---
await page.goto("http://localhost:3000/mypage", { waitUntil: "domcontentloaded" });
await page.waitForTimeout(500);

for (const label of ["배송조회", "쿠폰함", "적립금"]) {
  await page.getByRole("button", { name: label }).click();
  await page.waitForTimeout(300);
  if (!page.url().endsWith("/mypage")) {
    const buf = await page.screenshot();
    const path = await saveScreenshot(buf, `mypage-menu-cleanup-scenario-8-${label}-fail.png`);
    throw new Error(`시나리오 8 판정 기준 미충족: '${label}' 클릭 후 URL 변경됨 (url=${page.url()}, screenshot: ${path})`);
  }
}
console.log("PASS: 시나리오 8");
```
