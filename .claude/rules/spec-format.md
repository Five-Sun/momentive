# Spec 작성 규격

`grillme`/`grilling` 세션이 끝나고 사용자 컨펌을 받은 뒤 작성하는 spec 문서의 규격이다. 아래 필드/섹션 정의를 스키마로 취급하고 정확히 따른다.

## 파일 위치 및 이름

```
docs/specs/YYYY-MM-DD-<feature-slug>.md
```

- `YYYY-MM-DD`: spec이 컨펌된 날짜
- `<feature-slug>`: 기능을 나타내는 영문 kebab-case (예: `user-signup`, `order-refund-api`)
- 세션 1회 = 기능 1개 = 파일 1개가 원칙. 세션 도중 여러 기능이 섞이면 그릴링 중에 별도 세션으로 분리할지 사용자에게 확인한다.
- 동일 기능을 나중에 다시 그릴링해 요구사항이 바뀌면, 기존 파일은 수정하거나 삭제하지 않고 새 날짜로 새 파일을 추가한다 (이력 누적). 최신 버전은 파일명의 날짜로 판별한다.

## Frontmatter

파일 최상단에 YAML frontmatter를 넣는다.

```yaml
---
date: YYYY-MM-DD
feature: <feature-slug>
status: confirmed
supersedes: <이전 spec 파일명>  # 이 기능의 이전 버전 spec이 있을 때만 포함, 없으면 필드 자체를 생략
---
```

| 필드 | 타입 | 필수 | 값 | 설명 |
|---|---|---|---|---|
| `date` | string (`YYYY-MM-DD`) | 필수 | - | 컨펌된 날짜, 파일명의 날짜와 동일 |
| `feature` | string (kebab-case) | 필수 | - | 파일명의 slug와 동일 |
| `status` | enum | 필수 | `confirmed` \| `implemented` | 작성 시점엔 항상 `confirmed`. 구현이 끝나면 사용자가 수동으로 `implemented`로 변경 |
| `supersedes` | string (파일명) | 선택 | - | 이 spec이 대체하는 이전 spec 파일명. 최초 작성이면 생략 |

## 본문 섹션

아래 5개 섹션을 이 순서대로, 이 제목 그대로 포함한다. 섹션을 생략하지 않는다 — 해당 사항이 없으면 "해당 없음"이라고 명시한다.

### 1. 목적 (Why)

이 기능/작업을 왜 만드는지, 어떤 문제를 푸는지 서술. 배경이 되는 사용자 불편, 비즈니스 이유 등.

### 2. 범위 (Scope)

- **In Scope**: 이번 spec에서 다루는 것
- **Out of Scope**: 명시적으로 다루지 않는 것 (나중에 별도 spec으로 다룰 수 있는 것 포함)

### 3. 사용자 시나리오

사용자가 이 기능을 겪는 과정을 단계별로 서술. 화면 흐름, 상태 전이, 예외 케이스(에러/엣지 케이스) 포함.

### 4. 인터페이스

이 spec이 API 하나, 화면 하나, 도메인 전체 등 무엇이든 될 수 있다는 전제 하에, **이 spec이 실제로 다루는 경계만** 작성한다. 다루지 않는 하위 섹션은 통째로 생략한다 (예: API만 다루는 spec이면 화면 섹션 자체를 넣지 않는다).

- **API**: 엔드포인트, request/response 형태, 에러 코드
- **화면**: 화면/컴포넌트 단위 요구사항, 상태별 UI
- **데이터 모델**: 엔티티, 필드, 관계, 제약조건

### 5. 수용 기준 (Acceptance Criteria)

체크 가능한 형태로 작성한다 (CLAUDE.md 원칙).

```markdown
- [ ] 조건 1
- [ ] 조건 2
```

## 템플릿

```markdown
---
date: YYYY-MM-DD
feature: <feature-slug>
status: confirmed
---

# <기능명>

## 목적 (Why)

## 범위 (Scope)

### In Scope
-

### Out of Scope
-

## 사용자 시나리오

## 인터페이스

### API
(해당 없으면 섹션 생략)

### 화면
(해당 없으면 섹션 생략)

### 데이터 모델
(해당 없으면 섹션 생략)

## 수용 기준 (Acceptance Criteria)

- [ ]
```
