# 모멘티브 백엔드

Spring Boot. Railway 배포 (Root Directory: `backend/`, Watch Paths: `backend/**`).

## 스택
- Java, Spring Boot, JPA
- DB: PostgreSQL (로컬은 docker-compose, 배포는 Railway managed)

## 컨벤션 (첫 기능 구현하며 채워나갈 것 — 지금은 뼈대만)
- 계층: Controller / Service / Repository
- DTO ↔ Entity 분리
- 예외: CustomException(ErrorCode) 패턴
- 트랜잭션: @Transactional 경계는 Service에만
- Entity: Lombok 사용 (`@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`)로 보일러플레이트 제거. Setter는 두지 않고, 특정 필드만 받는 생성자·도메인 메서드는 직접 작성한다

## 테스트
- 컨트롤러(MockMvc) 테스트보다 서비스 로직 테스트를 우선한다 — 컨트롤러는 서비스에 위임만 하는 얇은 계층이므로, 서비스 단위/통합 테스트로 커버리지를 확보한다
- 새 기능 구현 시 컨트롤러 테스트는 원칙적으로 작성하지 않는다. 이미 있는 컨트롤러 테스트가 서비스 테스트로 대체 가능하면 삭제한다

## 검증 방법
- `./gradlew test`로 단위 테스트 실행
- `/health` 엔드포인트로 기동 확인
