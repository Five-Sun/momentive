---
date: 2026-08-30
feature: api-documentation
status: implemented
---

# Swagger(OpenAPI) API 문서화

## 목적 (Why)

백엔드 API가 도메인마다 하나씩 늘어나고 있는데(현재 Auth/Product/Address/Order), API 명세를 별도 문서로 관리하지 않아 엔드포인트/요청·응답 구조를 파악하려면 컨트롤러와 DTO 코드를 직접 읽어야 한다. 혼자 개발하는 프로젝트라 지금은 크게 불편하지 않지만, AI 에이전트 팀으로 구현을 이어가는 체제상 "코드를 새로 만들 때마다 자연스럽게 최신 API 문서가 같이 생성되는" 상태를 만들어두는 것이 장기적으로 유지비용을 줄인다.

단발성으로 지금 있는 API만 문서화하고 끝내는 것이 아니라, **앞으로 백엔드 컨트롤러를 작성할 때마다 Swagger 애노테이션이 항상 함께 작성되도록 컨벤션과 자동 검증을 고정하는 것**이 이 작업의 핵심 목적이다.

## 범위 (Scope)

### In Scope
- springdoc-openapi 의존성 도입 및 기본 설정 (`springdoc-openapi-starter-webmvc-ui:2.8.17` — 실제 구현 시 2.8.9로 조정됨, 하단 AC 참고)
- 기존 API 전체(Auth/Product/Address/Order, 총 4개 컨트롤러)에 Swagger 애노테이션 소급 적용
- `backend/CLAUDE.md`에 "컨트롤러/DTO 작성 시 Swagger 애노테이션 필수" 컨벤션 추가
- `backend-reviewer.md` 체크리스트에 Swagger 애노테이션 검증 항목 추가 — 이후 새로 추가되는 API도 리뷰 단계에서 누락이 자동으로 걸러지게 함
- `SecurityConfig`에 Swagger UI/OpenAPI JSON 경로 permitAll 추가

### Out of Scope
- Admin 전용 API 그룹 분리(`GroupedOpenApi`) — admin 도메인이 실제로 생기면 별도로 다룸
- profile별(dev/prod) 노출 제어 — 지금은 profile 분리 인프라 자체가 없고, 이번 spec에서도 운영 환경 노출을 그대로 허용하기로 결정했으므로 범위 밖
- Swagger UI에서 인증(쿠키/토큰) 기반 "Try it out" 호출 테스트를 위한 별도 설정 — 문서 열람이 목적이며, 브라우저 세션이 있으면 되는 대로 두고 추가 설정하지 않음
- 에러 응답(`@ApiResponse`) 명시 — `ErrorCode`/`GlobalExceptionHandler`로 이미 일관되게 처리되는 영역이라 문서화 필수 대상에서 제외(선택 사항으로 남김)
- API 버전 관리 체계 도입 — `version` 필드는 고정 문자열로만 사용

## 사용자 시나리오

### 1. 기존 API 문서 확인
1. 개발자(본인 또는 AI 에이전트)가 `/swagger-ui/index.html`에 접속하면 모멘티브 API 전체 목록이 단일 그룹으로 보인다.
2. 각 엔드포인트를 펼치면 한 줄 요약(`@Operation` summary), 요청/응답 DTO의 필드별 설명(`@Schema`), 인증 필요 여부(자물쇠 아이콘, `@SecurityRequirement`)를 확인할 수 있다.
3. Product처럼 인증이 필요 없는 API는 자물쇠 아이콘이 없고, Address/Order/`/auth/me`처럼 인증이 필요한 API는 자물쇠 아이콘이 표시된다.

### 2. 새 API 추가 시 문서화 강제
1. 개발자가 새 컨트롤러/엔드포인트를 추가하면서 `@Operation`/`@Schema`/`@SecurityRequirement` 애노테이션을 빠뜨린다.
2. `backend-reviewer` 에이전트가 해당 phase를 검증할 때 이 누락을 감지해 실패로 판정하고, `docs/backlog/`에 원인을 기록한다.
3. 개발자가 애노테이션을 보완한 뒤 재검증을 통과한다.

### 3. 운영 환경에서의 접근
1. Railway에 배포된 실제 서버(`api.모멘티브도메인`)에서도 `/swagger-ui/index.html`, `/v3/api-docs`에 별도 인증 없이 접근할 수 있다 (profile 분리 없이 permitAll).

## 인터페이스

### API

Swagger가 노출하는 신규 경로 (springdoc 기본값, 커스텀 없음):
- `GET /swagger-ui/index.html` — Swagger UI 화면
- `GET /v3/api-docs` — OpenAPI 3.0 JSON 명세

`SecurityConfig`의 permitAll 목록에 `/swagger-ui/**`, `/v3/api-docs/**` 추가.

### 데이터 모델

해당 없음 (문서화 작업이며 도메인 모델 변경 없음).

### 컨벤션 (backend/CLAUDE.md에 반영될 내용)

- 모든 컨트롤러 메서드(엔드포인트)에는 `@Operation(summary = "...")`를 필수로 작성한다.
- 모든 요청/응답 DTO의 필드에는 `@Schema(description = "...")`를 필수로 작성한다.
- 인증이 필요한 엔드포인트(또는 컨트롤러 클래스 전체)에는 `@SecurityRequirement`를 필수로 작성한다.
- 에러 응답(`@ApiResponse`)은 선택 사항이며, 강제하지 않는다.
- OpenAPI `Info` 메타정보: title "모멘티브 API", version 고정 문자열 `"v1"`, description은 한 줄 또는 생략.
- 그룹 분리는 하지 않는다(단일 그룹). Admin 전용 도메인이 실제로 생기면 그룹 분리를 재검토한다.

## 수용 기준 (Acceptance Criteria)

- [x] `build.gradle.kts`에 springdoc-openapi 의존성이 추가되고 빌드가 통과한다 — 계획한 `2.8.17`은 Spring Boot 3.4.1과 `PatternParseException`을 일으키는 알려진 회귀(springdoc-openapi#3210, 2.8.15+에서 발생)라 `2.8.9`로 조정해 적용(`docs/plans/2026-08-30-api-documentation.md` Phase 1 참고)
- [x] `SecurityConfig`의 permitAll 목록에 `/swagger-ui/**`, `/v3/api-docs/**`가 추가되어 인증 없이 접근 가능하다
- [x] `/swagger-ui/index.html` 접속 시 Auth/Product/Address/Order 4개 도메인의 전체 엔드포인트가 단일 그룹으로 노출된다
- [x] 모든 엔드포인트에 `@Operation` summary가 작성되어 Swagger UI에 한 줄 설명으로 표시된다
- [x] 모든 요청/응답 DTO 필드에 `@Schema` description이 작성되어 Swagger UI에 필드 설명으로 표시된다
- [x] 인증이 필요한 엔드포인트(Address 전체, Order 전체, `/auth/me`)는 Swagger UI에서 자물쇠 아이콘으로 구분되고, 인증이 필요 없는 엔드포인트(Product 전체, `/auth/signup`·`/auth/login`·`/auth/logout`·`/auth/refresh`)는 표시되지 않는다
- [x] Swagger UI 상단에 title "모멘티브 API", version "v1"이 표시된다
- [x] `backend/CLAUDE.md`에 위 "컨벤션" 섹션 내용이 반영된다
- [x] `backend-reviewer.md` 체크리스트에 Swagger 애노테이션(`@Operation`/`@Schema`/`@SecurityRequirement`) 누락 여부를 검증하는 항목이 추가된다
