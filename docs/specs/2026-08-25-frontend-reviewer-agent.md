---
date: 2026-08-25
feature: frontend-reviewer-agent
status: confirmed
---

# frontend-reviewer 에이전트

## 목적 (Why)

혼자 개발 + AI 에이전트 팀 체제에서, 매 phase 구현이 끝날 때마다 사용자가 직접 코드를 전부 눈으로 검토하지 않아도 "올바른 시야"(spec의 수용기준, plan의 step, 프론트엔드 컨벤션)로 검증해주는 존재가 필요하다. `.claude/agents/planner.md`만 만들어두고 developer/reviewer 쌍은 "실제 plan이 필요로 할 때" 만들기로 미뤄뒀는데, 현재 진행 중인 `app-shell` plan의 Phase 2가 그 시점이다. 이번 spec은 그중 프론트엔드 리뷰어를 먼저 다룬다.

## 범위 (Scope)

### In Scope
- `frontend/.claude/agents/frontend-reviewer.md` 작성
- Phase 단위 리뷰 절차: plan/spec 조회 → 코드로 검증 가능한 step 식별 → 정적 리뷰(컨벤션/ESLint/타입/로직/correctness/simplification/efficiency) → `npm run build`/`npm run lint` 직접 실행
- 통과/실패 처리: plan 체크박스 갱신, `status: done` 갱신 조건, `docs/backlog/` 규격에 따른 실패 기록
- `ReportFindings` 툴을 통한 결과 보고
- Tools/model 확정

### Out of Scope
- 브라우저/시각/동작(E2E) 검증 — 추후 별도 QA 에이전트로 다룬다 (이번 spec에서 명시적으로 제외)
- `frontend-developer` 에이전트 (구현 담당) — 아직 착수 안 함
- `backend-reviewer`/`backend-developer` — 별도 spec
- 자동 코드 수정(auto-fix) — reviewer는 소스 코드를 직접 고치지 않는다
- 구현→리뷰 자동 연쇄 호출(오케스트레이션) — 여전히 사용자가 수동으로 호출

## 사용자 시나리오

1. 사용자가 plan의 한 phase 구현을 마친 뒤 "frontend-reviewer로 `<feature-slug>` Phase `<N>` 검증해줘"라고 호출한다.
2. 에이전트가 `docs/plans/`에서 해당 feature-slug와 매칭되는 plan 파일을 스스로 찾는다.
   - 매칭되는 plan이 없으면 중단하고, spec/plan부터 먼저 진행하라고 안내한다.
   - 원본 plan과 `-fix-N` 수정 계획이 함께 존재해 여러 개 매칭되면 사용자에게 어떤 것인지 확인한다.
3. plan에서 Phase `<N>`을 찾는다. 존재하지 않으면 중단하고 실제 존재하는 phase 목록을 안내한다. 해당 phase의 모든 step이 이미 체크되어 있으면 재검증 의도가 맞는지 사용자에게 확인한다.
4. plan이 참조하는 spec 파일을 읽어 수용기준 맥락을 파악하고, `docs/backlog/`에서 같은 feature 또는 관련 category의 과거 실패 항목을 훑어 참고한다.
5. Phase `<N>`의 step 목록을 두 그룹으로 나눈다.
   - **코드 검증 가능**: 파일/컴포넌트가 명시된 step
   - **스코프 밖**: "수동 검증" 등 브라우저 동작/시각 확인이 필요한 step — 이 그룹은 체크박스를 건드리지 않고 목록으로만 남긴다
6. "코드 검증 가능" 그룹의 파일들을 정적으로 검토한다: `frontend/CLAUDE.md` 컨벤션, `docs/design.md` 톤/일관성, `/style-guide`에 이미 있는 컴포넌트 재사용 여부, TypeScript 타입, correctness 버그, simplification/efficiency.
7. `npm run build`, `npm run lint`을 Bash로 직접 실행해 통과 여부를 확인한다.
8. 결과를 종합한다.
   - correctness 버그 또는 build/lint 실패가 있으면 phase 실패로 처리하고, `docs/backlog/`에 `backlog-format.md` 규격대로 기록한다(`category`는 실패 원인에 따라 판단). 이 경우 plan 체크박스는 갱신하지 않는다.
   - 문제가 없으면 "코드 검증 가능" 그룹의 체크박스를 Edit으로 체크한다. simplification/efficiency 제안이 있어도 advisory로만 보고하고 phase는 통과 처리한다.
9. plan 전체(모든 phase, 스코프 밖으로 남겨둔 수동 검증 항목까지 포함)의 체크박스가 전부 체크된 상태를 확인하면 frontmatter `status`를 `done`으로 갱신한다. 아니면 그대로 둔다.
10. `ReportFindings` 툴로 검증 결과(통과/실패, 발견된 이슈, 스코프 밖으로 남긴 항목 목록)를 보고한다. 어떤 경우에도 소스 코드는 직접 수정하지 않는다.

## 인터페이스

### API

에이전트 호출을 하나의 인터페이스로 본다.

- **호출**: `Agent` 툴로 `frontend-reviewer`를 이름 지정해 호출. 프롬프트에 `feature-slug`와 `phase 번호`를 포함한다 (예: "app-shell Phase 2 검증해줘").
- **응답**: `ReportFindings` 결과(통과/실패 여부, correctness/simplification/efficiency 이슈, 스코프 밖 수동 검증 항목 목록). 부수효과로 plan 파일이 갱신되거나(체크박스, `status`) `docs/backlog/`에 새 파일이 생성될 수 있다.
- **에러 케이스**:
  - `docs/plans/`에 매칭되는 feature-slug plan이 없음 → 중단, spec/plan부터 진행하라고 안내
  - 지정한 phase 번호가 plan에 없음 → 중단, 존재하는 phase 목록 안내
  - 동일 feature-slug로 원본 + `-fix-N` plan이 여러 개 존재 → 어떤 plan인지 사용자에게 확인
  - 지정한 phase의 모든 step이 이미 체크됨 → 재검증 의도 확인 후 진행

## 수용 기준 (Acceptance Criteria)

- [ ] `frontend/.claude/agents/frontend-reviewer.md` 파일이 존재하고 frontmatter(`name`, `description`, `tools: Read, Glob, Grep, Bash, Edit, Write, ReportFindings`, `model: inherit`)를 포함한다
- [ ] "frontend-reviewer로 `<feature-slug>` Phase `<N>` 검증해줘" 형태 호출 시 `docs/plans/`에서 해당 feature-slug의 plan 파일을 스스로 찾아낸다
- [ ] Phase `<N>`의 step 중 파일/컴포넌트가 명시된 항목만 리뷰 대상으로 삼고, "수동 검증"류 항목은 리뷰 대상에서 제외해 별도로 명시한다
- [ ] 리뷰 시 `frontend/CLAUDE.md` 컨벤션, `docs/design.md`, `/style-guide` 재사용 여부, correctness, simplification/efficiency를 모두 점검한다
- [ ] `npm run build`와 `npm run lint`을 직접 실행해 결과를 반영한다
- [ ] correctness 버그 또는 build/lint 실패가 있으면 phase를 실패 처리하고 `docs/backlog/` 규격대로 기록하며, plan 체크박스를 갱신하지 않는다
- [ ] simplification/efficiency 제안만 있는 경우 phase를 통과 처리하고 제안은 advisory로만 보고한다
- [ ] 검증 통과 시 plan 파일의 해당 체크박스를 Edit으로 갱신하고, plan 전체가 모두 체크된 경우에만 `status: done`으로 갱신한다
- [ ] 리뷰 결과를 `ReportFindings` 툴로 보고한다
- [ ] 어떤 경우에도 소스 코드를 직접 수정하지 않는다
