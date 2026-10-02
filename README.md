# 모멘티브 (Momentive)

강아지 쇼핑몰. AI 에이전트 팀으로 스펙 기반 개발. 프론트(Next.js, Vercel)·백엔드(Spring Boot, Railway)로 구성되어 있고, 로컬 개발은 `./dev.sh` 하나로 기동한다.

## 지침 파일
- `AGENTS.md` — 프로젝트 공통 지침 (Claude Code, Codex CLI 공유)
- `CLAUDE.md` — `AGENTS.md`를 가리키는 Claude Code 전용 진입점
- `backend/CLAUDE.md`, `frontend/CLAUDE.md` — 도메인별 컨벤션

## 작업 흐름

역할별 모델 라우팅과 사용량 보호 규칙은 프로젝트 루트 [`.hermes.md`](.hermes.md)를 따른다. 기존 spec-driven 문서와 규칙은 그대로 유지한다.

1. 새 기능·계약 변경은 Architect가 `grilling` 절차로 요구사항을 정리하고, 사용자 승인 후 같은 역할이 phase/step plan을 작성한다.
2. Builder는 확정된 plan의 **한 phase만** 구현하고 관련 단위 테스트를 작성·수정한다.
3. Hermes QA가 build/test/lint/API smoke를 실제 실행한다. backend 또는 frontend 위험 phase는 Reviewer가 해당 도메인 checklist로 독립 검토한다.
4. 마지막 코드 phase 후에는 QA가 `verify-e2e` 절차로 브라우저 E2E를 수행한다. 이후 재실행은 통과한 스크립트를 우선 사용한다.

Buzz는 선택적인 외부 협업 채널이며, Momentive의 기본 개발 workflow나 역할별 모델 라우팅에 필요하지 않다.

## 디렉토리
- `docs/specs/` — 기능 스펙
- `docs/plans/` — phase/step 플랜
- `docs/backlog/` — 검증 실패 기록
- `docs/e2e/` — E2E 케이스
- `.claude/rules/` — spec/plan/backlog/e2e 작성 규격 + git 규칙 (필수 준수)
- `.claude/skills/` — grilling, write-plan, review-phase, verify-e2e 절차
- `backend/`, `frontend/` — 각 도메인 컨벤션은 하위 `CLAUDE.md`
