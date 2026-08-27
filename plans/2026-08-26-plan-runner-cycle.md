---
date: 2026-08-26
feature: plan-runner-cycle
spec: 2026-08-26-plan-runner-cycle.md
status: done
---

# plan-runner 자동 사이클 플랜

## 개요

spec `2026-08-26-plan-runner-cycle.md`을 기반으로, (1) e2e-tester 에이전트와 그 규격, (2) 기존 reviewer들의 e2e 체이닝 조건 추가, (3) plan-runner 오케스트레이터 순서로 만든다. e2e-tester가 먼저 있어야 reviewer 체이닝과 plan-runner가 호출할 대상이 존재하므로 이 순서로 phase를 나눴다. 각 산출물은 문서(agent/rule 정의) 작성이라 빌드/테스트가 없고, phase 종료 조건은 "문서가 규격을 만족하고 실제 시나리오로 dry run했을 때 의도대로 동작하는지"로 잡는다.

## Phase 1: e2e-format 규격 + e2e-tester 에이전트

이 phase가 끝나면 `e2e-tester` 에이전트를 단독 호출해 임의 feature-slug에 대해 케이스 문서를 생성하고 dev-browser로 실행할 수 있는 상태가 된다.

- [x] `.claude/rules/e2e-format.md` 작성 — 파일 위치/이름, frontmatter, 시나리오 섹션 구조, dev-browser 스크립트 코드 블록 규칙을 spec-format.md/plan-format.md와 같은 수준의 스키마로 정의
- [x] `.claude/agents/e2e-tester.md` 작성 — spec 사용자 시나리오/AC 기반 케이스 도출, 로컬 서버 헬스체크, dev-browser 실행, pass/fail 판정, backlog 기록(`category: test`), 스크린샷 저장, plan 체크박스 갱신 절차를 backend/frontend-reviewer와 대칭되는 구조로 작성
- [x] dev-browser CLI가 로컬에 설치되어 있는지 확인 (없으면 설치 방법을 에이전트 정의에 사전조건으로 명시) — 로컬 미설치 확인, e2e-tester 정의에 사전조건 절차로 명시함

## Phase 2: reviewer의 e2e 체이닝

이 phase가 끝나면 backend-reviewer/frontend-reviewer가 마지막 코드 phase를 pass 처리할 때 자동으로 e2e-tester를 호출하는 조건이 두 에이전트 정의에 반영된 상태가 된다.

- [x] `.claude/agents/backend-reviewer.md`에 "통과시킨 phase의 다음 섹션이 `## Phase <N+1>: E2E 검증`이면 e2e-tester를 체이닝 호출" 절차 추가
- [x] `.claude/agents/frontend-reviewer.md`에 동일 절차 추가

## Phase 3: plan-runner 오케스트레이터

이 phase가 끝나면 `plan-runner` 에이전트가 존재하고, plan 하나를 지정해 호출하면 phase 순회·reviewer 호출·fix 루프·최종 보고까지 자동 수행하는 상태가 된다.

- [x] `.claude/agents/plan-runner.md` 작성 — plan 상태 확인(재개 지점 질의 포함), phase 순회, 구현 서브에이전트 스폰, 파일 경로 기반 reviewer 선택(혼재 시 둘 다 호출), 실패 시 backlog 기반 fix 서브에이전트 재스폰, phase당 최대 3회 상한과 에스컬레이션, 최종 통과 보고 절차를 정의
- [x] Agent 도구로 서브에이전트(구현/fix)를 스폰할 때 spec/plan/backlog 파일 경로만 넘기고 세부 내용은 서브에이전트가 직접 읽게 하는 프롬프트 구조를 명시

## Phase 4: Dry run 검증

이 phase가 끝나면 세 산출물(e2e-tester, reviewer 체이닝, plan-runner)이 실제로 문서 규격을 만족하고 상호 정합적으로 연결되는지 최소 시나리오로 확인된 상태가 된다. 실제 dev.sh 서버 기동 없이도 확인 가능한 범위(문서 정합성, 절차 시뮬레이션)와 서버가 필요한 범위(dev-browser 실제 실행)를 구분해 진행한다.

- [x] e2e-format.md 규격대로 임시 `e2e/` 케이스 문서 하나를 실제로 만들어 형식을 검증 (실제 서버 필요 시 절차만 시뮬레이션하고 결과를 기록) — 스크래치패드에 home-screen 기반 샘플 4개 시나리오 작성 중 규격 구멍 3건(셀렉터 전략, feature 필드 기준, 사전조건 충돌 처리) 발견 및 e2e-format.md 보강
- [x] backend-reviewer/frontend-reviewer/plan-runner 세 에이전트 정의를 상호 참조 관계(체이닝 조건, 파일 경로, 호출 규칙)가 실제로 맞물리는지 교차 검토 — 불일치 2건(환경 실패 판단 문구, backlog 경로 보고 명시성) 발견 및 수정
- [x] dry run 결과를 정리해 사용자에게 보고
</content>
