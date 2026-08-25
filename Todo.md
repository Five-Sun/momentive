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

## frontend-reviewer 에이전트

배경: `specs/2026-08-25-frontend-reviewer-agent.md` / `plans/2026-08-25-frontend-reviewer-agent.md`. phase 구현 후 사용자가 직접 전체 리뷰하지 않아도 되도록 정적 리뷰(컨벤션/타입/correctness/simplification) + build/lint 실행을 맡는 서브에이전트.

- [x] Phase 1: `frontend-reviewer.md` 에이전트 정의 작성
- [x] 버그 발견 및 수정: `frontend/.claude/agents/`(하위 디렉토리)에 둬서 에이전트가 로드되지 않던 문제 — Claude Code는 서브에이전트를 프로젝트 루트 `.claude/agents/`와 유저 레벨 `~/.claude/agents/`에서만 스캔함(하위 디렉토리 재귀 스캔 미지원). `.claude/agents/frontend-reviewer.md`로 이동 (커밋 `d8ab54e`) — 세션 재시작 후 정상 로드 확인
- [ ] Phase 2: dry-run 5개 시나리오 재실행 (`plans/2026-08-25-frontend-reviewer-agent.md` Phase 2 참고)
  - [ ] 존재하지 않는 feature-slug 호출 → 에러 안내 확인
  - [ ] `app-shell`의 존재하지 않는 Phase(예: 99) 호출 → 에러 안내 확인
  - [ ] "frontend-reviewer로 app-shell Phase 2 검증해줘" → 재검증 의도 확인 질문 확인
  - [ ] 재검증 진행 → 정적 리뷰 + build/lint + ReportFindings 사이클 확인
  - [ ] 종료 후 `git status`/`git diff`로 소스 코드 무변경 확인

## 다음 기능

- [ ] 다음 기능 grillme 필요 (장바구니 등 — `docs/domain-overview.md` 우선순위 참고)
