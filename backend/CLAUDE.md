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
- API 계약: spec에 명시된 status code, error code, query parameter 기본값, 정렬/필터 조건을 Service 테스트 또는 명확한 정적 코드 경로로 검증 가능하게 유지한다.

### 권장
- 단순 조회라도 조건/정렬이 늘어나면 테스트 데이터를 통해 결과 순서를 검증한다.
- 반복되는 DTO 변환이나 금액/상태 계산은 기존 유틸 또는 도메인 메서드로 모은다.
- N+1 가능성이 있는 연관 데이터 조회는 fetch join, EntityGraph, projection, batch size 중 현재 구조에 맞는 방식을 검토한다.

## 테스트
- 컨트롤러(MockMvc) 테스트보다 서비스 로직 테스트를 우선한다 — 컨트롤러는 서비스에 위임만 하는 얇은 계층이므로, 서비스 단위/통합 테스트로 커버리지를 확보한다
- 새 기능 구현 시 컨트롤러 테스트는 원칙적으로 작성하지 않는다. 이미 있는 컨트롤러 테스트가 서비스 테스트로 대체 가능하면 삭제한다

## 검증 방법
- `./gradlew build`로 컴파일/패키징까지 확인
- `./gradlew test`로 단위 테스트 실행
- `/health` 엔드포인트 기동 확인, 실제 API 호출, 외부 연동 확인은 수동 검증 또는 별도 QA 단계에서 수행
