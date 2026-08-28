---
date: 2026-08-25
feature: frontend-reviewer-agent
spec: 2026-08-25-frontend-reviewer-agent.md
status: done
---

# frontend-reviewer 에이전트 플랜

## 개요

`docs/specs/2026-08-25-frontend-reviewer-agent.md`를 기반으로, `frontend/.claude/agents/frontend-reviewer.md` 서브에이전트 정의 파일 하나를 작성한다. 산출물이 파일 하나이므로 phase를 파일 단위가 아니라 "결과가 검증 가능한 단위"로 나눈다.

1. **Phase 1**: 에이전트 정의 파일 자체를 완성한다 — frontmatter, plan/phase 탐색 절차, 리뷰 체크리스트, 통과/실패 처리 로직, 보고 형식까지 전부 작성해 spec의 수용 기준이 텍스트로 확인 가능한 상태로 만든다.
2. **Phase 2**: 작성된 에이전트를 실제로 호출해 spec의 시나리오/에러 케이스대로 동작하는지 dry-run으로 검증한다. `docs/plans/2026-08-25-app-shell.md`가 이미 `status: done`으로 전 phase가 체크되어 있어, "지정한 phase의 모든 step이 이미 체크됨 → 재검증 의도 확인" 에러 케이스를 포함해 실호출 검증에 활용하기 좋다.

`docs/backlog/`에는 참고할 과거 실패 항목이 없었고(신규 카테고리), 동일 feature-slug의 기존 plan도 없어 신규 plan으로 작성한다.

## Phase 1: `frontend/.claude/agents/frontend-reviewer.md` 작성

이 phase가 끝나면 에이전트 정의 파일이 완성되어 있고, spec의 수용 기준 중 "내용이 문서에 존재하는가"에 해당하는 항목이 전부 텍스트로 확인 가능한 상태가 된다. (`.claude/agents/planner.md`를 형식 참조로 삼음)

- [x] frontmatter 작성 — `name: frontend-reviewer`, `description`(트리거 문구: "frontend-reviewer로 `<feature-slug>` Phase `<N>` 검증해줘" 형태 포함), `tools: Read, Glob, Grep, Bash, Edit, Write, ReportFindings`, `model: inherit`
- [x] plan/phase 탐색 절차 작성 — `docs/plans/`에서 feature-slug 매칭, 원본/`-fix-N` 다중 매칭 시 사용자 확인, phase 번호 존재 확인, 4가지 에러 케이스(매칭 plan 없음 / phase 없음 / 다중 매칭 / 이미 전부 체크됨) 처리 로직
- [x] step 분류 및 리뷰 체크리스트 작성 — "코드 검증 가능"/"스코프 밖(수동 검증)" 분리 기준, 정적 리뷰 항목(`frontend/CLAUDE.md` 컨벤션, `docs/design.md` 톤, `/style-guide` 컴포넌트 재사용, TypeScript 타입, correctness, simplification/efficiency), `npm run build`/`npm run lint` 직접 실행 지시
- [x] 통과/실패 처리 로직 작성 — 실패(correctness 버그 또는 build/lint 실패) 시 `docs/backlog/` 규격대로 기록하고 plan 체크박스 미변경, 통과 시 Edit으로 체크박스 갱신 + plan 전체 완료 시에만 `status: done` 갱신
- [x] `ReportFindings` 보고 형식 및 "하지 않는 것"(소스 코드 직접 수정 금지 등) 섹션 작성

## Phase 2: 기능 검증 (dry run)

이 phase가 끝나면, 작성된 에이전트가 실제로 spec의 시나리오/에러 케이스대로 동작함을 실호출로 확인한 상태가 된다. `docs/plans/2026-08-25-app-shell.md`(이미 `done`)를 테스트 대상으로 사용한다.

- [x] 존재하지 않는 feature-slug로 호출 → "매칭 plan 없음, spec/plan부터 진행 안내" 동작 확인
- [x] `app-shell`의 존재하지 않는 phase 번호(예: Phase 99)로 호출 → "존재하는 phase 목록 안내" 동작 확인
- [x] "frontend-reviewer로 app-shell Phase 2 검증해줘" 실호출 → 모든 step이 이미 체크된 상태이므로 재검증 의도 확인 질문이 뜨는지 확인
- [x] 재검증 진행 확인 후, `GlobalBottomNav.tsx`/`(shell)/layout.tsx` 등 대상 정적 리뷰 + `npm run build`/`npm run lint` 실행 + 보고까지 한 사이클이 정상 동작하는지 확인 — 최초 dry-run에서 `ReportFindings` 툴이 커스텀 서브에이전트에 실제로 부여되지 않는 문제 발견 → `docs/specs/2026-08-26-frontend-reviewer-agent.md` + `docs/plans/2026-08-25-frontend-reviewer-agent-fix-1.md`로 대화형 구조화 요약 방식으로 수정 후 재확인 완료
- [x] dry run 종료 후 `git status`/`git diff`로 소스 코드가 전혀 수정되지 않았음을 확인 (부수효과 없음 검증)
</content>
