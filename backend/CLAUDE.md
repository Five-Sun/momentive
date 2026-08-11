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

## 검증 방법
- `./gradlew test`로 단위 테스트 실행
- `/health` 엔드포인트로 기동 확인
