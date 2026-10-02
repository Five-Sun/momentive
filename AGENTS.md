# 모멘티브 (Momentive)

강아지 쇼핑몰. 개인사업자로 실 운영. 실제 고객이 있는 서비스.

## 제약사항
- 결제: 토스페이먼츠 (샌드박스 → 심사 후 실결제 전환)
- 프론트: Next.js, Vercel 배포
- 백엔드: Spring Boot, Railway 배포, 서브도메인(api.모멘티브도메인)
- 혼자 개발 + AI 에이전트 팀으로 구현. 비용(API 호출량, 인프라) 항상 염두에 둘 것

## 로컬 개발 환경 실행
- `./dev.sh` — DB(docker compose) + 백엔드 + 프론트를 한 번에 기동. Ctrl+C로 전체 종료
- 로그는 `.dev-logs/backend.log`, `.dev-logs/frontend.log`

## 저장소 구조
- `docs/specs/` — grillme로 뽑은 기능 스펙 (기능 단위, 프론트+백엔드 통합)
- `docs/plans/` — 스펙 기반 phase/step 플랜
- `.hermes.md` — Hermes Lead의 역할별 모델 라우팅·사용량 보호 정책
- `backend/` — Spring Boot. 컨벤션은 `backend/CLAUDE.md` 참고
- `frontend/` — Next.js. 컨벤션은 `frontend/CLAUDE.md` 참고
- `.claude/rules/spec-format.md` — 스펙 작성 규격 (필수 준수)
- `.claude/rules/plan-format.md` — 플랜 작성 규격 (필수 준수)
- `.claude/rules/git.md` — 커밋 메시지·브랜치 규칙 (필수 준수)
- `.claude/skills/` — Architect·Reviewer·QA가 읽는 grilling, write-plan, review-phase, verify-e2e 절차

## 작업 원칙
1. 역할별 모델 선택, 호출 범위, 사용량 보호는 `.hermes.md`를 따른다. 이 파일은 spec-driven 원칙을 대체하지 않고 실행 주체를 정한다.
2. 새 기능 또는 API·DB·보안 계약 변경은 Architect가 `grilling` 절차로 요구사항을 정리한 뒤 스펙을 작성한다. 완료 조건이 이미 명확한 한두 파일의 국소 수정은 `.hermes.md`의 작은 수정 흐름으로 처리할 수 있다.
3. 스펙은 `.claude/rules/spec-format.md` 규격(파일명, frontmatter, 섹션 구성)을 그대로 따른다.
4. 확정 스펙이 둘 이상의 독립 검증 단위를 가지면 구현 전에 `.claude/rules/plan-format.md` 규격으로 phase/step plan을 작성한다.
5. 스펙의 수용 기준(acceptance criteria)은 체크 가능한 형태로 작성한다.
6. 커밋 메시지와 브랜치명은 `.claude/rules/git.md`를 따른다.
7. 백엔드/프론트 관련 세부 컨벤션은 각 하위 CLAUDE.md를 따른다 — 여기 중복 기재하지 않는다.
8. 작업 시작 전 관련 `docs/specs/`, `docs/plans/`, `docs/backlog/`를 먼저 찾아 참고한다 — 새 세션에서도 기존 결정·실패 근거를 이어받기 위함.
9. 문서(spec/plan 등)는 전체를 바로 작성하지 않고, 요약(목적·범위·주요 항목)을 먼저 제시해 컨펌받은 뒤 작성한다.
