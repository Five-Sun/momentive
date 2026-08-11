# 모멘티브 (Momentive)

강아지 쇼핑몰. AI 에이전트 팀으로 스펙 기반 개발.

## 세팅 순서
1. `git init` 후 이 스캐폴드 그대로 커밋
2. `backend/`에 Spring Boot 프로젝트 생성 (Spring Initializr) → `/health` 엔드포인트만 만들어서 Railway 배포
3. `frontend/`에 Next.js 프로젝트 생성 → Vercel 배포, `/health` 호출해서 화면에 표시
4. 도메인 연결 (프론트: 루트 도메인, 백엔드: api. 서브도메인)
5. `/grillme`로 첫 스펙(예: 상품 목록 조회) 작성 → `.claude/agents/planner.md`로 플랜 생성 → 구현 → `.claude/agents/reviewer.md`로 검증

## 디렉토리
- `specs/` 기능 스펙
- `plans/` phase/step 플랜
- `.claude/rules/spec-format.md` 스펙 작성 규격 (필수)
- `.claude/commands/grillme.md` 스펙 인터뷰 커맨드
- `.claude/agents/` 공통 에이전트 (planner, reviewer)
- `backend/`, `frontend/` 각 도메인 컨벤션은 하위 CLAUDE.md
