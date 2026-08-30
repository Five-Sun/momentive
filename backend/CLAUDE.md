# 모멘티브 백엔드

Spring Boot. Railway 배포 (Root Directory: `backend/`, Watch Paths: `backend/**`).

## 스택
- Java, Spring Boot, JPA
- DB: PostgreSQL (로컬은 docker-compose, 배포는 Railway managed)

## 컨벤션

아래 **필수** 항목은 backend reviewer의 실패 판정 기준이다. **권장** 항목은 simplification/efficiency 제안으로 보고하되, correctness나 필수 컨벤션 위반이 아니면 단독으로 phase를 실패시키지 않는다.

### 필수
- 계층 분리: Controller는 요청/응답 매핑과 검증만 담당하고, 비즈니스 로직은 Service에 둔다. Controller가 Repository를 직접 호출하지 않는다.
- Repository 책임: Repository는 영속성 접근만 담당한다. 비즈니스 규칙, 응답 DTO 조립, 트랜잭션 시나리오는 Service로 올린다.
- DTO ↔ Entity 분리: Controller 응답이나 요청 타입으로 Entity를 직접 노출하지 않는다. API 경계에는 request/response DTO를 둔다.
- 예외 처리: 도메인/API 오류는 `CustomException(ErrorCode)` 패턴을 따른다. 임의의 런타임 예외나 문자열 메시지 직접 throw로 API 오류를 표현하지 않는다.
- 트랜잭션: `@Transactional` 경계는 Service에만 둔다. 읽기 전용 조회는 `@Transactional(readOnly = true)`를 우선한다.
- Entity: Lombok `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 사용한다. Setter는 두지 않고, 특정 필드만 받는 생성자·정적 팩토리·도메인 메서드로 상태를 변경한다.
- Controller/Service 의존성 주입: Lombok `@RequiredArgsConstructor` + `private final` 필드를 사용한다. 수동 생성자를 직접 작성하지 않는다. (Entity는 예외 — 불변식 보호를 위해 위 항목대로 수동 생성자·정적 팩토리를 유지한다)
- API 계약: spec에 명시된 status code, error code, query parameter 기본값, 정렬/필터 조건을 Service 테스트 또는 명확한 정적 코드 경로로 검증 가능하게 유지한다.

### 권장
- 단순 조회라도 조건/정렬이 늘어나면 테스트 데이터를 통해 결과 순서를 검증한다.
- 반복되는 DTO 변환이나 금액/상태 계산은 기존 유틸 또는 도메인 메서드로 모은다.
- N+1 가능성이 있는 연관 데이터 조회는 fetch join, EntityGraph, projection, batch size 중 현재 구조에 맞는 방식을 검토한다.

### 인증/인가
- 인증 방식: JWT(Access + Refresh Token). Access Token 만료 30분, Refresh Token 만료 14일이며 사용할 때마다 재발급(rotation)한다.
- 토큰 전달: httpOnly + Secure 쿠키에 담는다. 프론트(Vercel)와 백엔드(Railway, `api.모멘티브도메인`)가 같은 상위 도메인의 서브도메인 관계이므로 `SameSite=Lax`로 안전하게 처리한다. localStorage에 토큰을 저장하지 않는다.
- Spring Security를 도입한다. JWT 검증은 커스텀 `OncePerRequestFilter` + `SecurityFilterChain`으로 구성한다.
- Refresh Token은 PostgreSQL `refresh_token` 테이블에 기록해 관리한다(Redis 등 별도 저장소는 도입하지 않는다 — 단일 인스턴스 배포라 분산 상태 공유가 필요 없고 인프라 비용도 늘어나지 않는다). 로그아웃/재사용 시 무효화(revoke) 가능해야 한다.
- 인가 모델은 `USER`/`ADMIN` 최소 역할만 둔다. 실제 admin 전용 API는 이 컨벤션의 범위가 아니며 별도 spec에서 다룬다.
- Controller에서 현재 로그인 사용자를 받을 때는 커스텀 `@CurrentUser` 어노테이션 + `HandlerMethodArgumentResolver`를 사용한다. Service가 `SecurityContextHolder`에 직접 의존하지 않는다.
- 인증 실패(401)/인가 실패(403)도 기존 `CustomException`/`ErrorCode`/`ErrorResponse` 포맷을 그대로 따른다. `AuthenticationEntryPoint`/`AccessDeniedHandler`를 커스텀 구현해 동일한 `ErrorResponse`로 직렬화한다.
- 비밀번호는 `BCryptPasswordEncoder`로 해싱한다.
- 쿠키 기반 인증을 붙일 때는 `WebConfig`의 CORS 설정에 `allowCredentials(true)`를 추가해야 한다(현재는 없음).

### 결제 연동(토스페이먼츠)
- 결제 흐름: 주문 생성 시 `Order`를 `PENDING`으로 만들고 재고를 즉시 선점(차감)한다. DB 트랜잭션을 커밋한 뒤(락을 들고 있지 않은 상태로) 토스 confirm API를 서버-투-서버로 호출한다. 승인 성공 시 `PAID`로, 실패/타임아웃 시 `FAILED`로 전이하고 선점한 재고를 원복한다.
- `PENDING` 주문 만료: `@Scheduled` 배치로 일정 시간 지난 `PENDING` 주문을 주기적으로 만료 처리하고 재고를 원복한다. 배치 주기 사이의 공백은 조회 시점 lazy 체크로 보완한다.
- confirm API 호출: 타임아웃은 5초로 짧게 두고 재시도하지 않는다(이중 승인 위험). 실패/타임아웃 시 즉시 `FAILED` 처리하지 않고 "확인 필요" 상태로 남긴 뒤, 재조회로 실제 상태를 확인하는 별도 로직(세부 구현은 Order/결제 feature spec에서 결정)으로 정리한다.
- 시크릿 관리: 샌드박스/실결제 키는 profile(`application-{profile}.yml`)로 분리하고 값은 환경변수로 주입한다. 코드나 설정 파일에 키를 하드코딩하지 않는다.
- 외부 API 호출은 `payment.client` 패키지의 `PaymentGatewayClient` 인터페이스 뒤에 둔다. `TossPaymentGatewayClient` 구현체가 실제 HTTP 호출(Spring `RestClient`)을 담당하고, `PaymentService`는 인터페이스에만 의존해 테스트에서 대체 가능하게 한다.
- 토스 원본 에러코드를 우리 `ErrorCode`에 1:1로 매핑하지 않는다. 소수의 결제 실패 계열 코드(`PAYMENT_FAILED`, `PAYMENT_CONFIRM_TIMEOUT` 등)로 단순화하고, 토스 원본 코드는 로그와 `ErrorResponse`의 상세 필드에만 남긴다.
- 웹훅 수신은 이번 컨벤션 범위 밖이다. 동기 confirm 흐름만으로 결제 완료를 확정한다.

### Write API 검증
- 형식적 검증(null/blank, 길이, 형식, 범위 등)은 요청 DTO에 Bean Validation 어노테이션(`@NotBlank`, `@Email` 등)과 컨트롤러의 `@Valid`로 처리한다.
- DB 조회가 필요한 검증(중복 여부, 재고 초과, 가격 위변조 등 비즈니스 규칙)은 Bean Validation으로 처리하지 않고 Service에서 `CustomException(ErrorCode)`로 처리한다.
- `MethodArgumentNotValidException`은 `GlobalExceptionHandler`에서 `ErrorCode.VALIDATION_FAILED`로 매핑하고, `ErrorResponse`에 필드별 상세(`fieldErrors: Map<String, String>`)를 포함한다.

### 동시성/락
- 동시 갱신 대상 필드(재고 등)가 있는 Entity에는 JPA `@Version` 낙관적 락을 적용한다.
- 낙관적 락 충돌(`OptimisticLockException`) 시 Service에서 직접 for-loop로 최대 2회까지 재조회 후 재시도한다. Spring Retry 등 별도 의존성은 추가하지 않는다.
- 2회 재시도 후에도 충돌하면 `ErrorCode.STOCK_CONFLICT`로 즉시 실패 응답한다. 무한 재시도하지 않는다.
- Redis 등 분산 락은 도입하지 않는다(단일 인스턴스 배포 기준). 여러 인스턴스로 수평 확장하게 되면 그 시점에 재검토한다.

### Swagger/OpenAPI (springdoc)
- 모든 컨트롤러 메서드(엔드포인트)에는 `@Operation(summary = "...")`를 필수로 작성한다.
- 모든 요청/응답 DTO의 필드에는 `@Schema(description = "...")`를 필수로 작성한다.
- 인증이 필요한 엔드포인트(또는 컨트롤러 클래스 전체)에는 `@SecurityRequirement`를 필수로 작성한다.
- 에러 응답(`@ApiResponse`)은 선택 사항이며, 강제하지 않는다.
- OpenAPI `Info` 메타정보: title "모멘티브 API", version 고정 문자열 `"v1"`, description은 한 줄 또는 생략.
- 그룹 분리는 하지 않는다(단일 그룹). Admin 전용 도메인이 실제로 생기면 그룹 분리를 재검토한다.

## 테스트
- 컨트롤러(MockMvc) 테스트보다 서비스 로직 테스트를 우선한다 — 컨트롤러는 서비스에 위임만 하는 얇은 계층이므로, 서비스 단위/통합 테스트로 커버리지를 확보한다
- 새 기능 구현 시 컨트롤러 테스트는 원칙적으로 작성하지 않는다. 이미 있는 컨트롤러 테스트가 서비스 테스트로 대체 가능하면 삭제한다

## 검증 방법
- `./gradlew build`로 컴파일/패키징까지 확인
- `./gradlew test`로 단위 테스트 실행
- `/health` 엔드포인트 기동 확인, 실제 API 호출, 외부 연동 확인은 수동 검증 또는 별도 QA 단계에서 수행
