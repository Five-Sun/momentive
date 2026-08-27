# Todo

집/회사 등 세션이 끊기는 환경에서 작업을 이어가기 위한 진행 상황 기록. 완료되면 체크하고, 다음 세션에서는 이 파일부터 확인한다.

## 디자인 시스템 스킬 구축

배경: `specs/2026-08-18-product-catalog.md`의 "화면" 섹션은 구성 요소·상태만 정의하고 비주얼 디자인(레이아웃, 색상, 타이포 등)은 다루지 않음. 화면이 늘어나도 균일한 톤/컨셉을 유지하려면 디자인 시스템을 스킬로 내재화하기로 결정.

- [x] ~~claude.ai 웹에서 시안 이미지 생성~~ → 시안 없이 코드로 직접 토큰/컴포넌트 구현 (커밋 `2bf3126`)
- [x] `design.md`(브랜드 컨셉: 색상/톤/무드) 작성 — 구현된 코드 기준으로 역작성
- [x] ~~디자인 시스템 스킬 작성~~ → 스킬화 보류. `frontend/CLAUDE.md` → `design.md` 연결로 충분하다고 판단. 디자인 일관성 체크는 추후 `frontend-reviewer` 에이전트의 체크리스트 항목으로 포함 예정 (별도 퍼블리셔 에이전트는 만들지 않음)
- [x] `frontend/CLAUDE.md`의 "스타일링 방식" 항목 채우기
- [x] 스타일링 기반 기술 확정 — Tailwind CSS v4 (`@theme`), shadcn/ui 미사용, 자체 컴포넌트

## 상품 목록/상세 조회 + 홈 화면 (완료)

- [x] `specs/2026-08-18-product-catalog.md` 기반 plan 작성 → `plans/2026-08-23-product-catalog-home.md` (status: done)
- [x] Phase 1: 백엔드 — Product 도메인 및 조회 API 구현
- [x] Phase 2: 프론트 — 홈 화면 (목록 + 그리드 + 무한스크롤)
- [x] Phase 3: 프론트 — 상품 상세 페이지
- [x] `specs/2026-08-18-product-catalog.md`, `specs/2026-08-23-home-screen.md` — AC 전체 체크, status: implemented로 갱신
- [x] `./dev.sh` — DB+백엔드+프론트 로컬 일괄 실행 스크립트 추가
- [x] `feat/home-screen` 브랜치 푸시 및 `develop` 대상 PR 생성 → https://github.com/Five-Sun/momentive/pull/1 (머지 대기)

## frontend-reviewer 에이전트 (완료)

배경: `specs/2026-08-25-frontend-reviewer-agent.md` / `plans/2026-08-25-frontend-reviewer-agent.md`. phase 구현 후 사용자가 직접 전체 리뷰하지 않아도 되도록 정적 리뷰(컨벤션/타입/correctness/simplification) + build/lint 실행을 맡는 서브에이전트.

- [x] Phase 1: `frontend-reviewer.md` 에이전트 정의 작성
- [x] 버그 발견 및 수정: `frontend/.claude/agents/`(하위 디렉토리)에 둬서 에이전트가 로드되지 않던 문제 — Claude Code는 서브에이전트를 프로젝트 루트 `.claude/agents/`와 유저 레벨 `~/.claude/agents/`에서만 스캔함(하위 디렉토리 재귀 스캔 미지원). `.claude/agents/frontend-reviewer.md`로 이동 (커밋 `d8ab54e`) — 세션 재시작 후 정상 로드 확인
- [x] Phase 2: dry-run 5개 시나리오 실행 (`plans/2026-08-25-frontend-reviewer-agent.md` Phase 2)
  - [x] 존재하지 않는 feature-slug 호출 → 에러 안내 확인
  - [x] `app-shell`의 존재하지 않는 Phase(99) 호출 → 에러 안내 확인
  - [x] "frontend-reviewer로 app-shell Phase 2 검증해줘" → 재검증 의도 확인 질문 확인
  - [x] 재검증 진행 → 정적 리뷰 + build/lint + 보고 사이클 확인
  - [x] 종료 후 `git status`/`git diff`로 소스 코드 무변경 확인
- [x] 버그 발견 및 수정: `ReportFindings` 툴이 frontmatter에 선언돼 있어도 커스텀 서브에이전트 호출 시 실제로 제공되지 않음 (플랫폼 제약으로 추정) → `specs/2026-08-26-frontend-reviewer-agent.md`(supersedes 2026-08-25) + `plans/2026-08-25-frontend-reviewer-agent-fix-1.md`로 대화형 구조화 요약 방식으로 전환, 에이전트 정의 수정 후 재검증 완료
- [ ] 관찰됨(미해결, 우선순위 낮음): 재검증 시나리오를 두 번 호출했을 때 한 번은 "재검증 의도 확인" 질문을 했고 한 번은 확인 없이 바로 진행함 — 프롬프트 표현 차이에 따른 비결정적 동작으로 보임. 실사용에 지장 없으면 당장 안 고쳐도 됨

## plan-runner 자동 사이클 (완료)

배경: `specs/2026-08-26-plan-runner-cycle.md` / `plans/2026-08-26-plan-runner-cycle.md`(status: done). 정적 리뷰(backend/frontend-reviewer)만으로는 유저 플로우 회귀를 못 잡는 문제 + phase마다 사용자가 직접 구현→리뷰→수정을 손으로 트리거해야 하는 문제를 함께 해소. [dev-browser](https://github.com/sawyerhood/dev-browser) 기반 `e2e-tester`와, phase를 자동 순회시키는 `plan-runner` 오케스트레이터를 신설.

- [x] `.claude/agents/backend-reviewer.md` 신설 (frontend-reviewer와 대칭 구조 — 계층 분리/DTO·Entity/예외/트랜잭션 컨벤션 검증 + `./gradlew build`/`test`)
- [x] Phase 1: `.claude/rules/e2e-format.md` 규격 + `.claude/agents/e2e-tester.md` 신설 — spec 시나리오/AC 기반 케이스 도출, 로컬 서버 헬스체크, dev-browser 실행/판정, backlog 기록
- [x] Phase 2: `backend-reviewer`/`frontend-reviewer`에 "마지막 코드 phase 통과 시 e2e-tester 체이닝" 절차(6-C) 추가
- [x] Phase 3: `.claude/agents/plan-runner.md` 신설 — phase 순회, 구현/fix 서브에이전트 스폰(Agent 도구), phase당 최대 3회 재시도, 상한 초과 시 에스컬레이션
- [x] Phase 4: dry run 검증 — 문서 상호 정합성 교차검토, e2e-format 규격 준수 샘플 케이스 생성. 5건 불일치 발견·수정(feature 필드 기준 통일, `ENV_FAILURE:` 고정 문구로 서버미기동 판별 통일, backlog 경로 보고 누락, `plan:` frontmatter 채우기 누락, 셀렉터/사전조건 스킵 규칙)
- [x] `e2e-tester` 실사용 검증 (2026-08-27): `dev-browser` CLI 설치 확인, `./dev.sh`로 로컬 서버 기동. 백엔드 포트 충돌(다른 프로젝트가 8080 점유) 발견 → `backend/application.yml` 포트를 8081로 변경하고 `dev.sh`/`frontend/.env.local(.example)`/`e2e-tester.md`의 8080 참조를 8081로 일괄 수정
  - [x] 가드레일 경로 확인: E2E phase 없는 plan(`product-catalog-home`)에 대해 정확히 중단하고 안내, plan/spec 미수정 확인
  - [x] happy path 확인: `plans/2026-08-23-product-catalog-home-fix-1.md`(planner로 신규 작성한 E2E 검증 전용 fix plan)로 실제 dev-browser 시나리오 6개 실행 → 전부 pass, `e2e/2026-08-27-product-catalog-home.md` 생성, plan 체크박스/status 자동 갱신까지 확인
  - [ ] 미관찰: `plan-runner` 오케스트레이터 자체(phase 순회, fix 서브에이전트 스폰, 재시도 루프)는 아직 실사용 안 됨 — 현재 plan들이 전부 done이라 재시도를 촉발할 미완료 phase가 없음. 다음에 새 phase 작업 시 plan-runner로 직접 돌려서 확인 필요
- [x] `feat/plan-runner-cycle` 브랜치 푸시 및 `develop` 대상 PR 갱신 (기존 PR #2가 이미 있었음) → https://github.com/Five-Sun/momentive/pull/2 (머지 대기)

## 다음 기능

- [ ] `feat/app-redesign` 브랜치: Phase 4~6(장바구니/위시리스트/마이) 커밋·푸시 및 `develop` 대상 PR 생성이 아직 미진행 (다른 브랜치 작업이므로 별도 세션에서 처리)
- [ ] 위 두 브랜치 정리 후, 다음 기능은 별도 grillme 필요 (실제 결제/Order, 로그인/Auth 등 — 앱 리디자인에서 범위 밖으로 명시적으로 미룬 도메인들이 우선순위 후보)
