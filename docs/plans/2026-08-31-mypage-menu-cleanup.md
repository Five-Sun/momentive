---
date: 2026-08-31
feature: mypage-menu-cleanup
spec: 2026-08-31-mypage-menu-cleanup.md
status: done
---

# 마이페이지 메뉴 정리 (반려견 프로필 관리 + 고객센터) 플랜

## 개요

`docs/specs/2026-08-31-mypage-menu-cleanup.md`를 기반으로 백엔드 Pet 도메인 → 프론트 반려견 프로필 화면 → 프론트 고객센터 화면+마이페이지 메뉴 연결 → E2E 검증 순서로 진행한다.

Pet은 Order/Review처럼 다른 도메인과 연동이 없는 완전 독립 도메인이라 백엔드를 하나의 phase로 묶는다. 소유권 검증은 `Address`의 `getOwnedAddress` 패턴을, 삭제는 `Review`의 소유권 검증 후 삭제 패턴을 그대로 따른다. 프론트는 CRUD 상태 관리가 있는 반려견 프로필 화면과, 백엔드가 전혀 없는 정적 고객센터 화면+메뉴 연결의 성격이 달라 phase를 분리한다. 반려견 프로필 폼/카드는 `/mypage/pets` 한 곳에서만 쓰이는 1회성 UI라 별도 재사용 컴포넌트로 추출하지 않고 페이지 파일 안에 둔다(고객센터 FAQ 아코디언도 동일한 이유로 페이지 내 로컬 상태로 구현).

## Phase 1: 백엔드 Pet 도메인 (CRUD)

이 phase가 끝나면 프론트 연동 없이도 curl/Postman으로 반려견 등록(이름만 필수)·조회·수정·삭제 전체 흐름과 소유권 검증(타인 반려견 접근 시 `FORBIDDEN`)을 검증할 수 있는 상태가 된다.

- [x] `PetGender` enum(`backend/src/main/java/com/momentive/backend/pet/domain/PetGender.java`): `MALE`, `FEMALE`
- [x] `Pet` 엔티티(`backend/src/main/java/com/momentive/backend/pet/domain/Pet.java`): `id`, `user`(`@ManyToOne` FK), `name`(String, not null), `breed`(String, nullable), `birthDate`(`LocalDate`, nullable), `gender`(`PetGender`, `@Enumerated(STRING)`, nullable), `weightKg`(Double, nullable), `createdAt`. `@Getter`, `@NoArgsConstructor(access = PROTECTED)`, 정적 팩토리 + `update(name, breed, birthDate, gender, weightKg)` 도메인 메서드로 상태 변경(Setter 없음), `isOwnedBy(userId)` 헬퍼(`Address`/`Review` 소유권 검증 패턴과 동일)
- [x] `backend/src/main/resources/db/migration/V7__create_pet.sql` 신규 작성(최신 마이그레이션은 `V6__create_review.sql`): `pet` 테이블(`user_id` FK, `name` not null, `breed` nullable, `birth_date` date nullable, `gender` varchar(10) nullable, `weight_kg` double precision nullable, `created_at`)
- [x] `PetRepository`(`backend/src/main/java/com/momentive/backend/pet/repository/PetRepository.java`): `findAllByUserIdOrderByCreatedAtDesc(Long userId)`
- [x] `PetService`(`backend/src/main/java/com/momentive/backend/pet/service/PetService.java`):
  - `getPets(userId)`: 요청 사용자 소유 반려견만 최신순 조회
  - `createPet(userId, request)`: 저장 후 생성된 엔티티 반환
  - `updatePet(userId, petId, request)`: 소유권 검증(`getOwnedPet` — 존재하지 않으면 `PET_NOT_FOUND`, 본인 소유가 아니면 `FORBIDDEN`), 검증 통과 시 `update` 호출
  - `deletePet(userId, petId)`: 동일한 소유권 검증 후 삭제
  - `@Transactional` 경계는 Service, 읽기 전용 조회는 `@Transactional(readOnly = true)`
- [x] `PetController`(`backend/src/main/java/com/momentive/backend/pet/controller/PetController.java`): spec "인터페이스 > API" 섹션 요청/응답 계약 그대로
  - `GET /pets` — 인증 필요, 목록 조회
  - `POST /pets`(201) — 인증 필요, 생성
  - `PATCH /pets/{petId}`(200) — 인증 필요, 수정
  - `DELETE /pets/{petId}`(204) — 인증 필요, 삭제
  - 전 엔드포인트 `@SecurityRequirement`, `@CurrentUser` 파라미터에 `@Parameter(hidden = true)`(`backend/CLAUDE.md` Swagger 컨벤션), 전 엔드포인트 `@Operation(summary = ...)`
- [x] `ErrorCode`(`backend/src/main/java/com/momentive/backend/common/exception/ErrorCode.java`)에 `PET_NOT_FOUND(HttpStatus.NOT_FOUND, ...)` 추가. `FORBIDDEN`은 기존 값 재사용
- [x] Request/Response DTO 신규(Entity 직접 노출 금지): `PetRequest`(`name` `@NotBlank`, `breed`, `birthDate`, `gender`, `weightKg`), `PetResponse`(단건), `PetListResponse`(`pets: List<PetResponse>`) — 전 필드 `@Schema(description = ...)` 필수
- [x] `PetServiceTest`(`backend/src/test/java/com/momentive/backend/pet/PetServiceTest.java`): 정상 등록/조회/수정/삭제, 이름만 입력해도 등록 성공, 삭제 후 같은 사용자가 재등록 가능, 본인 소유가 아닌 반려견 수정/삭제 시 `FORBIDDEN`, 존재하지 않는 `petId` 접근 시 `PET_NOT_FOUND`
- [x] 검증(자동): `./gradlew build`, `./gradlew test` 통과

## Phase 2: 프론트 반려견 프로필 관리 화면 (`/mypage/pets`)

이 phase가 끝나면 브라우저에서 `/mypage/pets`에 진입해 반려견을 등록·조회·수정·삭제할 수 있고, 이름 없이 저장을 시도하면 검증 에러가 표시되는 상태가 된다.

- [x] `frontend/src/lib/api/pets.ts` 신규: `getPets()`, `createPet(request)`, `updatePet(petId, request)`, `deletePet(petId)` — `apiFetch`(`src/lib/api/client.ts`) 경유, `Pet`/`PetListResponse` 등 타입 명시(`any` 없음)
- [x] `frontend/src/app/(shell)/mypage/pets/page.tsx` 신규:
  - 반려견이 없으면 빈 상태 안내 + "반려견 등록" 버튼
  - 반려견이 있으면 카드 목록(이름, 입력된 필드(품종/생일/성별/몸무게)만 표시, 품종/성별 기반 기본 아이콘 — 사진 없음) + 각 카드에 수정/삭제 버튼
  - "반려견 등록"/수정 버튼 클릭 시 같은 화면 내 인라인 폼 토글(React Hook Form + Zod, 이름만 `required`, 나머지 선택 입력) — 수정 시 기존 값이 채워진 채로 진입
  - 저장 성공 시 목록에 즉시 반영(재조회 또는 응답값 반영), 서버 `fieldErrors`는 `setError`로 매핑
  - 삭제는 확인(`window.confirm` 또는 기존 리뷰 삭제와 동일 패턴) 후 처리, 성공 시 목록에서 제거
- [x] 검증(자동): `npm run build`, `npm run lint` 통과
- [x] 검증(수동, 브라우저): 이름만 입력해 등록 → 목록 즉시 반영 확인, 이름 비운 채 저장 시도 → 검증 에러 확인, 반려견 수정 → 기존 값 채워진 폼 진입 및 갱신 확인, 삭제 → 목록에서 제거 및 삭제 후 재등록 가능 확인 (`docs/e2e/2026-08-31-mypage-menu-cleanup.md` 시나리오 1~5로 대체 확인)

## Phase 3: 프론트 고객센터 화면(`/mypage/support`) + 마이페이지 메뉴 연결

이 phase가 끝나면 마이페이지에서 "반려견 프로필 관리"/"고객센터" 두 메뉴가 각각 실제 화면으로 연결되고, 고객센터 화면에서 FAQ 아코디언과 인스타그램 연락처를 확인할 수 있는 상태가 된다.

- [x] `frontend/src/app/(shell)/mypage/support/page.tsx` 신규: FAQ 아코디언 4개(배송비 — 3,400원/7만원 이상 무료배송/제주·도서산간 추가 4,000원, 결제수단 — 토스페이먼츠 카드/간편결제, 교환/환불 — 7일 이내 청약철회·단순변심 왕복배송비 구매자 부담·하자/오배송 판매자 부담·접수는 인스타그램 DM, 회원가입/로그인 — 이메일/비밀번호 방식), 클릭 시 답변 펼침(페이지 내 로컬 상태, 별도 컴포넌트 추출 없음). 인스타그램 연락처 링크(`https://instagram.com/momentive_official`, `target="_blank" rel="noopener noreferrer"`)
- [x] `frontend/src/app/(shell)/mypage/page.tsx`의 `MENU_ITEMS` 중 "반려견 프로필 관리" 항목 `onClick`을 `() => router.push("/mypage/pets")`로, "고객센터" 항목 `onClick`을 `() => router.push("/mypage/support")`로 교체. "배송조회"/"쿠폰함"/"적립금" 3개 항목은 변경하지 않고 기존처럼 무동작 유지
- [x] 검증(자동): `npm run build`, `npm run lint` 통과
- [x] 검증(수동, 브라우저): 마이페이지에서 두 메뉴 클릭 시 각 화면으로 라우팅되는지 확인, FAQ 아코디언 토글 확인, 인스타그램 링크 클릭 시 새 탭에서 실제 프로필로 이동하는지 확인, 나머지 3개 메뉴가 여전히 무동작인지 확인(회귀 없음) (`docs/e2e/2026-08-31-mypage-menu-cleanup.md` 시나리오 6~8로 대체 확인)

## Phase 4: E2E 검증

Phase 3의 frontend-reviewer 승인 직후, `e2e-tester`가 spec `docs/specs/2026-08-31-mypage-menu-cleanup.md`의 사용자 시나리오(반려견 등록/조회/수정/삭제, 고객센터 FAQ+연락처)를 근거로 `docs/e2e/` 규격(`.claude/rules/e2e-format.md`)에 맞춰 케이스를 도출·실행한다.

- [x] `docs/e2e/YYYY-MM-DD-mypage-menu-cleanup.md` 작성 및 각 시나리오 실행, 전체 pass 확인 후 이 phase의 체크박스를 체크한다. 실패 시나리오가 있으면 `docs/backlog/` 규격대로 실패를 기록하고 이 phase는 미완료로 남긴다.
