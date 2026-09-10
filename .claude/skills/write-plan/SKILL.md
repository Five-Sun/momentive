---
name: write-plan
description: Write the phase/step implementation plan for a confirmed spec, following .claude/rules/plan-format.md. Use after a spec in docs/specs/ reaches status "confirmed" and before any implementation begins, or for a follow-up "-fix-N" plan when execution surfaces new work outside the original plan's scope.
---

플랜 작성 절차. 산출물은 오직 plan 파일 하나 — 구현 코드나 spec을 쓰지 않는다.

## 규격

`.claude/rules/plan-format.md`가 이 작업의 전체 스키마다. 작업을 시작하기 전에 반드시 그 파일을 읽고, 파일 위치/이름, frontmatter 필드, 본문 섹션, phase/step 작성 기준을 정확히 그대로 따른다. 이 지시문과 plan-format.md가 상충하면 plan-format.md가 우선한다.

## 절차

1. 대상 spec을 정한다 — 지정됐으면 그 파일, 아니면 `docs/specs/`에서 `status: confirmed`인 spec 중 이번 요청과 맞는 것을 찾는다. spec이 없거나 `confirmed` 상태가 아니면 멈추고 알린다 (grillme부터 하라고 안내).
2. spec 전체를 읽는다.
3. `docs/backlog/`를 훑어 같은 `feature` 또는 관련 `category`의 과거 실패 항목이 있는지 확인한다 (`.claude/rules/backlog-format.md`의 "조회 방법" 절차). 있으면 phase 설계에 반영한다.
4. `docs/plans/`에서 같은 feature-slug의 기존 plan(원본 또는 `-fix-N`)이 있는지 확인한다.
   - 없으면 신규 plan.
   - 있고 아직 실행 전/진행 중이면 그 사실을 알리고 어떻게 할지 확인한다.
   - 있고 실행이 끝난 뒤 추가 작업이 필요한 상황이면 `-fix-N` 수정 계획으로 분리한다 (원본 plan은 건드리지 않는다).
5. Phase/Step을 설계한다. 각 phase는 그 자체로 독립적으로 검증 가능한 마일스톤이어야 하고, 각 step은 완료 시 확인 가능한 산출물 단위로 쓴다 (파일 단위로 억지로 쪼개지 않는다).
6. **파일을 바로 쓰지 않고**, phase 목록과 각 phase의 핵심 내용을 요약해 먼저 제시하고 컨펌을 받는다.
7. 컨펌 후 `.claude/rules/plan-format.md`의 frontmatter/템플릿 그대로 `docs/plans/YYYY-MM-DD-<feature-slug>.md`(또는 `-fix-N.md`)를 작성한다. `status`는 항상 `planned`로 시작한다.
