# Todo

집/회사 등 세션이 끊기는 환경에서 작업을 이어가기 위한 진행 상황 기록. 완료되면 체크하고, 다음 세션에서는 이 파일부터 확인한다.

## 앱 전체 재디자인 (완료, PR 머지 대기)

배경: Claude Design 핸드오프 프로젝트(claude.ai/design `f05007c9-8716-43a5-b06f-1982d8a1b595`, `design_handoff_momentive_app/`)를 근거로 홈/카테고리/검색/상품상세/장바구니/위시리스트/마이 7개 화면 전체를 재구현. `specs/2026-08-26-app-redesign.md`(supersedes `2026-08-23-home-screen.md`), `plans/2026-08-26-app-redesign.md`(status: in_progress). 브랜치 `feat/app-redesign`.

- [x] grillme 세션 (Q1~Q12 결정 — UI 재구현 우선, category/sort만 백엔드 확장, 나머지는 localStorage 목업, 결제/Auth/Review/Coupon 백엔드는 범위 밖)
- [x] Phase 0: 공통 기반 — 백엔드 category/sort 파라미터, 아이콘 세트(lucide-react) 교체, 디자인 토큰 대조 확인(변경 불필요), BottomNav 5탭 전환, localStorage 유틸(`frontend/src/lib/storage/`)
- [x] Phase 1: 홈 — 프로모 배너/인기 랭킹/카테고리 칩 필터/최근 본 상품 섹션
- [x] Phase 2: 카테고리 + 검색 — 자동완성/최근검색어/인기검색어/정렬(`FilterSheet`). 검증 중 correctness 버그 발견·수정(`.claude/backlog/2026-08-26-app-redesign-phase2-01.md`)
- [x] Phase 3: 상품상세 — 사이즈 셀렉터/사이즈가이드·배송안내 아코디언/`ReviewCard`/위시 토글/장바구니 담기/최근 본 상품 기록
- [x] Phase 0~3 커밋 및 `origin/feat/app-redesign` 푸시 (커밋 `713eaa2`)
- [x] Phase 4: 장바구니 (`/cart`) — `ShippingProgress` 이식, 수량/삭제/쿠폰 토글/금액 요약, 결제 버튼 무동작 처리(토스트만). `npm run build`/`npm run lint` 통과
- [x] Phase 5: 위시리스트 (`/wishlist`) — 2열 그리드, 하트 토글. 구현 중 `ProductCard` 하트 클릭이 카드 클릭(라우팅)으로 버블링되던 버그 발견·수정(`e.stopPropagation()`)
- [x] Phase 6: 마이 (`/mypage`) — 프로필(아바타 placeholder)/위시·장바구니 실카운트·주문 0 고정/메뉴 5개 무동작. `./gradlew test`(9/9)·`npm run build`·`npm run lint` 전부 최종 재확인 통과
- [x] **7개 phase 전부 끝난 뒤 브라우저 시각 확인 일괄 진행** — Playwright(chromium-cli 미설치라 스크래치패드에 직접 설치해 대체)로 `./dev.sh` 기동 후 홈/카테고리/검색/상품상세/장바구니/위시리스트/마이 전 화면 스크린샷 검증 완료. BottomNav 5탭 활성 스타일, 카테고리→검색 필터링, 사이즈선택→장바구니담기→토스트, 위시토글, 수량변경/쿠폰/무료배송진행바/결제무동작, 위시리스트, 마이 카운트 실시간 반영 전부 정상 확인
  - 검증 중 발견·해결한 이슈 2건: (1) 로컬 개발 DB(`backend_momentive-db` 볼륨)에 flyway 이력상 시드가 성공했다고 나오는데 실제 `product` 테이블이 비어있던 문제 → 볼륨 재생성으로 해결 (원인 불명, 코드 문제 아님). (2) 이 검증 세션(샌드박스)이 Google Fonts(`fonts.gstatic.com`) 요청에 간헐적으로 실패해 홈 500 에러 발생 → `layout.tsx`를 시스템 폰트로 임시 치환해 검증 후 원본으로 완전 원복(`git diff` 없음 확인). 실제 Vercel 배포 환경 코드는 변경 없음
  - Phase 5 구현 중 발견한 실제 버그도 수정 완료: `ProductCard`의 하트 버튼 클릭이 카드 클릭(라우팅)으로 버블링되던 문제 → `e.stopPropagation()` 추가
- [x] plan `status`를 `done`으로, 스펙(`specs/2026-08-26-app-redesign.md`) 수용 기준 전체 체크 및 `status`를 `implemented`로 갱신 완료
- [x] `feat/app-redesign` 커밋(Phase 4~6분) 및 푸시, `develop` 대상 PR 생성 → https://github.com/Five-Sun/momentive/pull/3, `develop`에 머지 완료
- [ ] 알려진 한계(참고만, 당장 조치 불필요): 시드 상품 15개 대부분이 의류가 아니라 `category`가 ACCESSORY로 쏠림(KNIT/INNERWEAR 상품 없음) — 실제 의류 상품 추가 전까지 카테고리 필터가 밋밋해 보일 수 있음

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
- [x] `feat/plan-runner-cycle` 브랜치 푸시 및 `develop` 대상 PR 갱신 → https://github.com/Five-Sun/momentive/pull/2, 이후 `develop`에 머지 완료

## 백엔드/프론트엔드 컨벤션 정비 (완료)

배경: PR #3(`feat/app-redesign`) 머지 후 다음 후보인 로그인/Auth, 결제/Order 착수 전에, 두 도메인에서 실제로 걸릴 아키텍처 컨벤션을 `/grillme`로 미리 정리 — 정식 기능 spec은 아니고 `backend/CLAUDE.md`/`frontend/CLAUDE.md`의 컨벤션 섹션을 확장하는 세션.

- [x] `/grillme 백엔드 컨벤션` — 인증/인가(JWT+Spring Security, Refresh Token은 Postgres 테이블로 관리·Redis 미도입), 토스페이먼츠 연동(재고 선점 → confirm 호출 → 성공/실패 전이 흐름, `PENDING` 만료 스케줄러, `PaymentGatewayClient` 추상화, confirm 재시도 없음), Write API 검증(Bean Validation ↔ Service 검증 경계, `fieldErrors`), 동시성/락(`@Version` 낙관적 락 + for-loop 2회 재시도, Redis 분산락 미도입) 결정. Controller/Service Lombok `@RequiredArgsConstructor` 전환 규칙도 포함. `backend/CLAUDE.md` 반영 (커밋 `859210b`)
- [x] `/grillme frontend 컨벤션` — API 에러 처리(공통 `apiFetch` 래퍼 + `ApiError` 타입, `fieldErrors` 유무로 인라인/Toast 구분), 인증 상태 관리(`AuthProvider`를 `(shell)/layout.tsx`에서 SSR 초기화, 401 시 자동 refresh + 1회 재시도), Write 폼/검증(React Hook Form + Zod, `src/components/forms/` 필드 컴포넌트) 결정. 데이터 페칭 전략(TanStack Query 도입 여부)은 방향성만 정하고 현재 패턴 유지로 보류. `frontend/CLAUDE.md` 반영 (커밋 `8e15b64`)
- [ ] 참고(이번 세션 범위 밖, 다음 Auth/Order feature spec 그릴링에서 다룰 것): 보호된 라우트 패턴(middleware vs 페이지 가드), 실제 가입 방식(이메일 vs 소셜로그인), admin 화면 자체

## 다음 기능

- [x] PR #3(`feat/app-redesign` → `develop`) 리뷰 후 머지 완료 (PR #2는 이미 머지됨 — 이 merge로 Todo.md 충돌 해소)
- [ ] 백엔드/프론트 컨벤션 정비 완료 — 다음은 로그인/Auth 또는 결제/Order 중 하나를 골라 `/grillme`로 기능 spec 작성부터 시작

## Swagger(API 명세서) 도입

배경: 백엔드 API를 Swagger/OpenAPI로 문서화해 API 명세서로 쓰고 싶음. 확인 결과 현재 `build.gradle` 의존성, `backend/CLAUDE.md` 컨벤션, `backend-reviewer` 체크리스트 어디에도 Swagger 설정이 없음(2026-08-27 확인).

- [ ] `/grillme`로 요구사항 확정 — 문서화 범위(전체 API vs 일부), 인증 필요 엔드포인트 노출 여부, 그룹 분리 여부 등
- [ ] `build.gradle`에 springdoc-openapi 의존성 추가 + OpenAPI 설정
- [ ] `backend/CLAUDE.md`에 컨트롤러/DTO 문서화 애노테이션(`@Operation`, `@Schema` 등) 컨벤션 추가
- [ ] `backend-reviewer.md` 체크리스트에 Swagger 애노테이션 검증 항목 추가
