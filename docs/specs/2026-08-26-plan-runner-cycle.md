---
date: 2026-08-26
feature: plan-runner-cycle
status: confirmed
---

# plan-runner 자동 사이클 (구현→리뷰→E2E→수정 순환)

## 목적 (Why)

혼자 개발 + AI 에이전트 팀 체제에서, plan이 confirm된 뒤 phase별 구현 → 정적 리뷰(backend/frontend-reviewer) → 통과 시 다음 phase 진행이라는 흐름을 지금까지는 사용자가 매번 손으로 호출해왔다. 여기에 더해 유저 플로우 관점의 동적 검증(브라우저 기반 E2E)이 빠져 있어, 정적 리뷰를 통과해도 실제 화면 동작은 검증되지 않는다.

이 스펙은 두 가지를 채운다.

1. 정적 리뷰만으로는 잡을 수 없는 유저 플로우 회귀를 잡는 `e2e-tester` 에이전트를 추가한다.
2. plan의 구현→리뷰→(실패 시)수정→재검증을 자동으로 순환시키는 `plan-runner` 오케스트레이터를 추가해, 사용자는 plan을 실행 지시하고 최종 통과 결과만 확인하면 되는 흐름을 만든다.

## 범위 (Scope)

### In Scope
- `e2e-tester` 에이전트 신설: dev-browser(https://github.com/sawyerhood/dev-browser) 스크립트 기반으로 유저 플로우 시나리오를 실행하고 pass/fail을 판정
- `docs/e2e/YYYY-MM-DD-<feature-slug>.md` 케이스 문서 규격 신설 (`.claude/rules/e2e-format.md`)
- `backend-reviewer`/`frontend-reviewer`에 "마지막 코드 phase 통과 시 e2e-tester 체이닝" 조건 추가
- `plan-runner` 에이전트 신설: plan의 phase를 순회하며 구현 서브에이전트 스폰 → reviewer 호출 → 실패 시 backlog 기반 fix 서브에이전트 재스폰 → 재검증까지 자동 순환
- phase당 최대 시도 횟수 상한과 초과 시 사용자 에스컬레이션

### Out of Scope
- planner 에이전트 수정 (phase를 프론트/백엔드가 섞이지 않게 나누는 개선 등은 이번에 다루지 않음)
- e2e 전체 회귀 실행을 위한 별도 스케줄러/CI 연동 (이번 스펙은 신규 시나리오의 즉시 실행만 다룸)
- `./dev.sh` 자동 기동/종료 (사용자가 이미 띄운 로컬 서버를 전제로 함)
- 실제 배포 환경(Vercel/Railway) 대상 E2E

## 사용자 시나리오

### 시나리오 1 — plan 전체 자동 실행 (정상 경로)
1. 사용자가 spec을 confirm하고 planner로 plan을 작성한 뒤, `./dev.sh`로 로컬 서버를 띄운다.
2. 사용자가 "plan-runner로 `<feature-slug>` 실행해줘"라고 요청한다.
3. plan-runner가 plan frontmatter의 `status`가 `planned`인지 확인하고 Phase 1부터 순회를 시작한다.
4. 각 phase마다: 구현 서브에이전트를 스폰해 step을 구현시킨 뒤, step의 파일 경로를 보고 backend-reviewer/frontend-reviewer(필요 시 둘 다)를 호출한다.
5. reviewer가 pass면 다음 phase로 진행한다.
6. 마지막 코드 phase의 reviewer가 pass하면, reviewer가 plan의 다음 섹션이 `## Phase <N+1>: E2E 검증`임을 확인하고 e2e-tester를 체이닝 호출한다.
7. e2e-tester는 로컬 서버 헬스체크 후 spec 기반으로 `docs/e2e/` 케이스 문서를 생성하고 dev-browser로 실행, 결과를 판정한다.
8. 모든 phase(E2E 포함)가 pass하면 plan-runner가 사용자에게 "전체 통과" 결과를 보고한다.

### 시나리오 2 — 리뷰 실패 후 자동 수정 (수정 루프)
1. 시나리오 1의 4번 단계에서 reviewer가 fail을 반환하고 `docs/backlog/`에 실패 항목을 기록한다.
2. plan-runner는 이 backlog 파일 경로와 spec/plan 파일 경로를 프롬프트에 담아 fix 서브에이전트를 새로 스폰한다.
3. fix 서브에이전트는 backlog의 "조치" 섹션을 근거로 코드를 수정하고, backlog 파일의 "조치" 섹션에 실제 수정 내용을 덧붙인다.
4. plan-runner가 같은 phase에 대해 reviewer를 재호출한다.
5. pass하면 다음 phase로 진행. fail이면 2~4를 반복하되, 한 phase당 시도 횟수(최초 구현 1회 + fix 2회 = 총 3회)를 넘기면 중단한다.
6. 상한 초과 시 plan-runner는 지금까지 쌓인 해당 phase의 backlog 항목들을 근거로 사용자에게 "자동 루프로 해결 안 됨, 직접 개입 필요"라고 보고하고 종료한다.

### 시나리오 3 — E2E 실패 후 자동 수정
1. 시나리오 1의 7번 단계에서 e2e-tester가 특정 시나리오를 fail로 판정하고 backlog에 기록(스크린샷 경로 포함)한다.
2. plan-runner는 시나리오 2와 동일한 방식으로 fix 서브에이전트를 스폰해 수정을 지시한다.
3. 수정 후 e2e-tester를 재호출해 해당 시나리오만 재실행한다.
4. pass하면 전체 통과 보고, 상한 초과 시 에스컬레이션은 시나리오 2와 동일하다.

### 시나리오 4 — 서버 미기동
1. e2e-tester가 헬스체크 시 백엔드 `/health` 또는 프론트 3000 포트 응답이 없음을 확인한다.
2. e2e-tester는 케이스 실행을 시도하지 않고 "먼저 `./dev.sh`를 실행해달라"는 메시지와 함께 실패를 반환한다.
3. plan-runner는 이를 코드 결함이 아닌 환경 문제로 판단해 fix 루프(재구현 시도)를 돌리지 않고, 즉시 사용자에게 서버 기동을 요청하며 중단한다.

### 시나리오 5 — 이미 진행 중이거나 완료된 plan 재실행
1. 사용자가 `status: in_progress` 또는 `status: done`인 plan에 대해 plan-runner를 호출한다.
2. plan-runner는 바로 처음부터 실행하지 않고, 어느 phase부터 재개할지(또는 전체 재검증할지) 사용자에게 확인한다.

### 시나리오 6 — phase에 프론트/백엔드 파일 혼재
1. 한 phase의 step들이 `backend/`와 `frontend/` 경로에 걸쳐 있다.
2. plan-runner는 해당 phase에 대해 backend-reviewer와 frontend-reviewer를 순차로 모두 호출하고, 둘 다 pass해야 그 phase를 통과로 취급한다.

## 인터페이스

### 화면
해당 없음 (개발 도구/에이전트 워크플로우이며 최종 사용자에게 노출되는 화면이 아님)

### 데이터 모델

**`docs/e2e/YYYY-MM-DD-<feature-slug>.md`** (신규 문서 유형, 규격은 `.claude/rules/e2e-format.md`에 별도 정의)
- frontmatter: `date`, `feature`, `spec`(근거 spec 파일명)
- 본문: 시나리오별 섹션 — 시나리오 설명 + dev-browser에 전달할 JS 스크립트 코드 블록 + pass/fail 판정 기준
- 재작업 시 새 날짜로 새 파일 추가, 기존 파일은 보존(병렬 축적, `supersedes` 없음)

**plan의 E2E 검증 phase** (기존 plan-format.md 스키마 재사용, 신규 필드 없음)
- 마지막 코드 phase 다음에 `## Phase <N+1>: E2E 검증` 섹션 추가
- step은 시나리오 단위 개별 체크박스

**backlog 항목의 `category`** (기존 backlog-format.md의 enum 값 재사용)
- e2e-tester가 기록하는 실패는 `category: test`를 사용 (신규 enum 값 추가 없음)

## 수용 기준 (Acceptance Criteria)

- [ ] `.claude/rules/e2e-format.md` 규격 문서가 작성되어 있다
- [ ] `.claude/agents/e2e-tester.md` 에이전트가 존재하고, spec 기반으로 `docs/e2e/` 케이스 문서를 생성한다
- [ ] e2e-tester가 로컬 서버 헬스체크(백엔드 `/health`, 프론트 3000 포트)를 수행하고, 서버 미기동 시 실행 없이 안내 후 중단한다
- [ ] e2e-tester가 dev-browser로 시나리오를 실행하고, assertion 실패 또는 런타임 에러를 모두 실패로 판정한다
- [ ] e2e-tester가 실패 시 `docs/backlog/`에 `category: test`로 기록하고, 스크린샷을 `.dev-logs/e2e-failures/`에 저장해 경로를 backlog에 남긴다
- [ ] e2e-tester가 통과한 시나리오 step만 plan 체크박스를 갱신한다
- [ ] `backend-reviewer`/`frontend-reviewer`가 마지막 코드 phase를 pass 처리할 때, plan의 다음 섹션이 `## Phase <N+1>: E2E 검증`이면 e2e-tester를 체이닝 호출한다
- [ ] `.claude/agents/plan-runner.md` 에이전트가 존재하고, plan의 phase를 순서대로 순회하며 구현→리뷰→(실패 시 fix)→재검증 루프를 수행한다
- [ ] plan-runner가 phase에 backend/frontend 파일이 혼재하면 두 reviewer를 모두 호출해 둘 다 pass해야 통과로 취급한다
- [ ] plan-runner가 phase당 최대 3회(최초 1 + fix 2) 시도 후에도 실패하면 중단하고 backlog 근거로 사용자에게 에스컬레이션한다
- [ ] plan-runner가 `in_progress`/`done` 상태의 plan을 호출받으면 재개 지점을 사용자에게 확인한다
- [ ] plan-runner가 모든 phase(E2E 포함) 통과 시 사용자에게 최종 결과를 보고한다
</content>
