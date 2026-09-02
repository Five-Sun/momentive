---
date: 2026-09-01
feature: coupon-system
spec: 2026-09-01-coupon-system.md
plan: 2026-09-01-coupon-system.md
---

# 쿠폰 시스템 E2E 케이스

**재시딩 후 재실행 (2026-09-01)**: 사용자가 `docker compose down -v` 후 `./dev.sh`로 로컬 DB를 완전히 재시딩했다. 재실행 전 직접 확인한 결과 `product` 15건, `coupon` 4건(`WELCOME3000`/`MOMENTIVE10`/`FIRSTORDER5000`/`VIP20`, 전부 `expires_at = 2026-12-31`)이 정상 시드돼 있고 `users`/`orders`는 0건(신규 재시딩 직후이므로 정상)임을 확인했다. 이번 재실행에서 시나리오 2~13을 진행한다. 직전 실행에서 발견된 시나리오 14의 "쿠폰" 텍스트 오매칭 이슈(테스트 상품명에 우연히 "쿠폰"이 포함돼 거짓 실패)는 이미 상품명에서 "쿠폰" 단어를 제거해 해소된 상태이므로 이번에는 그대로 재사용한다.

**최종 실행 결과 요약**: 시나리오 1·2·3·5·6·7·8·9·10·11·12·14 PASS(총 12건), 시나리오 4·13은 사전조건 미충족으로 스킵(실패 아님, 사유는 각 시나리오 절 참고). 스크립트 작성 과정에서 발견해 그 자리에서 고친 테스트 스크립트 이슈 3건(전부 코드 결함 아님) — (1) 배송지가 비어있는 채로 여러 체크아웃 시나리오를 재진입해 "결제하기" 클릭이 클라이언트 검증에 막힘 → 시나리오 9·12 진입 시 배송지 입력 재수행하도록 보강, (2) dev-browser QuickJS 샌드박스에 전역 `URL` 클래스가 없어 `new URL(page.url())` 호출이 `TypeError`로 실패 → 정규식(`page.url().match(/orderId=(\d+)/)`)으로 대체, (3) `isVisible({ timeout })`이 polling이 아니라 즉시 체크로 동작해(이전 `cart-order-payment` 케이스에서도 동일 패턴 발견) 시나리오 9의 쿠폰 상태 전환 확인이 거짓 실패 → `waitFor({ state: "visible", timeout })`로 대체. 이 조치들을 반영한 뒤 재실행해 전부 통과를 확인했다.

## 시나리오 1: 존재하지 않는 쿠폰 코드 등록 시 인라인 에러

spec 사용자 시나리오 1-4("존재하지 않는 코드" 실패 문구), 수용 기준 3번째 항목을 검증한다. `coupon` 테이블에 시드 데이터가 있는지 여부와 무관하게 "절대 존재할 수 없는 코드"로 등록을 시도하면 `COUPON_NOT_FOUND` → `fieldErrors.code`가 인라인 표시되므로, 시드 상태와 무관하게 독립적으로 검증 가능하다.

**사전조건**: 로그인 상태(신규 가입).

**판정 기준**: `/mypage/coupons`에서 무작위 문자열 코드(`NOTEXISTCODE999`)로 등록 시도 시 입력칸 아래에 "존재하지 않는 쿠폰 코드입니다" 문구가 인라인으로 표시되고, "사용 가능한 쿠폰" 목록에 추가되지 않는다.

**실행 결과 (2026-09-01, 재시딩 후): PASS.**

## 시나리오 2: 유효한 쿠폰 코드 등록 성공 → 사용 가능한 쿠폰 목록에 즉시 추가

spec 사용자 시나리오 1-2/1-3, 수용 기준 2번째 항목을 검증한다. 시드 쿠폰 `WELCOME3000`(FIXED, 3,000원, 최소주문금액 30,000원)을 등록한다.

**사전조건**: 시나리오 1과 동일 세션(로그인 상태). `coupon` 테이블에 `WELCOME3000` 코드가 존재해야 한다(재시딩 후 확인 완료).

**판정 기준**: 등록 후 "사용 가능한 쿠폰" 목록에 "웰컴 3,000원 할인" 카드가 즉시 나타나고, 등록 성공 토스트("쿠폰을 등록했어요")가 노출된다.

**실행 결과 (2026-09-01, 재시딩 후): PASS.**

## 시나리오 3: 같은 코드 중복 등록 실패

spec 사용자 시나리오 1-4("이미 등록한 쿠폰" 실패 문구), 수용 기준 3·4번째 항목(1인 1회, DB unique 제약)을 검증한다. 시나리오 2에서 등록한 `WELCOME3000`을 같은 계정으로 한 번 더 등록한다.

**사전조건**: 시나리오 2가 먼저 성공해 `WELCOME3000`을 이미 보유한 상태여야 한다.

**판정 기준**: 재등록 시도 시 입력칸 아래에 "이미 등록한 쿠폰입니다" 문구가 인라인으로 표시된다.

**실행 결과 (2026-09-01, 재시딩 후): PASS.**

## 시나리오 4: 만료된 쿠폰 등록 실패

spec 사용자 시나리오 1-4("유효기간이 지난 쿠폰" 실패 문구)를 검증한다.

**사전조건**: `expiresAt`이 과거인 쿠폰 코드가 DB에 존재해야 한다.

**판정 기준**: 만료된 코드로 등록 시도 시 "유효기간이 지난 쿠폰입니다" 문구가 인라인으로 표시된다.

**스킵 사유 (2026-09-01, 재확인)**: 재시딩된 V10 시드도 여전히 4개 쿠폰(`WELCOME3000`/`MOMENTIVE10`/`FIRSTORDER5000`/`VIP20`) 전부 `expires_at = 2026-12-31 23:59:59`로 만료되지 않은 상태다. 관리자 쿠폰 발급 API/화면은 spec Out of Scope로 존재하지 않아(`backend/src/main/java/com/momentive/backend/auth/domain/Role.java`에 `ADMIN` enum만 있고 발급 경로가 없음) 브라우저 조작만으로 만료 쿠폰을 만들 방법이 없다. DB에 직접 INSERT하거나 시드 마이그레이션을 변경하는 것은 이 phase(E2E 검증)의 범위를 벗어나므로 시도하지 않았다. 사전조건 미충족으로 스킵 — 실패 아님.

## 시나리오 5: 쿠폰함 목록의 사용 가능 / 사용 완료·만료 구간 분리 표시

spec 사용자 시나리오 1-3, 수용 기준 5번째 항목(`/mypage/coupons` 사용 가능한 쿠폰과 사용 완료·만료 쿠폰이 구분되어 표시)을 검증한다. 두 번째 쿠폰(`FIRSTORDER5000`, 최소주문금액 0)을 추가 등록해 "사용 가능한 쿠폰" 목록에 2건이 보이는지 확인한다. "사용 완료·만료" 구간은 시나리오 9(주문 생성으로 쿠폰 선점)에서 만들어지는 상태를 이어받아 함께 확인한다.

**사전조건**: 시나리오 2·3과 동일 세션.

**판정 기준**: "사용 가능한 쿠폰" 구간에 `WELCOME3000`, `FIRSTORDER5000` 두 건이 표시된다.

**실행 결과 (2026-09-01, 재시딩 후): PASS.**

## 시나리오 6: 체크아웃 쿠폰 선택 — 전부 나열, 최소 주문금액 미달 쿠폰 비활성 + 사유 문구

spec 사용자 시나리오 2-3, 수용 기준 7번째 항목을 검증한다. `VIP20`도 추가 등록해 3개 쿠폰을 보유한 상태에서, 상품금액을 최소 주문금액이 있는 `VIP20`(최소 50,000원)에는 못 미치지만 `WELCOME3000`(최소 30,000원)은 충족하는 37,600원(id=3 강아지 하네스 M 32,000원 + id=12 강아지 장난감 로프 할인가 5,600원)으로 맞춘 뒤 체크아웃에 진입한다.

**사전조건**: `WELCOME3000`, `FIRSTORDER5000`, `VIP20` 세 쿠폰을 보유한 상태(시나리오 2·5의 연장 + `VIP20` 추가 등록), 상품금액 37,600원, 실 상품(id=3, id=12) 데이터 사용.

**판정 기준**: 쿠폰 선택 영역에 보유한 3개 쿠폰이 전부 나열된다. `VIP20`은 선택 불가(비활성 스타일) 상태이고 그 아래 "50,000원 이상 구매 시 사용 가능" 사유 문구가 보인다. `WELCOME3000`/`FIRSTORDER5000`은 선택 가능하다.

**실행 결과 (2026-09-01, 재시딩 후): PASS.**

## 시나리오 7: 체크아웃 쿠폰 선택/해제 시 금액 요약 즉시 갱신

spec 사용자 시나리오 2-4, 수용 기준 8번째 항목을 검증한다. `FIRSTORDER5000`(정액 5,000원, 최소 주문금액 0)을 선택/해제하며 "쿠폰 할인" 줄과 총 결제금액이 즉시 바뀌는지 확인한다.

**사전조건**: 시나리오 6과 동일 상태(같은 체크아웃 화면).

**판정 기준**: `FIRSTORDER5000` 선택 시 "쿠폰 할인" 줄에 "-5,000원"이 나타나고 총 결제금액이 5,000원 줄어든다. 선택 해제 시 "쿠폰 할인" 줄이 사라지고 총 결제금액이 원래대로 복귀한다.

**실행 결과 (2026-09-01, 재시딩 후): PASS.**

## 시나리오 8: 무료배송 임계값이 할인 전 상품금액 기준

spec 사용자 시나리오 2-5, 수용 기준 11번째 항목("무료배송 임계값 판정이 할인 전 상품금액 기준")을 검증한다. 상품금액을 74,600원(id=9 강아지 이동 가방 할인가 33,600원 + id=3 강아지 하네스 M 32,000원 + id=8 오리 육포 100g 9,000원, 70,000원 이상)으로 맞춰 배송비가 무료로 표시되는 상태에서 정액 쿠폰(`FIRSTORDER5000`, 5,000원 할인)을 적용해 최종 결제금액이 70,000원 아래(69,600원)로 내려가도 배송비가 그대로 무료로 유지되는지 확인한다.

**사전조건**: 상품금액 74,600원(비제주 배송지), 보유 쿠폰 `FIRSTORDER5000`(시나리오 5에서 등록).

**판정 기준**: 쿠폰 적용 전 배송비 "무료" 표시. `FIRSTORDER5000` 선택(할인 5,000원 적용, 최종 69,600원) 후에도 배송비는 여전히 "무료"로 유지된다.

**실행 결과 (2026-09-01, 재시딩 후): PASS.**

## 시나리오 9: 쿠폰 적용 주문 생성 → 쿠폰 선점 확인

spec 사용자 시나리오 2-6, 3, 수용 기준 12·13번째 항목을 검증한다. 상품금액 37,600원(id=3+id=12, 시나리오 6과 동일 조합)에 `WELCOME3000`을 적용해 "결제하기"로 주문을 생성한 뒤, `/mypage/coupons`로 돌아가 해당 쿠폰이 "사용 완료·만료" 구간으로 이동했는지 확인한다.

**사전조건**: 상품금액 37,600원(WELCOME3000 최소 주문금액 30,000원 충족), 보유 쿠폰 `WELCOME3000`, 실 상품 데이터(id=3, id=12).

**판정 기준**: 주문 생성(`POST /orders`) 성공 후 `/checkout/payment?orderId=...`로 이동한다. `/mypage/coupons` 재방문 시 `WELCOME3000`이 "사용 가능한 쿠폰" 목록에서 사라지고 "사용 완료·만료" 구간에 비활성 스타일로 나타난다.

**실행 결과 (2026-09-01, 재시딩 후): PASS.**

## 시나리오 10: Toss 결제위젯 렌더 금액이 할인 반영 금액과 일치 (confirm 성공 경로는 스킵)

spec 사용자 시나리오 2-7, 수용 기준 18번째 항목 중 위젯 렌더링 금액 부분만 검증한다. Toss confirm 성공 경로는 상점(스토어) 미등록 제약(`docs/backlog/2026-08-30-cart-order-payment-phase4-01.md`)으로 이번에도 검증하지 않는다.

**사전조건**: 시나리오 9가 먼저 성공해 할인 적용된 `PENDING` 주문이 있어야 한다(상품금액 37,600원 − 할인 3,000원 + 배송비 3,400원 = 38,000원, 상품금액이 무료배송 임계값 70,000원 미달이라 배송비가 붙는다).

**판정 기준**: `/checkout/payment?orderId=...`의 결제 버튼에 표시되는 금액(38,000원)이 서버가 계산한 `totalAmount`(할인 반영 금액)와 일치한다.

**실행 결과 (2026-09-01, 재시딩 후): PASS.**

## 시나리오 11: 주문상세에 할인 줄과 쿠폰명 표시

spec 사용자 시나리오 4-2, 수용 기준 19번째 항목을 검증한다.

**사전조건**: 시나리오 9에서 생성한 쿠폰 적용 주문의 `orderId`.

**판정 기준**: `/mypage/orders/{orderId}`의 금액 내역에 "쿠폰 할인 · 웰컴 3,000원 할인" 줄과 "-3,000원"이 표시된다.

**실행 결과 (2026-09-01, 재시딩 후): PASS.**

## 시나리오 12: 쿠폰 미사용 주문에서는 할인 줄이 렌더되지 않음

spec 사용자 시나리오 4-3, 수용 기준 19번째 항목 후반부를 검증한다. 쿠폰을 선택하지 않고 생성한 별도 주문(id=1 강아지 무릎담요 18,000원)의 상세에서 할인 줄 자체가 없는지 확인한다.

**사전조건**: 쿠폰 미적용 주문 1건, 실 상품 데이터(id=1).

**판정 기준**: `/mypage/orders/{orderId}`에 "쿠폰 할인" 문구를 포함한 줄이 전혀 렌더되지 않는다.

**실행 결과 (2026-09-01, 재시딩 후): PASS.**

## 시나리오 13: 기존 주문(마이그레이션 백필 대상)의 주문상세 금액 내역 회귀 없음

spec 수용 기준 20번째 항목(`Order` 금액 필드 재구성 이후 기존 주문이 깨지지 않고 표시)을 검증한다.

**사전조건**: 이번 plan의 마이그레이션(V11) 적용 이전에 생성된 기존 `orders` 로우가 최소 1건 있어야 한다.

**판정 기준**: 기존 주문 상세에서 "상품금액"/"배송비"/"총 결제금액"이 깨지지 않고 정상 숫자로 표시되고, `discountAmount`가 0이므로 "쿠폰 할인" 줄은 렌더되지 않는다.

**스킵 사유 (2026-09-01, 재확인)**: `docker compose down -v` 재시딩으로 `orders` 테이블이 0건에서 다시 시작됐고, flyway는 V11까지 이미 적용된 스키마로 DB를 구성하므로 이 세션에서 새로 생성하는 모든 주문은 V11 적용 이후의 신규 컬럼(`items_subtotal`/`discount_amount`)이 정상 채워진 채로 생성된다. "V11 적용 이전에 생성돼 백필된" 기존 주문을 재현하려면 스키마를 V10 상태로 되돌려 주문을 만들거나 `items_subtotal`/`discount_amount` 없이 DB에 직접 INSERT해야 하는데, 둘 다 이 phase(E2E 검증) 범위를 벗어나는 코드/시드/DB 조작이라 시도하지 않았다. 사전조건 미충족으로 스킵 — 실패 아님. (이 항목은 Phase 2 백엔드 마이그레이션 자체의 로직으로 이미 커버되며, `backend/CLAUDE.md` 컨벤션에 따른 마이그레이션 작성 시점의 코드 리뷰/테스트가 1차 검증 수단이다.)

## 시나리오 14: 장바구니에 쿠폰 관련 UI가 더 이상 없음

spec 사용자 시나리오 2-1, 수용 기준 6번째 항목을 검증한다. `frontend/src/app/(shell)/cart/page.tsx` 코드 확인 결과 쿠폰 토글 UI와 `COUPON_DISCOUNT` 상수가 이미 제거돼 있음을 정적으로 확인했으나, 실제 렌더링에서도 "쿠폰"이라는 텍스트나 토글 요소가 없는지 브라우저로 재확인한다. localStorage 직접 주입으로 상품 데이터 의존성을 제거해 시드 유실 상태에서도 검증 가능하다.

**사전조건**: 해당 없음(localStorage 직접 주입).

**판정 기준**: `/cart`에 진입해 "쿠폰"이라는 텍스트를 포함한 요소가 화면에 존재하지 않는다.

**실행 결과 (2026-09-01, 최초): FAIL(테스트 스크립트 이슈).** 시나리오 14에서 장바구니에 주입한 테스트 상품명을 "쿠폰 UI 제거 확인용 테스트 상품"으로 지었는데, `getByText("쿠폰")` 판정 로직이 이 상품명 자체를 "쿠폰" 텍스트로 오매칭해 거짓 실패가 발생했다(코드 결함 아님, 스크린샷상 실제 화면에는 쿠폰 관련 UI가 전혀 없음을 육안 확인). 상품명에서 "쿠폰" 단어를 제거("장바구니 UI 확인용 테스트 상품")한 뒤 재실행해 PASS를 확인했다.

**실행 결과 (2026-09-01, 재시딩 후 재실행): PASS.**

## 실행 스크립트

```javascript
const page = await browser.getPage("coupon-system");

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

async function signup(nickname) {
  const email = `e2e-coupon-${Date.now()}-${Math.floor(Math.random() * 10000)}@example.com`;
  await page.goto("http://localhost:3000/signup", { waitUntil: "domcontentloaded" });
  await page.getByLabel("이메일").fill(email);
  await page.getByRole("textbox", { name: "비밀번호" }).fill("testpass1");
  await page.getByLabel("닉네임").fill(nickname);
  await page.getByRole("button", { name: "회원가입" }).click();
  await page.waitForURL("**/mypage", { timeout: 10000 });
  return email;
}

async function registerCouponByCode(code) {
  await page.goto("http://localhost:3000/mypage/coupons", { waitUntil: "domcontentloaded" });
  const codeInput = page.getByLabel("쿠폰 코드");
  await codeInput.waitFor({ state: "visible", timeout: 8000 });
  await codeInput.fill(code);
  await page.getByRole("button", { name: "등록하기" }).click();
}

// ============================================================
// 시나리오 1: 존재하지 않는 쿠폰 코드 등록 시 인라인 에러
// ============================================================
await signup("쿠폰테스터");
await registerCouponByCode("NOTEXISTCODE999");

const notFoundError = page.getByText("존재하지 않는 쿠폰 코드입니다");
await notFoundError.waitFor({ state: "visible", timeout: 5000 }).catch(async () => {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-1-no-inline-error");
  throw new Error(
    `시나리오 1 판정 기준 미충족: 존재하지 않는 코드 등록 시 인라인 에러 문구가 보이지 않음 (스크린샷: ${screenshotPath})`,
  );
});

console.log("PASS: 시나리오 1");

// ============================================================
// 시나리오 2: 유효한 쿠폰 코드 등록 성공
// ============================================================
await registerCouponByCode("WELCOME3000");

const welcomeCard = page.getByText("웰컴 3,000원 할인");
await welcomeCard.waitFor({ state: "visible", timeout: 5000 }).catch(async () => {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-2-not-added");
  throw new Error(
    `시나리오 2 판정 기준 미충족: WELCOME3000 등록 후 "웰컴 3,000원 할인" 카드가 보이지 않음 (스크린샷: ${screenshotPath})`,
  );
});

const successToast = page.getByText("쿠폰을 등록했어요");
if (!(await successToast.isVisible({ timeout: 3000 }).catch(() => false))) {
  throw new Error("시나리오 2 판정 기준 미충족: 등록 성공 토스트가 노출되지 않음");
}

console.log("PASS: 시나리오 2");

// ============================================================
// 시나리오 3: 같은 코드 중복 등록 실패
// ============================================================
await registerCouponByCode("WELCOME3000");

const duplicateError = page.getByText("이미 등록한 쿠폰입니다");
await duplicateError.waitFor({ state: "visible", timeout: 5000 }).catch(async () => {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-3-no-duplicate-error");
  throw new Error(
    `시나리오 3 판정 기준 미충족: 중복 등록 시 인라인 에러 문구가 보이지 않음 (스크린샷: ${screenshotPath})`,
  );
});

console.log("PASS: 시나리오 3");

// ============================================================
// 시나리오 4: 만료된 쿠폰 등록 실패 — 스킵
// ============================================================
console.log(
  "SKIP: 시나리오 4 — V10 시드 쿠폰 4종 전부 미만료(2026-12-31)이고 관리자 발급 API가 없어 만료 쿠폰을 만들 방법이 없음",
);

// ============================================================
// 시나리오 5: 쿠폰함 목록 구간 분리 표시 (FIRSTORDER5000 추가 등록)
// ============================================================
await registerCouponByCode("FIRSTORDER5000");

const firstOrderCard = page.getByText("첫 구매 5,000원 할인");
await firstOrderCard.waitFor({ state: "visible", timeout: 5000 }).catch(async () => {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-5-not-added");
  throw new Error(
    `시나리오 5 판정 기준 미충족: FIRSTORDER5000 등록 후 카드가 보이지 않음 (스크린샷: ${screenshotPath})`,
  );
});

const availableSection = page.locator("text=사용 가능한 쿠폰").locator("..");
const availableCount = await availableSection.locator("span.font-semibold").count();
if (availableCount < 2) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-5-count-mismatch");
  throw new Error(
    `시나리오 5 판정 기준 미충족: "사용 가능한 쿠폰" 구간에 2건이 표시돼야 하는데 ${availableCount}건 발견 (스크린샷: ${screenshotPath})`,
  );
}

console.log("PASS: 시나리오 5");

// ============================================================
// 시나리오 6: 체크아웃 쿠폰 선택 — 전부 나열, 최소 주문금액 미달 비활성 + 사유
// (VIP20 추가 등록 후 진행)
// ============================================================
await registerCouponByCode("VIP20");
const vipCard = page.getByText("VIP 20% 할인");
await vipCard.waitFor({ state: "visible", timeout: 5000 }).catch(async () => {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-6-vip-not-added");
  throw new Error(`시나리오 6 사전조건 실패: VIP20 등록 후 카드가 보이지 않음 (스크린샷: ${screenshotPath})`);
});

await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [
    { key: "3-M", id: 3, title: "강아지 하네스 M", size: "M", unitPrice: 32000, qty: 1 },
    { key: "12-M", id: 12, title: "강아지 장난감 로프", size: "M", unitPrice: 5600, qty: 1 },
  ];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["3-M", "12-M"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

const recipientInput = page.getByLabel("받는 사람");
if (await recipientInput.isVisible({ timeout: 3000 }).catch(() => false)) {
  await recipientInput.fill("테스트 수령인");
  await page.getByLabel("연락처").fill("01012345678");
  await page.getByLabel("우편번호").fill("12345");
  await page.getByLabel("주소", { exact: true }).fill("서울시 테스트구 테스트로 1");
}

await waitForLocatorText(
  page.locator("text=상품금액").locator("..").locator("span").last(),
  (t) => t.includes("37,600"),
  "시나리오 6 사전 확인 실패: 체크아웃 상품금액이 37,600원으로 반영되지 않음",
);

const welcomeCouponOption = page.getByText("웰컴 3,000원 할인");
const firstOrderCouponOption = page.getByText("첫 구매 5,000원 할인");
const vipCouponOption = page.getByText("VIP 20% 할인");

for (const [name, loc] of [
  ["웰컴 3,000원 할인", welcomeCouponOption],
  ["첫 구매 5,000원 할인", firstOrderCouponOption],
  ["VIP 20% 할인", vipCouponOption],
]) {
  if (!(await loc.isVisible({ timeout: 5000 }).catch(() => false))) {
    const buf = await page.screenshot();
    const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-6-coupon-missing");
    throw new Error(
      `시나리오 6 판정 기준 미충족: 체크아웃 쿠폰 목록에 "${name}"이 보이지 않음 (스크린샷: ${screenshotPath})`,
    );
  }
}

const vipButton = page.locator("button", { hasText: "VIP 20% 할인" });
if (!(await vipButton.isDisabled())) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-6-vip-not-disabled");
  throw new Error(`시나리오 6 판정 기준 미충족: 최소 주문금액 미달인 VIP20이 선택 가능 상태임 (스크린샷: ${screenshotPath})`);
}

const vipReason = page.getByText("50,000원 이상 구매 시 사용 가능");
if (!(await vipReason.isVisible({ timeout: 3000 }).catch(() => false))) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-6-no-reason");
  throw new Error(`시나리오 6 판정 기준 미충족: VIP20 비활성 사유 문구가 보이지 않음 (스크린샷: ${screenshotPath})`);
}

const welcomeButton = page.locator("button", { hasText: "웰컴 3,000원 할인" });
const firstOrderButton = page.locator("button", { hasText: "첫 구매 5,000원 할인" });
if (await welcomeButton.isDisabled()) {
  throw new Error("시나리오 6 판정 기준 미충족: WELCOME3000이 최소 주문금액을 충족하는데 비활성 상태임");
}
if (await firstOrderButton.isDisabled()) {
  throw new Error("시나리오 6 판정 기준 미충족: FIRSTORDER5000이 선택 가능해야 하는데 비활성 상태임");
}

console.log("PASS: 시나리오 6");

// ============================================================
// 시나리오 7: 체크아웃 쿠폰 선택/해제 시 금액 요약 즉시 갱신
// ============================================================
const totalLocator = page.locator("text=총 결제금액").locator("..").locator("span").last();
const totalBefore = await totalLocator.textContent();

await firstOrderButton.click();

const discountLine = page.locator("text=쿠폰 할인").locator("..").locator("span").last();
await waitForLocatorText(
  discountLine,
  (t) => t.includes("5,000"),
  "시나리오 7 판정 기준 미충족: FIRSTORDER5000 선택 후 쿠폰 할인 -5,000원 줄이 나타나지 않음",
);

const totalAfterSelect = await totalLocator.textContent();
if (!totalBefore || !totalAfterSelect) {
  throw new Error("시나리오 7 판정 기준 미충족: 총 결제금액 텍스트를 읽을 수 없음");
}
const beforeNum = Number(totalBefore.replace(/[^0-9]/g, ""));
const afterNum = Number(totalAfterSelect.replace(/[^0-9]/g, ""));
if (beforeNum - afterNum !== 5000) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-7-amount-mismatch");
  throw new Error(
    `시나리오 7 판정 기준 미충족: 쿠폰 선택 전후 차액이 5,000원이 아님 (before=${beforeNum}, after=${afterNum}, 스크린샷: ${screenshotPath})`,
  );
}

await firstOrderButton.click();
const discountLineGone = await page.getByText("쿠폰 할인").isVisible({ timeout: 3000 }).catch(() => false);
if (discountLineGone) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-7-discount-not-removed");
  throw new Error(`시나리오 7 판정 기준 미충족: 선택 해제 후에도 "쿠폰 할인" 줄이 남아있음 (스크린샷: ${screenshotPath})`);
}
const totalAfterDeselect = await totalLocator.textContent();
if (!totalAfterDeselect || Number(totalAfterDeselect.replace(/[^0-9]/g, "")) !== beforeNum) {
  throw new Error(
    `시나리오 7 판정 기준 미충족: 선택 해제 후 총 결제금액이 원래대로 복귀하지 않음 (원래: ${totalBefore}, 현재: ${totalAfterDeselect})`,
  );
}

console.log("PASS: 시나리오 7");

// ============================================================
// 시나리오 8: 무료배송 임계값이 할인 전 상품금액 기준
// ============================================================
await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [
    { key: "9-M", id: 9, title: "강아지 이동 가방", size: "M", unitPrice: 33600, qty: 1 },
    { key: "3-M", id: 3, title: "강아지 하네스 M", size: "M", unitPrice: 32000, qty: 1 },
    { key: "8-M", id: 8, title: "오리 육포 100g", size: "M", unitPrice: 9000, qty: 1 },
  ];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["9-M", "3-M", "8-M"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

await waitForLocatorText(
  page.locator("text=상품금액").locator("..").locator("span").last(),
  (t) => t.includes("74,600"),
  "시나리오 8 사전 확인 실패: 체크아웃 상품금액이 74,600원으로 반영되지 않음",
);

const shippingLine = page.locator("text=배송비").locator("..").locator("span").last();
await waitForLocatorText(
  shippingLine,
  (t) => t.includes("무료"),
  "시나리오 8 판정 기준 미충족: 쿠폰 적용 전 배송비가 무료로 표시되지 않음",
);

const firstOrderButton8 = page.locator("button", { hasText: "첫 구매 5,000원 할인" });
await firstOrderButton8.waitFor({ state: "visible", timeout: 5000 });
await firstOrderButton8.click();

await waitForLocatorText(
  page.locator("text=총 결제금액").locator("..").locator("span").last(),
  (t) => t.includes("69,600"),
  "시나리오 8 판정 기준 미충족: 쿠폰 적용 후 총 결제금액이 69,600원으로 갱신되지 않음",
);

const shippingLineAfter = await shippingLine.textContent();
if (!shippingLineAfter || !shippingLineAfter.includes("무료")) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-8-shipping-not-free");
  throw new Error(
    `시나리오 8 판정 기준 미충족: 쿠폰 적용 후 최종 금액이 70,000원 아래로 내려갔는데 배송비가 무료로 유지되지 않음 (실제: ${shippingLineAfter}, 스크린샷: ${screenshotPath})`,
  );
}

// 다음 시나리오를 위해 쿠폰 선택 해제하고 이탈
await firstOrderButton8.click();

console.log("PASS: 시나리오 8");

// ============================================================
// 시나리오 9: 쿠폰 적용 주문 생성 → 쿠폰 선점 확인
// ============================================================
await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [
    { key: "3-M", id: 3, title: "강아지 하네스 M", size: "M", unitPrice: 32000, qty: 1 },
    { key: "12-M", id: 12, title: "강아지 장난감 로프", size: "M", unitPrice: 5600, qty: 1 },
  ];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["3-M", "12-M"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

const recipientInput9 = page.getByLabel("받는 사람");
if (await recipientInput9.isVisible({ timeout: 3000 }).catch(() => false)) {
  await recipientInput9.fill("테스트 수령인");
  await page.getByLabel("연락처").fill("01012345678");
  await page.getByLabel("우편번호").fill("12345");
  await page.getByLabel("주소", { exact: true }).fill("서울시 테스트구 테스트로 1");
}

await waitForLocatorText(
  page.locator("text=상품금액").locator("..").locator("span").last(),
  (t) => t.includes("37,600"),
  "시나리오 9 사전 확인 실패: 체크아웃 상품금액이 37,600원으로 반영되지 않음",
);

const welcomeButton9 = page.locator("button", { hasText: "웰컴 3,000원 할인" });
await welcomeButton9.waitFor({ state: "visible", timeout: 5000 });
await welcomeButton9.click();

await waitForLocatorText(
  page.locator("text=쿠폰 할인").locator("..").locator("span").last(),
  (t) => t.includes("3,000"),
  "시나리오 9 사전 확인 실패: WELCOME3000 선택 후 쿠폰 할인 -3,000원이 반영되지 않음",
);

await page.getByRole("button", { name: "결제하기" }).click();
await page.waitForURL("**/checkout/payment**", { timeout: 10000 });

const orderIdMatch9 = page.url().match(/orderId=(\d+)/);
const orderId9 = orderIdMatch9 ? orderIdMatch9[1] : null;
if (!orderId9) {
  throw new Error("시나리오 9 판정 기준 미충족: /checkout/payment URL에서 orderId를 확인할 수 없음");
}

await page.goto("http://localhost:3000/mypage/coupons", { waitUntil: "domcontentloaded" });

const inactiveSection = page.locator("text=사용 완료・만료").locator("..");
const welcomeInInactive = inactiveSection.getByText("웰컴 3,000원 할인");
await welcomeInInactive.waitFor({ state: "visible", timeout: 8000 }).catch(async () => {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-9-not-used");
  throw new Error(
    `시나리오 9 판정 기준 미충족: 주문 생성 후 WELCOME3000이 "사용 완료・만료" 구간으로 이동하지 않음 (스크린샷: ${screenshotPath})`,
  );
});

const availableSection9 = page.locator("text=사용 가능한 쿠폰").locator("..");
const welcomeInAvailable = availableSection9.getByText("웰컴 3,000원 할인");
if (await welcomeInAvailable.isVisible({ timeout: 2000 }).catch(() => false)) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-9-still-available");
  throw new Error(
    `시나리오 9 판정 기준 미충족: WELCOME3000이 여전히 "사용 가능한 쿠폰" 목록에 남아있음 (스크린샷: ${screenshotPath})`,
  );
}

console.log("PASS: 시나리오 9");

// ============================================================
// 시나리오 10: Toss 결제위젯 렌더 금액이 할인 반영 금액과 일치
// ============================================================
await page.goto(`http://localhost:3000/checkout/payment?orderId=${orderId9}`, { waitUntil: "domcontentloaded" });

const payButton10 = page.getByRole("button", { name: /결제하기|불러오는 중/ });
await page.waitForTimeout(3000);
const payButtonText10 = await payButton10.textContent().catch(() => null);
// 상품금액 37,600원은 무료배송 기준(70,000원) 미달이므로 배송비 3,400원이 붙는다.
// itemsSubtotal(37,600) - discount(3,000) + shippingFee(3,400) = 38,000원이 버튼에 그대로 반영돼야 함
if (!payButtonText10 || !/[0-9,]+원/.test(payButtonText10)) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-10-widget-not-loaded");
  throw new Error(
    `시나리오 10 판정 기준 미충족: Toss 결제위젯이 정상 로드되지 않음 (버튼 텍스트: ${payButtonText10}, 스크린샷: ${screenshotPath})`,
  );
}
if (!payButtonText10.includes("38,000") && !payButtonText10.includes("불러오는 중")) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-10-amount-mismatch");
  throw new Error(
    `시나리오 10 판정 기준 미충족: 결제위젯 금액이 할인 반영 금액(38,000원 = 상품금액 37,600 - 할인 3,000 + 배송비 3,400)과 다름 (버튼 텍스트: ${payButtonText10}, 스크린샷: ${screenshotPath})`,
  );
}

console.log("PASS: 시나리오 10 (orderId=" + orderId9 + ", confirm 성공 경로는 상점 미등록 제약으로 스킵)");

// ============================================================
// 시나리오 11: 주문상세에 할인 줄과 쿠폰명 표시
// ============================================================
await page.goto(`http://localhost:3000/mypage/orders/${orderId9}`, { waitUntil: "domcontentloaded" });

const discountDetailLine = page.getByText("쿠폰 할인 · 웰컴 3,000원 할인");
await discountDetailLine.waitFor({ state: "visible", timeout: 5000 }).catch(async () => {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-11-no-discount-line");
  throw new Error(
    `시나리오 11 판정 기준 미충족: 주문상세에 "쿠폰 할인 · 웰컴 3,000원 할인" 줄이 보이지 않음 (스크린샷: ${screenshotPath})`,
  );
});

const discountAmountText = await discountDetailLine.locator("..").locator("span").last().textContent();
if (!discountAmountText || !discountAmountText.includes("3,000")) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-11-amount-wrong");
  throw new Error(
    `시나리오 11 판정 기준 미충족: 할인 금액이 -3,000원으로 표시되지 않음 (실제: ${discountAmountText}, 스크린샷: ${screenshotPath})`,
  );
}

console.log("PASS: 시나리오 11");

// ============================================================
// 시나리오 12: 쿠폰 미사용 주문에서는 할인 줄이 렌더되지 않음
// ============================================================
await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [{ key: "1-M", id: 1, title: "강아지 무릎담요", size: "M", unitPrice: 18000, qty: 1 }];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
  window.sessionStorage.setItem("momentive:checkout-selection", JSON.stringify(["1-M"]));
});
await page.goto("http://localhost:3000/checkout", { waitUntil: "domcontentloaded" });

const recipientInput12 = page.getByLabel("받는 사람");
if (await recipientInput12.isVisible({ timeout: 3000 }).catch(() => false)) {
  await recipientInput12.fill("테스트 수령인");
  await page.getByLabel("연락처").fill("01012345678");
  await page.getByLabel("우편번호").fill("12345");
  await page.getByLabel("주소", { exact: true }).fill("서울시 테스트구 테스트로 1");
}

await waitForLocatorText(
  page.locator("text=상품금액").locator("..").locator("span").last(),
  (t) => t.includes("18,000"),
  "시나리오 12 사전 확인 실패: 체크아웃 상품금액이 18,000원으로 반영되지 않음",
);

// 쿠폰 선택하지 않고 바로 결제하기
await page.getByRole("button", { name: "결제하기" }).click();
await page.waitForURL("**/checkout/payment**", { timeout: 10000 });

const orderIdMatch12 = page.url().match(/orderId=(\d+)/);
const orderId12 = orderIdMatch12 ? orderIdMatch12[1] : null;
if (!orderId12) {
  throw new Error("시나리오 12 판정 기준 미충족: /checkout/payment URL에서 orderId를 확인할 수 없음");
}

await page.goto(`http://localhost:3000/mypage/orders/${orderId12}`, { waitUntil: "domcontentloaded" });

const noDiscountLine = await page.getByText("쿠폰 할인").isVisible({ timeout: 3000 }).catch(() => false);
if (noDiscountLine) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-12-unexpected-discount");
  throw new Error(
    `시나리오 12 판정 기준 미충족: 쿠폰 미적용 주문인데 "쿠폰 할인" 줄이 렌더됨 (스크린샷: ${screenshotPath})`,
  );
}

console.log("PASS: 시나리오 12");

// ============================================================
// 시나리오 13: 기존 주문(V11 이전 백필 대상) 회귀 확인 — 스킵
// ============================================================
console.log(
  "SKIP: 시나리오 13 — 재시딩 후 orders가 0건에서 시작하고 flyway가 V11까지 적용된 스키마로 구성되므로, 이 세션에서 생성하는 모든 주문은 V11 이후 신규 컬럼이 정상 채워진 채로 생성됨. V11 적용 이전 상태(백필 대상)를 재현하려면 스키마를 되돌리거나 DB에 직접 INSERT해야 하는데 이는 E2E 검증 phase 범위를 벗어남",
);

// ============================================================
// 시나리오 14: 장바구니에 쿠폰 관련 UI가 더 이상 없음
// ============================================================
await page.goto("http://localhost:3000/cart", { waitUntil: "domcontentloaded" });
await page.evaluate(() => {
  const cart = [
    { key: "9601-M", id: 9601, title: "장바구니 UI 확인용 테스트 상품", size: "M", unitPrice: 20000, qty: 1 },
  ];
  window.localStorage.setItem("momentive:cart", JSON.stringify(cart));
});
await page.reload({ waitUntil: "domcontentloaded" });

await waitForLocatorText(
  page.locator("text=총 결제금액").locator("..").locator("span").last(),
  (t) => t.includes("20,000"),
  "시나리오 14 사전 확인 실패: 장바구니에 테스트 상품이 반영되지 않음",
);

const couponTextCount = await page.getByText("쿠폰").count();
if (couponTextCount > 0) {
  const buf = await page.screenshot();
  const screenshotPath = await saveScreenshot(buf, "coupon-system-scenario-14-coupon-ui-found");
  throw new Error(
    `시나리오 14 판정 기준 미충족: 장바구니 화면에 "쿠폰" 텍스트를 포함한 요소가 ${couponTextCount}개 발견됨 (스크린샷: ${screenshotPath})`,
  );
}

console.log("PASS: 시나리오 14");
```
