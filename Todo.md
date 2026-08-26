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

## 앱 전체 재디자인 (진행중)

배경: Claude Design 핸드오프 프로젝트(claude.ai/design `f05007c9-8716-43a5-b06f-1982d8a1b595`, `design_handoff_momentive_app/`)를 근거로 홈/카테고리/검색/상품상세/장바구니/위시리스트/마이 7개 화면 전체를 재구현. `specs/2026-08-26-app-redesign.md`(supersedes `2026-08-23-home-screen.md`), `plans/2026-08-26-app-redesign.md`(status: in_progress). 브랜치 `feat/app-redesign`.

- [x] grillme 세션 (Q1~Q12 결정 — UI 재구현 우선, category/sort만 백엔드 확장, 나머지는 localStorage 목업, 결제/Auth/Review/Coupon 백엔드는 범위 밖)
- [x] Phase 0: 공통 기반 — 백엔드 category/sort 파라미터, 아이콘 세트(lucide-react) 교체, 디자인 토큰 대조 확인(변경 불필요), BottomNav 5탭 전환, localStorage 유틸(`frontend/src/lib/storage/`)
- [x] Phase 1: 홈 — 프로모 배너/인기 랭킹/카테고리 칩 필터/최근 본 상품 섹션
- [x] Phase 2: 카테고리 + 검색 — 자동완성/최근검색어/인기검색어/정렬(`FilterSheet`). 검증 중 correctness 버그 발견·수정(`.claude/backlog/2026-08-26-app-redesign-phase2-01.md`)
- [x] Phase 3: 상품상세 — 사이즈 셀렉터/사이즈가이드·배송안내 아코디언/`ReviewCard`/위시 토글/장바구니 담기/최근 본 상품 기록
- [x] Phase 0~3 커밋 및 `origin/feat/app-redesign` 푸시 (커밋 `713eaa2`)
- [ ] Phase 4: 장바구니 (`/cart`) — `ShippingProgress` 이식, 수량/삭제, 쿠폰 토글, 결제 버튼 무동작 처리
- [ ] Phase 5: 위시리스트 (`/wishlist`)
- [ ] Phase 6: 마이 (`/mypage`) + 스펙 전체 수용 기준 최종 회귀 검증
- [ ] **7개 phase 전부 끝난 뒤 브라우저 시각 확인 일괄 진행** — 지금 구현/리뷰 에이전트들 도구에 브라우저 자동화가 없어 전부 코드 레벨 검증만 하고 넘어간 상태. `./dev.sh`로 로컬 띄워서 BottomNav 5탭, 화면 전환, localStorage 유지 등을 직접 눈으로 확인 필요
- [ ] 알려진 한계(참고만, 당장 조치 불필요): 시드 상품 15개 대부분이 의류가 아니라 `category`가 ACCESSORY로 쏠림(KNIT/INNERWEAR 상품 없음) — 실제 의류 상품 추가 전까지 카테고리 필터가 밋밋해 보일 수 있음

## 다음 기능

- [ ] 위 앱 리디자인(Phase 4~6) 마무리 후, 그 다음 기능은 별도 grillme 필요 (실제 결제/Order, 로그인/Auth 등 — 이번 리디자인에서 범위 밖으로 명시적으로 미룬 도메인들이 우선순위 후보)
