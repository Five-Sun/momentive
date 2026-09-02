# Todo

집/회사 등 세션이 끊기는 환경에서 작업을 이어가기 위한 진행 상황 기록. 완료되면 체크하고, 다음 세션에서는 이 파일부터 확인한다.

## 앱 전체 재디자인 (완료)

배경: Claude Design 핸드오프 프로젝트(claude.ai/design `f05007c9-8716-43a5-b06f-1982d8a1b595`, `design_handoff_momentive_app/`)를 근거로 홈/카테고리/검색/상품상세/장바구니/위시리스트/마이 7개 화면 전체를 재구현. `specs/2026-08-26-app-redesign.md`(supersedes `2026-08-23-home-screen.md`), `plans/2026-08-26-app-redesign.md`(status: in_progress). 브랜치 `feat/app-redesign`.

- [x] grillme 세션 (Q1~Q12 결정 — UI 재구현 우선, category/sort만 백엔드 확장, 나머지는 localStorage 목업, 결제/Auth/Review/Coupon 백엔드는 범위 밖)
- [x] Phase 0: 공통 기반 — 백엔드 category/sort 파라미터, 아이콘 세트(lucide-react) 교체, 디자인 토큰 대조 확인(변경 불필요), BottomNav 5탭 전환, localStorage 유틸(`frontend/src/lib/storage/`)
- [x] Phase 1: 홈 — 프로모 배너/인기 랭킹/카테고리 칩 필터/최근 본 상품 섹션
- [x] Phase 2: 카테고리 + 검색 — 자동완성/최근검색어/인기검색어/정렬(`FilterSheet`). 검증 중 correctness 버그 발견·수정(`docs/backlog/2026-08-26-app-redesign-phase2-01.md`)
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
- [x] `plan-runner` 오케스트레이터 실사용 확인 (2026-08-28, `plans/2026-08-27-auth.md` 5-phase 전체 실행): 실행 중 main/develop에서 바로 도는 것을 막는 작업 브랜치 확인 절차가 없다는 간극을 발견해 `.claude/agents/plan-runner.md`에 추가(커밋 `c5922f2`). 5개 phase 전부 backlog 실패 기록 없이 1차 통과, E2E phase까지 체이닝되어 완주 확인
- [x] `feat/plan-runner-cycle` 브랜치 푸시 및 `develop` 대상 PR 갱신 → https://github.com/Five-Sun/momentive/pull/2, 이후 `develop`에 머지 완료

## 백엔드/프론트엔드 컨벤션 정비 (완료)

배경: PR #3(`feat/app-redesign`) 머지 후 다음 후보인 로그인/Auth, 결제/Order 착수 전에, 두 도메인에서 실제로 걸릴 아키텍처 컨벤션을 `/grillme`로 미리 정리 — 정식 기능 spec은 아니고 `backend/CLAUDE.md`/`frontend/CLAUDE.md`의 컨벤션 섹션을 확장하는 세션.

- [x] `/grillme 백엔드 컨벤션` — 인증/인가(JWT+Spring Security, Refresh Token은 Postgres 테이블로 관리·Redis 미도입), 토스페이먼츠 연동(재고 선점 → confirm 호출 → 성공/실패 전이 흐름, `PENDING` 만료 스케줄러, `PaymentGatewayClient` 추상화, confirm 재시도 없음), Write API 검증(Bean Validation ↔ Service 검증 경계, `fieldErrors`), 동시성/락(`@Version` 낙관적 락 + for-loop 2회 재시도, Redis 분산락 미도입) 결정. Controller/Service Lombok `@RequiredArgsConstructor` 전환 규칙도 포함. `backend/CLAUDE.md` 반영 (커밋 `859210b`)
- [x] `/grillme frontend 컨벤션` — API 에러 처리(공통 `apiFetch` 래퍼 + `ApiError` 타입, `fieldErrors` 유무로 인라인/Toast 구분), 인증 상태 관리(`AuthProvider`를 `(shell)/layout.tsx`에서 SSR 초기화, 401 시 자동 refresh + 1회 재시도), Write 폼/검증(React Hook Form + Zod, `src/components/forms/` 필드 컴포넌트) 결정. 데이터 페칭 전략(TanStack Query 도입 여부)은 방향성만 정하고 현재 패턴 유지로 보류. `frontend/CLAUDE.md` 반영 (커밋 `8e15b64`)
- [x] 참고 항목 해소: 보호된 라우트 패턴(페이지 내 조건부 렌더링 채택, `<RequireAuth>` 공통화는 보호 화면이 마이페이지 하나뿐이라 범위 밖으로 보류), 실제 가입 방식(이메일/비밀번호, 소셜로그인은 별도 spec), admin 화면(role 필드만 존재, 실기능은 범위 밖) — 아래 Auth spec/grillme에서 결정됨

## 로그인/회원가입 (Auth) (완료, PR 머지)

배경: `docs/specs/2026-08-27-auth.md`, `docs/plans/2026-08-27-auth.md`(status: done). 이메일/비밀번호 회원가입·로그인·로그아웃 + JWT(Access 30분/Refresh 14일, rotation) 세션 도입. 브랜치 `feat/auth`.

- [x] Phase 1: 백엔드 통합 — `User`/`RefreshToken` 엔티티, Spring Security, JWT 발급/검증 필터, `/auth/*`(signup·login·logout·refresh·me), `@CurrentUser`, `WebConfig` CORS `allowCredentials(true)`. `AuthService` 단위/통합 테스트 포함
- [x] Phase 2: 프론트 인증 인프라 — `src/lib/api/auth.ts`, `apiFetch` 401 자동 refresh(동시요청 in-flight 공유), `AuthProvider`, `(shell)/layout.tsx`의 `/me` 선호출
- [x] Phase 3: `/login`, `/signup` 화면 — React Hook Form + Zod 클라이언트 검증, 서버 `fieldErrors` 인라인 매핑
- [x] Phase 4: 마이페이지 로그인/비로그인 상태 조건 분기, 로그아웃 버튼, 기존 비로그인 허용 기능(상품조회/장바구니/위시리스트) 회귀 없음 확인
- [x] Phase 5: E2E 검증 — `docs/e2e/2026-08-27-auth.md` 9개 시나리오 전부 PASS (회원가입/로그인/세션갱신/로그아웃/마이페이지 비로그인·로그인 분기 등), 실패 backlog 없음
- [x] `feat/auth` 브랜치 푸시 및 `develop` 대상 PR 생성 → https://github.com/Five-Sun/momentive/pull/5, `develop`에 머지 완료
- [x] 뒷정리: spec `status`를 `implemented`로 갱신, 수용 기준(AC) 체크박스 13개 전부 체크 완료

## E2E 케이스 단일 탭 순차 실행 전환 (완료)

배경: e2e-tester 브라우징 테스트 중 탭이 9개(auth 시나리오 개수만큼)까지 쌓이는 문제 발견. 원인은 `e2e-format.md` 규격상 시나리오마다 `browser.getPage(name)`에 다른 name을 써서 탭을 분리했는데, dev-browser가 CLI 호출이 끝나도 브라우저를 유지하는 영속 데몬이라 탭이 자동으로 안 닫혔기 때문. 브랜치 `chore/e2e-single-tab-flow`.

- [x] `.claude/rules/e2e-format.md` 개정 — 파일 하나당 탭 하나(`getPage(feature-slug)` 1회 호출)를 공유하며 시나리오를 순서대로 이어 실행하는 단일 `## 실행 스크립트` 섹션 방식으로 변경. 앞 시나리오 실패 시 뒤 시나리오는 자동 중단(즉시 중단 방식 선택, try/catch로 계속 진행하지 않음). 상태 충돌 시 화면 조작으로 되돌리거나 별도 파일로 분리하는 가이드 추가
- [x] `.claude/agents/e2e-tester.md` 개정 — 시나리오별 개별 dev-browser 실행 → 파일당 1회 실행으로 변경. 판정에 "미실행"(앞 시나리오 실패로 못 돈 시나리오, fail과 구분·backlog 중복 생성 안 함) 카테고리 추가
- [x] 기존 `docs/e2e/2026-08-27-auth.md`, `2026-08-27-product-catalog-home.md`는 구 형식 그대로 둠(사용자 요청 — 나중에 필요해지면 새 형식으로 재작성)
- [x] 커밋(`5be078f`) 및 `chore/e2e-single-tab-flow` 푸시
- [x] `develop` 대상 PR 생성 및 머지 → https://github.com/Five-Sun/momentive/pull/6, `develop`에 머지 완료

## 다음 기능

- [x] PR #3(`feat/app-redesign` → `develop`) 리뷰 후 머지 완료 (PR #2는 이미 머지됨 — 이 merge로 Todo.md 충돌 해소)
- [x] PR #5(`feat/auth` → `develop`) 리뷰 후 머지 완료
- [x] 로그인/Auth 완료 — 다음은 결제/Order(토스페이먼츠 연동)를 `/grillme`로 기능 spec 작성부터 시작 (Auth가 선행 조건이었음, `backend/CLAUDE.md`에 이미 정리된 토스페이먼츠 컨벤션 참고)

## 장바구니→주문→결제(토스페이먼츠) (구현 완료, 실결제 연동만 보류 — PR 대기)

배경: `docs/specs/2026-08-29-cart-order-payment.md`(status: confirmed — 실결제 미검증으로 되돌림), `docs/plans/2026-08-29-cart-order-payment.md`(status: in_progress). `feat/cart-order-payment` 브랜치, `plan-runner`로 Phase 1~6 자동 실행 완료.

- [x] grillme 세션 — 장바구니 localStorage 유지, 재고 수량 필드 도입, 사이즈는 문자열만 저장, 배송지는 다중 주소록(기본배송지 플래그, 최초 주문 시 입력), 부분결제 지원(장바구니 체크박스), 배송비 없음, Toss 결제위젯 SDK, 재고는 주문 생성 시 선점, Order 상태 4종(PENDING/PAID/FAILED/CANCELLED), 결제 실패 시 재시도 없이 새 주문, PAID 상태에서만 취소 가능(재고 복원), 마이페이지 주문내역 목록+상세 포함 결정. 쿠폰/적립금/배송비 정책/부분환불/배송추적/Cart 백엔드 이전/상품옵션 관리는 범위 밖으로 명시
- [x] spec 작성 완료 — `docs/specs/2026-08-29-cart-order-payment.md`
- [x] planner로 phase/step 플랜 작성 완료 — `docs/plans/2026-08-29-cart-order-payment.md`
- [x] `plan-runner`로 Phase 1~6 자동 실행 완료 (백엔드 재고/주문/배송지 골격, Toss confirm/cancel/만료 스케줄러, 프론트 장바구니 선택/체크아웃/결제위젯/마이페이지 주문내역, E2E 검증). 구현 중 발견·수정된 이슈: 낙관적 락 재시도 off-by-one(`docs/backlog/2026-08-29-cart-order-payment-phase1-01.md`), 쿠폰 placeholder가 실결제 금액에 반영되던 버그(phase3-01), `GlobalBottomNav`가 체크아웃/주문상세 CTA를 가리던 문제(phase6-01)
- [x] 체크아웃 화면 버그 발견·수정: 저장된 배송지 선택 시(신규 입력 폼 미노출) `useForm`이 항상 `zodResolver`로 검증을 시도해 "결제하기" 클릭이 무반응이던 문제 — `resolver`를 `showNewAddressForm`일 때만 적용하도록 수정 (`frontend/src/app/(shell)/checkout/page.tsx`)
- [ ] **Toss 결제위젯 실연동 미완성** — 클라이언트 키가 결제위젯 API(`widget-groups/keys`)에서 401. 원인은 Toss 개발자센터에서 계정 가입만 하고 상점(스토어) 등록(사업자 정보 필요)을 하지 않은 것으로 추정. 사업자 등록 확인 후(별도 진행) 재검증 필요 — 상세 `docs/backlog/2026-08-30-cart-order-payment-phase4-01.md`
- [x] `e2e-tester`가 확인 가능한 범위(장바구니 선택, 체크아웃 진입/배송지, 주문 생성, 결제 실패/만료 화면 전환, 주문내역 조회, FAILED/CANCELLED 취소버튼 미노출)는 전부 PASS — `docs/e2e/2026-08-29-cart-order-payment.md`
- [ ] 나머지 기능(다음 항목들) 먼저 마무리한 뒤, 상점 등록 완료 시점에 이 기능으로 돌아와 결제위젯 렌더링~confirm 성공~`PAID` 취소까지 마무리 검증
- [x] `feat/cart-order-payment` 커밋 및 `develop` 대상 PR 생성 → https://github.com/Five-Sun/momentive/pull/7, 리뷰 후속 조치(결제/주문내역 에러 핸들링 보완) 반영 완료, `develop`에 머지 완료

## Swagger(API 명세서) 도입 (완료)

배경: 백엔드 API를 Swagger/OpenAPI로 문서화해 API 명세서로 쓰고 싶음. 확인 결과 현재 `build.gradle` 의존성, `backend/CLAUDE.md` 컨벤션, `backend-reviewer` 체크리스트 어디에도 Swagger 설정이 없음(2026-08-27 확인). `docs/specs/2026-08-30-api-documentation.md`(status: implemented), `docs/plans/2026-08-30-api-documentation.md`(status: done).

- [x] `/grillme`로 요구사항 확정 — 전체 API 문서화(admin 없음), 필수 애노테이션은 `@Operation` summary + DTO `@Schema` + 인증 필요 엔드포인트 `@SecurityRequirement`(에러 응답 `@ApiResponse`는 선택), 운영 환경에서도 Swagger UI 노출(profile 분리 안 함), 그룹 미분리(admin 도메인 생기면 재검토), 컨벤션 문서+`backend-reviewer` 체크리스트 둘 다 반영
- [x] `plan-runner`로 Phase 1~3 자동 실행 완료 — springdoc-openapi 2.8.9(plan 명시 2.8.17이 Spring Boot 3.4.1과 `PatternParseException` 충돌 일으켜 다운그레이드) 도입, 4개 컨트롤러(Auth/Product/Address/Order) 전체 엔드포인트에 `@Operation`, DTO 17개 전체 필드에 `@Schema`, 인증 필요 엔드포인트에 `@SecurityRequirement` 소급 적용, `backend/CLAUDE.md`/`backend-reviewer.md` 컨벤션 반영
- [x] 코드 리뷰에서 나온 advisory 수정: `@CurrentUser Long userId` 파라미터가 Swagger 문서에 일반 필수 query parameter로 잘못 노출되던 문제 → 8개 엔드포인트에 `@Parameter(hidden = true)` 추가로 해소
- [x] `feat/api-documentation` 커밋 및 `develop` 대상 PR 생성 → https://github.com/Five-Sun/momentive/pull/8, 리뷰 후속 조치(spec 버전 정정) 반영 완료, `develop`에 머지 완료

## 다음 작업 후보 정리 (완료)

배경: "화면은 있는데 실제 동작이 구현 안 된 부분부터 1차 개발범위로 잡자"는 방향으로 코드베이스 전수 조사(2026-08-30). 결과를 Todo.md에 정리. 브랜치 `docs/todo-next-candidates`.

- [x] 조사 결과 3건을 다음 작업 후보로 정리 — 마이페이지 메뉴 5개(완전 무동작, 백엔드 도메인 없음), 리뷰(조회+작성, 전 상품 공통 하드코딩 목업), 쿠폰 시스템(UI/state만 있고 실제 쿠폰함 없음)
- [x] 커밋 및 `develop` 대상 PR 생성 → https://github.com/Five-Sun/momentive/pull/9, `develop`에 머지 완료

## 상품 리뷰 (조회 + 작성) (완료, PR 머지)

배경: 위 "다음 작업 후보" 조사에서 가장 우선순위 높게 선택된 항목. 상품상세의 `ReviewCard`가 전 상품 공통 `MOCK_REVIEWS`(하드코딩 목업)를 보여주고, 별점(`Rating value={4.5}`)도 5개 화면 전부에서 모든 상품에 동일하게 하드코딩되어 있던 문제를 해소. `docs/specs/2026-08-30-product-review.md`(status: implemented), `docs/plans/2026-08-30-product-review.md`(status: done). 브랜치 `feat/product-review`.

- [x] grillme 세션(Q1~Q16) — 구매 확인(verified purchase) 기반 작성 제한, 이미지 첨부는 범위 밖(텍스트+별점만), 홈/카테고리/검색/상품상세/위시리스트 5개 화면 평점 전부 실제 집계값으로 교체, 작성자 본인 수정/삭제 허용, 사용자당 상품 1개 제한(삭제 후 재작성 허용), 작성 진입점은 상품상세+마이페이지 주문내역 상세 둘 다, 별점(1~5 정수)·텍스트(10~500자) 둘 다 필수, 목록은 최신순+더보기, 평점 집계는 쓰기 시점 동기 갱신, 이미 리뷰 쓴 상품은 버튼이 바로 수정 폼으로 전환, 신고/모더레이션은 범위 밖, 작성자는 `User.nickname` 그대로 노출 결정
- [x] spec 작성 완료 — `docs/specs/2026-08-30-product-review.md`
- [x] planner로 phase/step 플랜 작성 완료 — `docs/plans/2026-08-30-product-review.md` (Phase 1: 백엔드 Review 도메인+Product 평점 집계 / Phase 2: 프론트 상품상세 조회+작성/수정/삭제 / Phase 3: 마이페이지 진입점+목록형 화면 평점 교체 / Phase 4: E2E 검증)
- [x] `plan-runner`로 Phase 1~4 자동 실행 완료. 구현 중 발견·수정된 실제 코드 결함: `GET /products/{id}/reviews/me`가 "리뷰 없음"을 `200 + 빈 바디`로 응답해 프론트 `apiFetch`가 JSON 파싱 에러를 던지고, 이게 "구매 미확인"으로 잘못 처리되어 정상 구매자도 리뷰 작성 버튼이 안 뜨던 문제(`docs/backlog/2026-08-31-product-review-phase4-01.md`) — 백엔드는 `ResponseEntity`로 명시적 JSON `null` 응답하도록, 프론트 `apiFetch`는 빈 바디를 `204`와 동일하게 안전 처리하도록, `ProductDetailView`는 예상 못한 에러를 조용히 폴백하지 않고 로깅+안내하도록 3곳 함께 수정
- [x] E2E 검증 — `docs/e2e/2026-08-30-product-review.md` 시나리오 1~6(로그인+빈 상태, 리뷰 작성, 수정, 마이페이지 진입점, 목록 평점 반영, 삭제 후 재작성) 전부 PASS
- [x] `./gradlew build`/`test`(43/43), `npm run build`/`lint` 최종 재확인 통과
- [x] spec AC 12개 전부 체크, status를 `implemented`로 갱신
- [x] 로컬 dev DB가 테스트 실행으로 초기화되어(별도 테스트 DB 없이 dev DB를 공유/wipe하는 정책) 무효화된 E2E 시딩값(상품 279→61, 주문 158→1)을 재시딩 후 갱신, "더보기" 페이지네이션·닉네임 비마스킹·카테고리/검색/위시리스트 평점 표시 등 나머지 AC 항목도 `docs/e2e/2026-08-31-product-review.md`로 추가 검증 — spec AC 11개 전부 실제 재확인 완료
- [x] `feat/product-review` 커밋 및 `develop` 대상 PR 생성 → https://github.com/Five-Sun/momentive/pull/10, `develop`에 머지 완료

## 마이페이지 메뉴 정리 (반려견 프로필 관리 + 고객센터) (완료, PR 머지)

배경: 마이페이지 메뉴 5개(배송조회/쿠폰함/적립금/반려견 프로필 관리/고객센터) 중 다른 진행 중 작업과 겹치지 않는 두 항목만 우선 정리. `docs/specs/2026-08-31-mypage-menu-cleanup.md`(status: implemented), `docs/plans/2026-08-31-mypage-menu-cleanup.md`(status: done). 브랜치 `feat/mypage-menu-cleanup`.

- [x] grillme 세션 — 반려견 프로필 관리+고객센터만 이번 범위(배송조회는 배송 상태 개념 자체가 없어 무거움, 쿠폰/적립금은 별도 쿠폰 시스템 항목과 겹침), 반려견은 사용자당 여러 마리(User 1:N)/필드(이름 필수+품종·생일·성별·몸무게 선택)/사진 없이 기본 아이콘(이미지 업로드 인프라 부재, 리뷰 이미지와 동일 사유)/전체 CRUD/다른 도메인과 연동 없는 독립 화면, 고객센터는 백엔드 없는 정적 FAQ+인스타그램(`@momentive_official`) 연락처 결정
  - 세션 중 발견: FAQ 배송비 문항 작성 과정에서 사용자가 실제 네이버 스마트스토어 배송비 정책(3,400원/7만원 이상 무료/제주·도서산간 +4,000원) 스크린샷을 제시 — `cart-order-payment`가 이미 "배송비 없음"으로 구현·배포된 것과 불일치함을 확인. 결제 로직 수정은 이번 grillme 범위 밖으로 분리하고 별도 후보로 아래에 기록(FAQ 문구는 실제 정책 그대로 반영)
- [x] spec 작성 완료 — `docs/specs/2026-08-31-mypage-menu-cleanup.md`
- [x] planner로 phase/step 플랜 작성 완료 — `docs/plans/2026-08-31-mypage-menu-cleanup.md` (Phase 1: 백엔드 Pet 도메인 CRUD / Phase 2: 프론트 `/mypage/pets` / Phase 3: 프론트 `/mypage/support`+메뉴 연결 / Phase 4: E2E 검증)
- [x] `plan-runner`로 Phase 1~4 자동 실행, 전 phase 1차 통과(backlog 실패 기록 없음). Phase 4 E2E에서 환경 문제 발견: 백엔드에 devtools가 없어 `./dev.sh`가 최초 기동한 `bootRun` 프로세스가 Phase 1의 Pet 코드 변경을 반영하지 못한 채 떠 있던 상태 → 사용자가 `./dev.sh` 재기동 후 재개해 해결. 재발 방지로 `.claude/agents/e2e-tester.md`에 "backend/ 파일을 다루는 plan이면 검증 전 백엔드 프로세스를 스스로 재기동" 절차 추가(별도 커밋)
- [x] E2E 검증 — `docs/e2e/2026-08-31-mypage-menu-cleanup.md` 시나리오 1~8 전부 PASS, 이후 FAQ 4개 항목 전부 노출 여부까지 추가로 직접 재확인
- [x] spec AC 11개 전부 체크, status를 `implemented`로 갱신
- [x] `feat/mypage-menu-cleanup` 커밋(기능 구현 + e2e-tester 개선 2개 커밋) 및 `develop` 대상 PR 생성 → https://github.com/Five-Sun/momentive/pull/11, `develop`에 머지 완료

## 배송비 정책 반영 (완료, PR 머지)

배경: 실제 사업자 배송비 정책(네이버 스마트스토어 기준: 기본 3,400원 / 상품금액 7만원 이상 무료배송 / 제주 우편번호(63000~63644)면 상품금액과 무관하게 4,000원 항상 추가)이 `cart-order-payment`의 "배송비 없음" 구현과 불일치하던 문제(`mypage-menu-cleanup`의 고객센터 FAQ grillme 세션 중 발견)를 해소. `docs/specs/2026-08-31-shipping-fee-policy.md`(status: implemented), `docs/plans/2026-08-31-shipping-fee-policy.md`(status: done). `cart-order-payment`는 건드리지 않고 독립 spec으로 분리(Toss 위젯 실연동 이슈와 별개로 추적). 브랜치 `feat/shipping-fee-policy`.

- [x] grillme 세션(Q1~Q7) — 제주만 우편번호 자동 판정(그 외 도서산간은 범위 밖, Todo 후속 후보로 분리), 무료배송 임계값은 상품금액(items subtotal) 기준, 제주 할증은 무료배송 조건과 무관하게 항상 별도 부과, 체크아웃/마이페이지 주문상세/장바구니 3화면 모두 반영(장바구니는 기존에 미연결 상태였던 `ShippingProgress` 컴포넌트 재활용, 기준금액 70,000원 갱신), 장바구니 진행바는 선택된 상품 기준, 우편번호 형식 검증은 범위 밖(별도 후속 후보로 분리) 결정
- [x] spec 작성 완료 — `docs/specs/2026-08-31-shipping-fee-policy.md`
- [x] planner 호출 시도 — coordinator를 통한 relay 승인이라는 이유로 planner가 파일 작성을 거부(기존 "subagent 승인은 직접 확인이어야 함" 패턴과 동일하게 재확인됨) → 직접 plan 파일 작성 — `docs/plans/2026-08-31-shipping-fee-policy.md` (Phase 1: 백엔드 계산 로직+`Order`/`OrderResponse` 확장+마이그레이션 / Phase 2: 프론트 체크아웃+주문상세 breakdown / Phase 3: 프론트 장바구니 `ShippingProgress` 연결 / Phase 4: E2E 검증)
- [x] `plan-runner`로 Phase 1~3 자동 실행, 전부 1차 통과(재시도 0회, backlog 실패 기록 없음)
- [x] Phase 4 E2E 검증 — 시나리오 1(장바구니 무료배송 안내)·시나리오 2(체크아웃 배송지 전환 시 배송비 재계산) PASS. 시나리오 3(주문 생성 후 마이페이지 주문상세 breakdown 확인)은 1차 시도 시 로컬 dev DB `product` 테이블이 비어있어 스킵됐으나(flyway 이력상 시드 마이그레이션은 `success`로 기록돼 있으나 실제 데이터 없음 — 기존 "Local DB branch drift" 패턴과 동일), 이전 세션에서 8081 포트를 물고 남아있던 좀비 백엔드 프로세스(22시간 넘게 방치)를 종료하고 `docker compose down -v` → `./dev.sh` 재기동으로 DB 재시딩 후 재검증해 PASS
- [x] Phase 2·3의 "검증(수동, 브라우저)" 항목도 사용자 요청으로 e2e-tester가 대체 검증 — 시나리오 4(체크아웃 신규 배송지 제주↔비제주 왕복 전환 시 배송비 즉시 재계산: 비제주 3,400원 → 제주 경계값 63644 7,400원 → 비제주 복귀 3,400원), 시나리오 5(장바구니 선택 금액 70,000원 기준 왕복 전환 시 안내/진행바 전환) 추가 PASS. `docs/e2e/2026-08-31-shipping-fee-policy.md`에 시나리오 1~5 전부 기록
- [x] Toss 결제위젯 confirm 성공 경로는 상점 미등록 제약으로 이번에도 스킵 대상(`docs/backlog/2026-08-30-cart-order-payment-phase4-01.md`와 동일) — 위젯 렌더링 금액은 시나리오 3에서 확인, confirm 검증 로직은 기존 `order.totalAmount` 단일 소스 구조 재확인으로 대체
- [x] plan 전 phase 완료로 `status`를 `done`으로 갱신, spec AC 9개 전부 체크 및 `status`를 `implemented`로 갱신
- [x] `feat/shipping-fee-policy` 커밋 및 `develop` 대상 PR 생성 → https://github.com/Five-Sun/momentive/pull/13, `develop`에 머지 완료 (커밋 `f506d69`)

## 쿠폰 시스템 (완료, PR #14 리뷰 대기)

배경: 마이페이지 "쿠폰함"이 `onClick: () => {}` 무동작이고, 장바구니 쿠폰 토글은 `COUPON_DISCOUNT = 3000` 하드코딩으로 표시만 바뀌고 실제 결제금액에 미반영(실 고객에게 노출된 거짓 UI). 백엔드에 coupon 도메인 전무였음. `docs/specs/2026-09-01-coupon-system.md`(status: confirmed, AC 13/20 검증), `docs/plans/2026-09-01-coupon-system.md`(status: done). 브랜치 `feat/coupon-system`.

- [x] grillme 세션(Q1~Q17) — 적립금/배송조회/무료배송쿠폰/관리자 발급 API/회원가입 자동지급/상품·카테고리 한정/선착순 소진/쿠폰 중복사용은 전부 범위 밖. 발급은 **쿠폰 코드 입력 하나로 고정**(정의는 flyway 시드), 정액+정률(상한 필수) 2종, 조건은 유효기간·최소주문금액·1인1회, 한 주문 1장, 무료배송 임계값은 **할인 전** 금액 기준, 할인액은 `min(할인액, itemsSubtotal)`로 제한, 쿠폰은 주문 생성(PENDING) 시 선점하고 실패·만료·PAID취소 3경로에서 복원, `Coupon`/`UserCoupon` 2테이블, 체크아웃에서 선택(장바구니 토글 제거), 할인 계산은 배송비와 동일하게 프론트 미러링 결정
- [x] spec 작성 완료 — `docs/specs/2026-09-01-coupon-system.md` (AC 20개)
- [x] plan 작성 완료 — `docs/plans/2026-09-01-coupon-system.md` (Phase 1: 백엔드 쿠폰 도메인 / Phase 2: 백엔드 주문 연동 / Phase 3: 프론트 쿠폰함 / Phase 4: 프론트 결제 흐름 / Phase 5: E2E). Phase 1·2를 나눈 이유는 `Order.getItemsSubtotal()` 역산 제거 + 기존 주문 백필이 **이미 배포된 데이터에 영향을 주는 유일한 위험 구간**이라 격리한 것
- [x] **JDK 21 블로커 해소** (2026-09-02, 다른 머신) — 이 머신은 Homebrew로 JDK 21(`/opt/homebrew/Cellar/openjdk@21/21.0.8`)이 이미 설치돼 있었음(과거 "JDK 21 자체가 없음" 블로커와 다른 상황). `JAVA_HOME`을 21로 지정하면(예: `JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home ./gradlew build`) 바로 해소됨 — 셸 프로필에는 영구 반영하지 않기로 함(사용자 선택), 매 gradle 호출에 인라인으로 지정
- [x] 착수 전 워크트리 정리 — 쿠폰 plan과 무관한 admin 로그인/시드 미커밋 변경(`LoginRequest`, `dev.sh`, 로그인 화면, `V9__seed_admin_user.sql`)을 발견해 `chore/admin-login-seed` 브랜치(develop 기준)로 분리·커밋(푸시는 안 함). **주의**: 이 브랜치도 `V9` 마이그레이션 파일명을 쓰는데, 쿠폰 시스템이 이미 `V9__create_coupon.sql`~`V11`을 선점했으므로 `chore/admin-login-seed`를 나중에 develop에 머지할 때 `V12`로 재번호해야 함
- [x] `plan-runner`로 Phase 1~5 자동 실행 완료 — Phase 1~4는 전부 1회 시도로 통과(재시도 0회, backlog 실패 기록 없음). `./gradlew build`/`test`(coupon 포함 85 tests), `npm run build`/`lint` 전부 통과
- [x] Phase 5 E2E 1차 실행 시 로컬 dev DB가 비어있어(coupon/product/orders/users 전부 0건, flyway 이력은 success인데 실제 데이터 없음 — 기존에 반복된 패턴과 동일) 대다수 시나리오 스킵 → `docker compose down -v` 후 `./dev.sh` 재기동으로 재시딩(상품 15건, 쿠폰 4건 확인) 후 재검증
- [x] E2E 재검증 — `docs/e2e/2026-09-01-coupon-system.md` 시나리오 14개 중 12개 PASS, 2개는 "사전조건 미충족으로 스킵"(시나리오 4: 관리자 발급 API가 없어 만료 쿠폰 시드를 만들 방법 없음, 시나리오 13: 재시딩 직후라 V11 이전 주문을 재현할 수 없음 — 둘 다 코드 결함 아님, Phase 2 백엔드 테스트로 별도 커버). Toss confirm 성공 경로는 상점 미등록 제약으로 기존과 동일하게 스킵(위젯 렌더 금액만 확인)
- [x] plan 전 phase 완료로 `status`를 `done`으로 갱신. spec AC는 실제 E2E로 확인된 9개만 체크, 나머지 11개는 각기 사유(백엔드 테스트로만 커버/환경 제약으로 검증 불가)를 남기고 미체크 유지 — `status`는 `implemented`로 올리지 않고 `confirmed` 유지(AC 전량 실증 전까지)
- [x] **Windows 머신 QA (2026-09-02)** — 사용자가 `C:\Users\tyo10\.jdks\graalvm-jdk-21.0.7`에 JDK 21을 설치해 이 머신에서도 백엔드 기동이 가능해짐. E2E가 "사전조건 미충족"으로 스킵했던 항목과 아무도 눈으로 안 본 화면을 직접 확인
  - `./gradlew build test` 통과 (84 tests, 실패 0 — 쿠폰 관련 23개 포함), `npm run build`/`lint` 통과
  - **만료 쿠폰 UX 확인** — 시드에 만료 쿠폰이 없는 게 원인이었으므로 로컬 DB에 직접 넣어 검증. 등록 실패 3종("유효기간이 지난 쿠폰입니다"/"존재하지 않는 쿠폰 코드입니다"/"이미 등록한 쿠폰입니다") 전부 인라인 표시 확인
  - **만료 파생 판정 확인** — 등록해둔 쿠폰의 `expires_at`을 과거로 바꾸자 `status`는 `AVAILABLE`인데도 "사용 완료・만료" 구간으로 이동. 상태값이 아닌 `expiresAt` 기준 판정이 실제 동작함
  - **정률 상한** 72,000원 × 10% = 7,200원 → 상한 5,000원으로 제한 확인. **무료배송 할인 전 기준** 할인 후 총액 67,000원(임계값 미만)인데도 배송비 무료 유지 확인
  - 장바구니 "쿠폰" 문자열 0건(가짜 토글 완전 제거), 마이페이지 메뉴 → `/mypage/coupons` 라우팅, 만료 쿠폰이 체크아웃 목록에서 제외됨 확인
  - 화면 시각 확인 — 기존 화면과 디자인 언어 일관. fullPage 스크린샷에서 결제하기 CTA가 금액 요약을 가리는 것처럼 보였으나 `position: fixed` 캡처 왜곡이었고, 뷰포트 좌표 측정 결과 겹침 없음(총 결제금액 하단 748px / 버튼 상단 798px)
  - spec AC 4개 추가 체크(메뉴 라우팅·등록 실패 3종·정률 상한·빌드 4종) → 13/20. 나머지 7개는 백엔드 테스트로만 커버되거나(복원 3경로, 할인액 초과, 서버 재검증) 환경 제약(Toss confirm, V11 이전 주문 재현 불가)이라 미체크 유지
  - 개선 제안(버그 아님): 체크아웃 쿠폰 카드에 쿠폰명만 있고 할인 내용/유효기간이 없음, 쿠폰함 카드에서 최소금액과 유효기간이 구분자 없이 붙어 보임, 만료 쿠폰 시드가 없어 이 영역이 계속 회귀 검증 사각지대로 남음
- [x] **`dev.sh` 크로스 플랫폼 대응** — `uname -s`로 macOS/Linux/Windows(Git Bash)를 판별해 분기하도록 개선. (1) 포트 정리를 OS별로 분기 — Windows에는 `lsof`가 없어 종료 시 정리가 실패하고 좀비 프로세스가 남던 문제(Todo에 기록된 "8081 포트 22시간 점유"의 원인)를 `netstat -ano` + `taskkill`로 해소 (2) JDK 21 자동 감지 — OS별 표준 설치 경로(Homebrew Cellar / `~/.jdks` / Program Files 등)를 훑어 `JAVA_HOME`을 지정, 못 찾으면 안내 후 중단 (3) Windows에서는 `gradlew.bat` 사용 (4) 기동 전 docker 설치·데몬 응답 확인 단계 추가. Windows에서 기동~종료 전 구간 실동작 확인 완료
  - 참고: 이전 세션에서 "dev.sh가 Windows에서 docker를 못 찾는다"고 기록했던 것은 **오진**이었음 — PowerShell에서 `bash -lc`에 인자를 넘길 때 `\$PATH`가 PowerShell 변수로 먼저 확장돼 PATH가 비워진 호출 실수였고, Git Bash 자체에는 docker가 정상적으로 잡힌다
- [x] `feat/coupon-system` 커밋 및 `develop` 대상 PR 생성 → https://github.com/Five-Sun/momentive/pull/14 (리뷰/머지 대기)

## 디자인 수정 사전 조사 (2026-09-01, 내일 작업 준비)

배경: 디자인 수정 예정이라 현재 프론트 디자인 관리 방식을 전수 조사. 코드 변경 없이 `docs/design.md`, `frontend/CLAUDE.md`, `frontend/src/app/globals.css`, `frontend/src/components/`, `(shell)` 라우트 전체를 확인했다. 결론은 **Tailwind CSS v4 + `globals.css` CSS 변수 토큰 + 자체 컴포넌트 + `/style-guide` 샘플** 구조이며, 외부 UI 컴포넌트 라이브러리(shadcn 등)는 없다. 아이콘은 `lucide-react`.

- [x] 디자인 기준 문서: `docs/design.md`가 살아있는 디자인 시스템 문서. 원칙상 새 화면/컴포넌트 전 `docs/design.md`와 `/style-guide`를 확인하고, 토큰/컴포넌트가 바뀌면 문서도 같이 갱신해야 한다. `frontend/CLAUDE.md`도 같은 규칙을 갖고 있음(raw hex/rgb/hsl 금지, 반복 arbitrary Tailwind 값은 토큰화).
- [x] 전역 토큰 원천: `frontend/src/app/globals.css`
  - 색상: `brand-pink*`, `brand-yellow*`, `ink/body/muted/muted-soft`, `hairline/hairline-soft/border-strong`, `canvas/surface-*`, `success/error/sale/scrim`
  - 레이아웃: radius `xs/sm/md/lg`, shadow `card/float`, spacing은 Tailwind 기본 4px 스케일 + `section` 64px
  - 타이포: `layout.tsx`에서 Google Fonts `Jua` + `Noto Sans KR` 로드, `.text-display-*`, `.text-title*`, `.text-body*`, `.text-caption`, `.text-price`, `.text-button`, `.text-tag` 합성 유틸 제공
- [x] 앱 셸 구조: `frontend/src/app/layout.tsx`는 폰트/전역 CSS만 담당. `frontend/src/app/(shell)/layout.tsx`가 모바일 앱 프레임(`max-w-[480px]`, 바깥 `surface-strong`, 안쪽 `canvas`, `shadow-float`)과 `AuthProvider`, `GlobalBottomNav`를 담당. `/style-guide`는 `(shell)` 밖이라 프레임/하단탭 제외.
- [x] 공통 컴포넌트 원천: `frontend/src/components/`
  - `core`: `Button`, `IconButton`, `Badge`, `Chip`
  - `forms`: `TextField`, `PasswordField`, `SearchInput`, `AddressFields`
  - `navigation`: `BottomNav`, `GlobalBottomNav`
  - `commerce`: `ProductCard`, `ProductGridItem`, `ProductMiniCard`, `ProductImage`, `ProductDetailView`, `Rating`, `SizeSelector`, `FilterSheet`, `ReviewCard`, `ReviewForm`
  - `feedback/skeleton`: `Toast`, `ShippingProgress`, `ProductCardSkeleton`
- [x] 에셋 상태: `frontend/public/logo/momentive-logo.jpeg`가 사실상 유일한 브랜드 이미지. 나머지는 Next 기본 SVG. 상품 이미지는 API URL을 그대로 쓰고, 실패/부재 시 `surface-strong` 플레이스홀더로 대체. 디자인 수정이 실제 비주얼 중심이면 `frontend/public/`에 에셋 추가 전략부터 잡아야 함.
- [x] 토큰 준수 상태: raw hex/rgb 색상은 `globals.css` 안에만 있음. 실제 화면/컴포넌트는 대부분 `bg-brand-*`, `text-ink`, `border-hairline` 같은 토큰 유틸을 사용. 단, `text-white`, `bg-white/90`, arbitrary 치수/보더(`text-[15px]`, `h-[38px]`, `border-[1.5px]`, `px-[18px]`, `h-[72px]`, `top-[52px]`, `rounded-[10px]`, `max-w-[480px]`)가 여러 파일에 흩어져 있음.
- [x] 중복 패턴/추출 후보:
  - 상단 헤더(`h-13`, 좌측 back, 가운데 제목, 우측 spacer)가 `cart`, `checkout`, `payment`, `orders`, `coupons`, `pets`, `support`, `ProductDetailView`에 반복됨
  - 하단 CTA 바(`sticky bottom-16` 또는 `fixed bottom-0 left-1/2 max-w-[480px]`)가 `ProductDetailView`, `cart`, `checkout`, `checkout/payment`, `mypage/orders/[orderId]`에 반복됨
  - 선택 원형 체크 UI가 `cart`, `checkout`에 반복됨
  - 카드/목록 컨테이너(`border-hairline bg-surface-card rounded-md border p-3~3.5`)가 주문/쿠폰/반려견/고객센터/체크아웃에 반복됨
  - 2열 상품 그리드는 홈/검색/위시리스트에 반복됨
- [x] 눈에 띄는 드리프트: `docs/design.md`는 `/style-guide`를 기준 샘플로 안내하지만, `frontend/src/app/style-guide/page.tsx`의 BottomNav 예시는 아직 4탭(홈/검색/위시/장바구니)이고 실제 앱 `GlobalBottomNav`는 5탭(홈/카테고리/검색/위시/마이). 디자인 수정 때 `/style-guide`도 같이 갱신 필요.
- [ ] 내일 권장 작업 순서:
  1. 먼저 수정 범위를 확정: 단순 브랜드 토큰 변경인지, 화면 구조/컴포넌트 재정리까지 포함한 리디자인인지 구분
  2. 기준 스냅샷 확보: `/style-guide`, `/`, `/search`, `/products/{id}`, `/cart`, `/checkout`, `/checkout/payment`, `/mypage`, `/mypage/orders`, `/mypage/coupons`, `/mypage/pets`, `/mypage/support`
  3. 토큰 변경은 `globals.css` → `docs/design.md` 순서로 반영. 색/폰트/radius/shadow를 먼저 바꾸면 대부분 컴포넌트가 따라감
  4. 공통 컴포넌트 수정: `Button/IconButton/Badge/Chip` → `TextField/PasswordField/SearchInput` → `ProductCard/ProductMiniCard/ProductImage/ProductGridItem` → `BottomNav/Toast/ShippingProgress/FilterSheet/ReviewCard/ReviewForm/SizeSelector`
  5. 넓은 리디자인이면 반복 패턴을 먼저 컴포넌트화 후보로 검토: `AppHeader`, `BottomActionBar`, `SelectableCircle` 또는 `CheckControl`, `SummaryRows`, `SurfaceCard/ListRow`, `ProductGrid`
  6. 마지막에 라우트별 페이지 sweep: 홈/검색/상품상세/장바구니/체크아웃/주문상세/마이 하위 화면 순서가 영향 범위가 큼
- [ ] 검증 체크: `cd frontend && npm run lint && npm run build`. 브라우저 검증은 390px 모바일 폭과 1280px 데스크톱 폭 둘 다 확인. 특히 하단 네비와 CTA 겹침, `FilterSheet` 오버레이 위치, `Toast` 위치, 긴 한국어 텍스트 줄바꿈, 상품 이미지 실패 플레이스홀더를 확인.
- [ ] 작업 전 주의: 쿠폰 시스템(`frontend/src/app/(shell)/cart/page.tsx`, `checkout/page.tsx`, `mypage/page.tsx`, `mypage/coupons/*` 등)이 2026-09-02 기준 구현·커밋 완료됨 — 디자인 작업은 이 화면들이 이미 반영된 최신 코드 위에서 진행하면 된다.

## 다음 작업 후보

- [ ] 디자인 수정 작업 — 위 "디자인 수정 사전 조사" 섹션 기준으로 착수. 우선순위는 `globals.css` 토큰과 공통 컴포넌트부터, 화면별 직접 수정은 그 다음
- [ ] **관리자 상품 관리 부재** — 상품 등록/수정/재고조정 API도 화면도 전혀 없고(`ProductController`는 `GET` 2개뿐), 상품 데이터는 `V2__seed_product.sql`의 15개 시드가 전부. `User.role`에 `ADMIN` enum은 있으나 부여 경로도 검사 지점도 0건인 미사용 스캐폴딩. **실 운영 중인데 신상품 등록·재입고에 매번 마이그레이션 작성 + 재배포가 필요한 상태**(2026-09-01 조사). 쿠폰도 같은 제약을 감수하고 시드 방식으로 가기로 했으므로, 이 항목이 해소되면 쿠폰 발급도 함께 편해짐
- [ ] Toss 결제위젯 실연동은 상점(스토어) 등록 완료 후 재검증 필요 (`docs/backlog/2026-08-30-cart-order-payment-phase4-01.md`) — 사용자 측 외부 조치 대기 중
- [ ] **우편번호 형식 검증 부재** — `AddressRequest.zipcode`가 `@NotBlank`만 있고 형식 검증이 전혀 없는 자유 텍스트(2026-08-31 배송비 정책 grillme 세션 중 발견). 배송비 spec에서는 제주 판정 시 숫자 파싱 실패/범위 밖이면 안전하게 "제주 아님"으로만 처리하고 정식 형식 검증(5자리 숫자 등)은 범위 밖으로 분리했음 — 기존에 저장된 배송지 데이터 하위호환까지 고려해야 해서 별도 작업으로 남김
