# 모멘티브 (Momentive)

강아지 쇼핑몰. AI 에이전트 팀으로 스펙 기반 개발. 프론트(Next.js, Vercel)·백엔드(Spring Boot, Railway)로 구성되어 있고, 로컬 개발은 `./dev.sh` 하나로 기동한다.

## 지침 파일
- `AGENTS.md` — 프로젝트 공통 지침 (Claude Code, Codex CLI 공유)
- `CLAUDE.md` — `AGENTS.md`를 가리키는 Claude Code 전용 진입점
- `backend/CLAUDE.md`, `frontend/CLAUDE.md` — 도메인별 컨벤션

## 작업 흐름
1. `/grillme`로 첫 스펙 작성 (`.claude/skills/grilling/SKILL.md`) → `docs/specs/`
2. 스펙 확정 후 `.claude/agents/planner.md`(`write-plan` skill)로 phase/step 플랜 작성 → `docs/plans/`
3. 구현 → `.claude/agents/backend-reviewer.md`/`frontend-reviewer.md`(`review-phase` skill)로 phase 검증
4. 마지막 코드 phase 통과 시 `.claude/agents/e2e-tester.md`(`verify-e2e` skill)로 브라우저 E2E 검증
5. 여러 phase를 한 번에 자동 순회하려면 `.claude/agents/plan-runner.md` 사용 (Claude Code 단독 세션 전용)

Buzz에서는 위 역할이 Lead(Architect)/Builder(Backend, Frontend)/Reviewer 세 agent로 나뉘어 채널 메시지로 handoff한다.

## 디렉토리
- `docs/specs/` — 기능 스펙
- `docs/plans/` — phase/step 플랜
- `docs/backlog/` — 검증 실패 기록
- `docs/e2e/` — E2E 케이스
- `.claude/rules/` — spec/plan/backlog/e2e 작성 규격 + git 규칙 (필수 준수)
- `.claude/skills/` — grilling, write-plan, review-phase, verify-e2e 절차
- `.claude/agents/` — planner, backend-reviewer, frontend-reviewer, e2e-tester, plan-runner
- `.claude/commands/grillme.md` — 스펙 인터뷰 커맨드
- `backend/`, `frontend/` — 각 도메인 컨벤션은 하위 `CLAUDE.md`
