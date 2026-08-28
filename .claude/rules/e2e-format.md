# E2E 케이스 작성 규격

`plan`의 마지막 코드 phase가 통과된 뒤, `e2e-tester` 에이전트가 유저 플로우 관점의 통합테스트 시나리오를 기록하는 문서의 규격이다. `docs/specs/`, `docs/plans/`와 명명 관례를 통일하고, 시나리오 설명(사람이 읽는 문서)과 실행 스크립트([dev-browser](https://github.com/sawyerhood/dev-browser)에 넘길 JS)를 한 파일에 함께 담는다.

## 파일 위치 및 이름

```
docs/e2e/YYYY-MM-DD-<feature-slug>.md
```

- `YYYY-MM-DD`: e2e-tester가 케이스를 생성한 날짜
- `<feature-slug>`: 기반 spec/plan과 동일한 slug
- 같은 feature를 나중에 다시 검증해 시나리오가 늘거나 바뀌면, 기존 파일은 수정하지 않고 새 날짜로 새 파일을 추가한다 (spec과 동일한 이력 누적 방식). spec의 `supersedes`와 달리 이전 파일이 "대체"되는 게 아니라 회귀 대상으로 계속 유효하므로, `supersedes` 필드는 두지 않는다.

## Frontmatter

```yaml
---
date: YYYY-MM-DD
feature: <feature-slug>
spec: <근거 spec 파일명>
plan: <근거 plan 파일명>
---
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `date` | string (`YYYY-MM-DD`) | 필수 | 생성된 날짜, 파일명의 날짜와 동일 |
| `feature` | string (kebab-case) | 필수 | 파일명의 slug와 동일. **plan의 frontmatter `feature:`를 그대로 쓴다** (plan이 참조하는 spec의 `feature:`와 다를 수 있음 — plan-runner/reviewer가 호출 시 쓰는 `feature-slug`와 일치시켜야 `docs/e2e/*-<feature-slug>.md` 검색이 맞물린다) |
| `spec` | string (파일명) | 필수 | 케이스 도출 근거가 된 spec 파일명 |
| `plan` | string (파일명) | 필수 | 이 케이스를 요구한 plan 파일명 |

## 본문 섹션

파일 상단에 `# <기능명> E2E 케이스` 제목을 두고, 그 아래 시나리오별로 `## 시나리오 N: <이름>` 섹션을 반복한다.

```markdown
## 시나리오 N: <시나리오 이름>

<spec 사용자 시나리오/AC의 어느 부분에서 도출했는지, 무엇을 검증하는지 한두 문장>

**사전조건**: <이 시나리오 실행 전 필요한 상태 — 예: 특정 상품이 DB에 있어야 함, 로그인 상태 등. 없으면 "해당 없음">

**판정 기준**: <무엇을 만족하면 pass인지 — 화면 텍스트, 라우팅 경로, 요소 노출 여부 등을 구체적으로>

\`\`\`javascript
const page = await browser.getPage("<feature-slug>-scenario-N");
await page.goto("http://localhost:3000/...", { waitUntil: "domcontentloaded" });
// ... 시나리오 진행 스텝
// 실패 시 반드시 throw로 종료해 종료 코드에 반영되게 한다
if (!(await page.locator("...").isVisible())) {
  throw new Error("판정 기준 미충족: ...");
}
console.log("PASS: 시나리오 N");
\`\`\`
```

- 스크립트는 dev-browser CLI(`dev-browser <<'EOF' ... EOF`)에 그대로 붙여넣을 수 있는 완결된 형태로 작성한다. 기본값은 headed 모드(브라우저 창이 화면에 보임) — 사용자가 진행 상황을 육안으로 확인해 수동 테스트/디버깅 부담을 줄이는 것이 이 도구의 목적이기 때문이다.
- 각 시나리오는 `browser.getPage(name)`에 서로 다른 `name`을 써서 페이지를 분리한다 — 시나리오 간 상태(쿠키, localStorage, 라우팅 히스토리)가 섞이지 않게 하기 위함이다.
- 판정은 스크립트 스스로 `throw`로 실패를 알린다. dev-browser 프로세스가 에러 없이 끝나고 `console.log`로 `PASS: ...`가 출력되면 성공으로 본다.
- 실패 시 스크린샷은 `saveScreenshot(buf, name)`으로 저장한다 — dev-browser의 파일 I/O는 `~/.dev-browser/tmp/`로 제한되므로, 저장된 실제 경로는 스크립트 실행 결과(stdout)에서 확인하고 e2e-tester가 이를 읽어 backlog 문서에 인용한다.
- **셀렉터**: 프론트엔드 코드에 `data-testid`가 없는 한(기본값으로 존재를 가정하지 않는다), Playwright의 role/text 기반 로케이터(`getByRole`, `getByText`, `getByPlaceholder` 등 spec의 화면 요구사항에 실제로 등장하는 문자열/역할)를 우선 사용한다. 케이스 생성 시 대상 컴포넌트 파일을 Read/Grep으로 확인해 실제 마크업(태그, aria 속성, 표시 텍스트)에 맞는 로케이터를 쓴다 — 추측이나 존재하지 않는 속성으로 셀렉터를 지어내지 않는다. role/text로 안정적으로 특정할 수 없는 요소(예: 반복 렌더링되는 카드 컨테이너처럼 텍스트가 매번 달라지는 경우)는 실제 컴포넌트의 className을 근거로 한 CSS 셀렉터를 fallback으로 쓰되, 시나리오 설명에 "CSS 셀렉터 fallback 사용, 컴포넌트 구조 변경 시 갱신 필요"라고 남긴다.
- **데이터 사전조건 충돌**: 한 케이스 문서 안에서 시나리오마다 요구하는 DB 상태가 서로 다르면(예: "상품 1개 이상" vs "상품 0개"), 상태를 공유하는 로컬 서버에서 동시에 둘 다 만족시킬 수 없다. 이런 시나리오는 한 파일에 같이 두더라도 실행 순서와 필요한 사전 조작(시드 삽입/삭제, 또는 실행 전 수동 준비)을 **사전조건**란에 명시하고, e2e-tester가 실행 직전 그 상태를 실제로 만족시킬 방법이 없으면(예: 시드 스크립트가 없어 DB를 비울 수 없음) 그 시나리오는 실행하지 않고 "사전조건 미충족으로 스킵"이라고 보고한다 — 실패로 잘못 판정하지 않는다.

## 케이스 도출 기준

- 1차 근거: 대상 spec의 "사용자 시나리오" 섹션 각 단계, "수용 기준(AC)" 각 항목
- 보조 참고: 실제 구현된 라우트/컴포넌트/API — spec에 없던 예외 케이스(예: 빈 장바구니, 재고 없음)가 코드에 존재하면 시나리오로 추가할 수 있다
- 이번 plan에서 새로 추가/변경된 시나리오만 대상으로 한다. 기존에 이미 다른 `docs/e2e/*.md`에 있는 시나리오를 다시 반복해서 만들지 않는다.

## 작성 주체 및 시점

`e2e-tester` 에이전트가 자신이 호출된 시점(plan의 마지막 코드 phase 통과 직후)에 대상 spec을 읽고 그 자리에서 생성한다. plan에는 미리 존재하지 않으며, planner가 만들지 않는다.

## 조회 방법

전체 회귀를 실행할 때는 `docs/e2e/*.md` 전체를 대상으로 하고, 특정 feature의 이력만 볼 때는 다음으로 훑는다.

```bash
ls docs/e2e/*-<feature-slug>.md
```

## 템플릿

```markdown
---
date: YYYY-MM-DD
feature: <feature-slug>
spec: <spec 파일명>
plan: <plan 파일명>
---

# <기능명> E2E 케이스

## 시나리오 1: <이름>

**사전조건**:

**판정 기준**:

\`\`\`javascript
\`\`\`
```
</content>
