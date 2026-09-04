# Todo

집/회사 등 세션이 끊기는 환경에서 작업을 이어가기 위한 진행 상황 기록. 완료되면 체크하고, 다음 세션에서는 이 파일부터 확인한다.

상세 맥락(결정 사유, 그릴링 Q&A, phase 구성)은 `docs/specs/`·`docs/plans/`·`docs/backlog/`·`docs/e2e/`와 PR 본문이 원천이다. 이 파일은 **지금 무엇이 남았는지**와 **어떤 순서로 갈지**만 관리한다.

---

## 실행 순서 (2026-09-03 확정)

아래 5단계를 순서대로 진행한다. 근거는 "유저 화면 전수 점검" 섹션.

| 단계 | 내용 | 브랜치 | spec |
|---|---|---|---|
| ~~1~~ | ~~장바구니 미비움 + 배송비 문구 정정~~ | ~~`fix/checkout-cart-and-shipping-notice`~~ | 완료 (PR #17) |
| 2 | 관리자 기반 + 상품 관리 (A) | `feat/admin-product-management` | `2026-09-04-admin-product-management` |
| 2-B | 주문 배송상태/송장 · 쿠폰 발급 (B) | 미정 | `/grillme` 필요 |
| 3 | 배송조회 | `feat/delivery-tracking` | `/grillme` |
| 4 | 화면 품질 정리 | `feat/*` | 규모 보고 판단 |
| 5 | 잔여 항목 | — | — |

### 1단계 — 실 고객 영향 버그 (완료, PR #17 머지)

- [x] **결제 성공 후 장바구니 비우기** — `checkout/success/page.tsx`가 `confirmOrder` 성공 후 장바구니를 전혀 건드리지 않아 결제를 마쳐도 상품이 남고 **중복 구매를 유발**했다. 부분결제를 지원하므로 전체를 비우지 않고, `getOrder`로 서버가 확정한 항목만 받아 키를 되돌려 제거한다. 키 포맷을 `cartKeyOf(productId, size)`로 추출하고 `removeCartItems`/`clearCheckoutSelection`을 추가했다. **confirm과 장바구니 정리를 분리**해, 정리 실패가 이미 확정된 결제를 실패 화면으로 보내지 않게 했다
- [x] **배송비 안내 문구 정정** — `ProductDetailView.tsx`의 제주/도서산간 추가 배송비 3,000원 → 4,000원(`ShippingFeePolicy.java:12`, 고객센터 FAQ와 일치)
- [x] `npm run build` / `npm run lint` 통과
- [ ] **브라우저 검증 보류** — `/checkout/success`는 Toss confirm 성공해야 도달하는데 상점 미등록으로 실결제 경로를 탈 수 없다. **상점 등록이 끝나면 Toss 실연동 검증과 같은 세션에서** 부분결제 시 결제한 항목만 사라지는지까지 확인할 것. 상품상세 문구도 육안 확인 미실시(정적 상수 한 줄이라 위험은 낮음)
- [ ] 참고(범위 밖): 상품상세 배송 안내에 기본 배송비 3,400원 / 7만원 이상 무료 문구가 없다. FAQ에는 있으므로 4단계에서 보완 가능

### 2단계 — 관리자 기능 (spec/plan 확정, 구현 대기)

`docs/specs/2026-09-04-admin-product-management.md`(status: confirmed), `docs/plans/2026-09-04-admin-product-management.md`(status: planned). 브랜치 `feat/admin-product-management`.

**그릴링 결과 범위를 2개로 쪼갰다.** 이번 spec(A)은 **관리자 기반 + 상품 관리**만 다루고, **주문 배송상태·송장과 쿠폰 발급은 후속 spec(B)**으로 분리했다. A가 B의 선행(인가 체계가 없으면 B도 못 만듦)이고, A만 끝나도 "신상품을 재배포 없이 등록한다"는 운영 병목이 실제로 풀리기 때문이다.

**그릴링에서 확인한 사실 3가지** — `Role.ADMIN`은 enum 값으로만 존재하고 참조 0건인 죽은 코드이며 ADMIN을 만들 경로가 없다 / JWT가 권한을 싣지 않고 `JwtAuthenticationFilter:38`이 `ROLE_USER`를 하드코딩해 인가를 바닥부터 세워야 한다 / `Product.stock`은 단일 정수라 사이즈 개념이 데이터에 아예 없다.

**주요 결정** — `/admin`은 같은 Next.js 앱의 `(shell)` 밖 라우트 / JWT `role` 클레임 + `hasRole("ADMIN")` / `ProductVariant` 도입(사이즈 없는 상품도 `size = null` 단일 variant로 통일) / `soldOut`을 `status` enum으로 대체하고 품절은 재고 합에서 파생 / Cloudinary signed upload 최대 5장 / 검색은 `name` LIKE만 / 관리자 승격은 flyway placeholder + 환경변수 `MOMENTIVE_ADMIN_EMAIL`(**리포지토리가 public이라 실제 이메일을 커밋하지 않는다**)

- [ ] Phase 1: 인가 기반 (JWT role 클레임, `hasRole`, `/auth/me`에 role, `V13` 승격 마이그레이션)
- [ ] Phase 2: `ProductVariant` 도입 + 데이터 이관 + 재고/주문 로직 이전 — **이미 배포된 주문 데이터를 건드리는 유일한 구간이라 독립 phase로 격리.** `V14`~`V16`
- [ ] Phase 3: 관리자 API (상품 CRUD, Cloudinary 서명, `GET /products`에 `q` 추가)
- [ ] Phase 4: 관리자 화면 2개 + `/admin/layout.tsx` 접근 보호
- [ ] Phase 5: 고객 화면 반영 (상품상세 variant 연동, `/search` 서버 검색 전환, `CartItem`에 `variantId`)
- [ ] Phase 6: E2E 검증
- [ ] **착수 전 필요**: Cloudinary 계정과 `CLOUD_NAME`/`API_KEY`/`API_SECRET` — 없으면 Phase 3의 서명 발급 검증과 Phase 4의 실업로드에서 막힌다
- [ ] **`chore/admin-login-seed`는 폐기 결정.** Mac에만 있을 가능성이 큰 브랜치로, 이번 spec이 채택한 승격 방식(계정을 새로 만들지 않고 기존 계정의 role만 올림)과 방식이 다르고 마이그레이션 재번호 비용이 새로 쓰는 것보다 크다. Mac에서 발견하면 삭제할 것

#### 후속 spec(B)로 분리된 항목

- [ ] **주문 배송상태 · 송장 관리** — 3단계 배송조회의 선행 조건. `OrderStatus`는 현재 `PENDING/PAID/FAILED/CANCELLED` 4종뿐
- [ ] **쿠폰 발급 API** — 현재 flyway 시드가 유일한 발급 경로. 생기면 "쿠폰 AC 7개 미검증"도 함께 해소된다

### 3단계 — 배송조회

- [ ] 마이페이지 메뉴 5개 중 유일하게 남은 무동작 항목(`mypage/page.tsx:16`). 2단계에서 배송상태·송장이 생긴 뒤 착수한다. 송장을 넣을 관리자 수단 없이 먼저 만들면 주문마다 DB를 직접 건드려야 해서 반쪽이 된다

### 4단계 — 화면 품질 정리

- [ ] **"인기순"을 실제 인기순으로** — `ProductService.java:40`이 `case NEW, POPULAR -> createdAt DESC`. 검색 정렬의 "인기순"과 홈 "지금 인기 있는" 섹션(라벨은 "리뷰 많은순")이 전부 최신순이다. 리뷰 기능으로 `reviewCount`/`averageRating`이 이미 DB에 있으므로 지금은 진짜로 만들 수 있다
- [ ] **장바구니·주문서·주문상세 썸네일** — 세 화면 모두 상품 이미지가 회색 사각형이다(`cart:139`, `checkout:330`, `orders/[orderId]:194`). `CartItem` 타입에 썸네일 필드가 없고 주문 응답에도 없다. 결제 직전 화면에서 무엇을 사는지 그림으로 확인이 안 된다
- [ ] **홈 프로모 배너** — "WINTER SALE / 겨울 신상 최대 20%"가 하드코딩 정적 텍스트이고 클릭도 안 된다(`(shell)/page.tsx:139`). 실제 할인과 연결돼 있지 않다
- [ ] **주문내역 페이지네이션** — `getOrders()`가 전체를 한 번에 받는다
- [ ] **장바구니 수량 재고 상한** — 재고 3개짜리를 99개까지 올릴 수 있고, 결제 버튼을 눌러야 실패를 안다
- [ ] **상품 조회 실패 처리** — 홈/검색/위시리스트의 `getProducts`에 `.catch`가 없어(`(shell)/page.tsx:42`, `search:54`, `wishlist:23`) API가 실패하면 스켈레톤이나 "검색 중"에서 영원히 멈춘다
- [ ] **장바구니 가격 스냅샷** — `cart.ts`의 `unitPrice`가 담을 때 값으로 고정이라 이후 가격이 바뀌어도 반영되지 않는다

### 5단계 — 잔여

- [ ] **우편번호 형식 검증 부재** — `AddressRequest.zipcode`가 `@NotBlank`만 있는 자유 텍스트(2026-08-31 발견). 배송비 spec에서는 제주 판정 시 파싱 실패를 "제주 아님"으로 안전 처리하고 정식 검증은 범위 밖으로 뒀다. 기존 저장 데이터 하위호환 고려 필요
- [ ] **인기 검색어 하드코딩** — `search/page.tsx:25`, 집계 인프라 없음
- [ ] **상품 실사진 부재** — `V2__seed_product.sql`의 이미지가 전부 `picsum.photos` 랜덤 스톡 사진. 실제 상품 사진이 하나도 없다. 2단계 관리자 기능에 이미지 업로드가 포함되면 함께 해소
- [ ] **시드 상품 카테고리 쏠림** — 15개 대부분이 ACCESSORY이고 KNIT/INNERWEAR 상품이 없어 카테고리 필터가 밋밋하다. 실제 상품 등록으로 자연 해소
- [ ] **적립금 도입 여부 결정** — 2026-09-03에 마이페이지 메뉴에서 내렸다(`mypage/page.tsx`, 복원 방법은 해당 파일 주석). 백엔드 도메인 0건. 제도를 실제로 운영할지는 사업 판단이며, 도입하면 별도 spec 대상

---

## 유저 화면 전수 점검 (2026-09-03)

"소비자가 보는 구현이 정말 다 됐는지"를 무동작 버튼 수준이 아니라 **화면 기능 자체** 관점에서 18개 라우트 + 공통 컴포넌트를 전수 확인한 결과. 12건 발견. 각 항목의 처리 단계는 위 실행 순서에 배치했다.

**실제 버그 (1단계)**
1. 결제 성공 후 장바구니가 비워지지 않음 — 중복 구매 유발
2. 상품상세 배송비 안내 3,000원 ↔ 실제 정책·FAQ 4,000원

**기능이 가짜 (2·4단계)**
3. "인기순" 정렬이 실제로는 최신순, 홈 "리뷰 많은순" 라벨도 거짓
4. 인기 검색어 3개 하드코딩
5. 백엔드 검색 API 부재 — 프론트가 100개 받아 필터링. **상품 100개 초과 시 검색 누락**, 위시리스트도 동일 캡(101번째 상품은 찜해도 목록에서 사라짐)
6. 사이즈 S/M/L/XL을 전 상품에 동일 노출, 사이즈별 재고 없음 — 목줄·간식에도 사이즈 선택이 뜨고 "M만 품절"을 표현할 수 없음
7. 장바구니 수량에 재고 상한 없음
8. 장바구니 가격 스냅샷 — 가격 변동 미반영

**비어있는 자리 (4·5단계)**
9. 장바구니·주문서·주문상세 상품 이미지가 전부 회색 박스
10. 홈 프로모 배너가 정적 텍스트, 클릭 불가
11. 상품 이미지가 전부 랜덤 스톡 사진
12. 주문내역 페이지네이션 없음

부수: 홈/검색/위시리스트 상품 조회에 `.catch`가 없어 API 실패 시 로딩 상태에서 멈춤.

---

## 완료 이력

상세는 각 spec/plan 파일과 PR 본문 참조. 미해결 잔여가 있는 건만 비고에 남긴다.

| 기능 | spec / plan (`docs/`) | PR | 비고 |
|---|---|---|---|
| 앱 전체 재디자인 (7개 화면) | `2026-08-26-app-redesign` | #3 | |
| plan-runner 자동 사이클 | `2026-08-26-plan-runner-cycle` | #2 | `e2e-tester`·`plan-runner` 에이전트 신설 |
| 백엔드/프론트 컨벤션 정비 | (spec 없음) | — | `backend/CLAUDE.md` `859210b`, `frontend/CLAUDE.md` `8e15b64` |
| 로그인/회원가입 (Auth) | `2026-08-27-auth` | #5 | JWT Access 30분 / Refresh 14일 rotation |
| E2E 단일 탭 순차 실행 전환 | (규격 개정) | #6 | `.claude/rules/e2e-format.md` |
| 장바구니→주문→결제 (토스페이먼츠) | `2026-08-29-cart-order-payment` | #7 | **Toss 실연동 미완** (아래 참조) |
| Swagger(OpenAPI) 문서화 | `2026-08-30-api-documentation` | #8 | springdoc 2.8.9 (2.8.17은 Boot 3.4.1과 충돌) |
| 다음 작업 후보 정리 | (조사 문서) | #9, #12 | |
| 상품 리뷰 (조회 + 작성) | `2026-08-30-product-review` | #10 | 5개 화면 평점을 실집계값으로 교체 |
| 마이페이지 메뉴 정리 (반려견/고객센터) | `2026-08-31-mypage-menu-cleanup` | #11 | |
| 배송비 정책 반영 | `2026-08-31-shipping-fee-policy` | #13 | 3,400원 / 7만원↑ 무료 / 제주 +4,000원 |
| 쿠폰 시스템 | `2026-09-01-coupon-system` | #14 | **spec AC 13/20** (아래 참조) |
| 디자인 핸드오프 2차 이관 | `2026-09-02-responsive-design-handoff` | #15 | 폰트·모션·데스크톱 반응형, `docs/design.md` 신규 |

### 완료 섹션에 묻혀 있던 미해결 잔여

아래 3건은 해당 기능이 "완료"로 처리됐지만 실제로는 남아 있는 항목이라 별도로 승격해 둔다.

- [ ] **Toss 결제위젯 실연동** — 클라이언트 키가 결제위젯 API(`widget-groups/keys`)에서 401. Toss 개발자센터에 계정만 만들고 상점(스토어) 등록(사업자 정보 필요)을 하지 않은 것이 원인으로 추정. 사업자 등록 확인 후 재검증 필요. 상세: `docs/backlog/2026-08-30-cart-order-payment-phase4-01.md`. **사용자 측 외부 조치 대기 중** — 상점 등록이 끝나면 위젯 렌더링~confirm 성공~`PAID` 취소까지 마무리 검증
- [ ] **쿠폰 spec AC 7개 미검증** — `docs/specs/2026-09-01-coupon-system.md`가 `status: confirmed`로 남아 있다(13/20 체크). 미체크분은 백엔드 테스트로만 커버되거나(복원 3경로, 할인액 초과, 서버 재검증) 환경 제약(Toss confirm, V11 이전 주문 재현 불가)이다. **만료 쿠폰 시드가 없어 이 영역이 계속 회귀 검증 사각지대**로 남는 것이 핵심 문제 — 2단계 관리자 발급 API가 생기면 함께 해소
- [ ] **`chore/admin-login-seed` 브랜치 행방** — 이 머신에도 origin에도 없다(2026-09-03 확인). admin 로그인 + `V9__seed_admin_user.sql` 포함. Mac에 커밋만 되고 푸시 안 된 상태로 추정. 2단계 착수 전 확인 필요, 살릴 경우 `V13`으로 재번호

### 반복적으로 발목을 잡은 환경 이슈 (참고)

같은 문제로 두 번 이상 시간을 쓴 것들. 새 세션에서 비슷한 증상이 보이면 여기부터 확인한다.

- **로컬 dev DB가 비어있는데 flyway 이력은 `success`** — 여러 번 재발. `docker compose down -v` → `./dev.sh` 재기동으로 재시딩하면 해소
- **테스트가 dev DB를 공유·wipe** — 별도 테스트 DB가 없어 `./gradlew test`를 돌리면 E2E 시딩값이 날아간다
- **백엔드 hot-reload 없음** — devtools가 없어 `bootRun`이 코드 변경을 반영하지 않는다. `e2e-tester`가 `backend/` 변경 시 백엔드를 스스로 재기동하도록 절차화됨
- **JDK 21** — 이 머신은 `C:\Users\tyo10\.jdks\graalvm-jdk-21.0.7`. `dev.sh`가 OS별 표준 경로를 자동 탐지한다
- **좀비 백엔드 프로세스(8081 점유)** — Windows에 `lsof`가 없어 종료 시 정리가 실패하던 문제. `dev.sh`가 `netstat -ano` + `taskkill`로 처리하도록 수정됨
