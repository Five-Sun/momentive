# 모멘티브 (Momentive)

강아지 쇼핑몰. 개인사업자로 실 운영. 실제 고객이 있는 서비스.

## 제약사항
- 결제: 토스페이먼츠 (샌드박스 → 심사 후 실결제 전환)
- 프론트: Next.js, Vercel 배포
- 백엔드: Spring Boot, Railway 배포, 서브도메인(api.모멘티브도메인)
- 혼자 개발 + AI 에이전트 팀으로 구현. 비용(API 호출량, 인프라) 항상 염두에 둘 것

## 저장소 구조
- `specs/` — grillme로 뽑은 기능 스펙 (기능 단위, 프론트+백엔드 통합)
- `plans/` — 스펙 기반 phase/step 플랜
- `backend/` — Spring Boot. 컨벤션은 `backend/CLAUDE.md` 참고
- `frontend/` — Next.js. 컨벤션은 `frontend/CLAUDE.md` 참고
- `.claude/rules/spec-format.md` — 스펙 작성 규격 (필수 준수)
- `.claude/rules/git.md` — 커밋 메시지·브랜치 규칙 (필수 준수)
- `.claude/commands/grillme.md` — 요구사항 인터뷰 커맨드

## 작업 원칙
1. 새 기능은 반드시 `/grillme`로 스펙부터 작성한다 (스펙 없이 바로 구현 금지)
2. 스펙은 `.claude/rules/spec-format.md` 규격(파일명, frontmatter, 섹션 구성)을 그대로 따른다
3. 스펙의 수용 기준(acceptance criteria)은 체크 가능한 형태로 작성한다
4. 커밋 메시지와 브랜치명은 `.claude/rules/git.md`를 따른다
5. 백엔드/프론트 관련 세부 컨벤션은 각 하위 CLAUDE.md를 따른다 — 여기 중복 기재하지 않는다
6. 작업 시작 전 `specs/`, `plans/`에서 관련 문서를 먼저 찾아 참고한다 — 새 세션에서도 기존 결정/컨텍스트를 이어받기 위함
7. 문서(스펙/플랜 등) 생성 시 전체를 바로 작성하지 않고, 요약(목적·범위·주요 항목)을 먼저 제시해 컨펌받은 뒤 작성한다 — 토큰 절약 목적
