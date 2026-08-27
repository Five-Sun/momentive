# Plan 작성 규격

스펙(`docs/specs/`)이 확정된 뒤, 구현에 들어가기 전 `grillme`/`grilling` 세션으로 사용자 컨펌을 받고 작성하는 plan 문서의 규격이다. 아래 필드/섹션 정의를 스키마로 취급하고 정확히 따른다.

플랜은 스펙과 달리 "phase/step 단위 진행상황"을 담는 문서다. 내용(phase/step 구성)은 확정 후 불변이지만, 진행상황을 나타내는 필드(`status`, step 체크박스)는 실행하면서 갱신한다.

## 파일 위치 및 이름

```
docs/plans/YYYY-MM-DD-<feature-slug>.md
```

- `YYYY-MM-DD`: plan이 컨펌된 날짜
- `<feature-slug>`: 기반 스펙과 동일한 slug (`docs/specs/YYYY-MM-DD-<feature-slug>.md`)
- 스펙 1개 = 플랜 파일 1개가 원칙. 하나의 플랜 파일 안에 여러 phase를 섹션으로 담는다.

### 수정 계획 (relates_to)

플랜 확정 후 실행하다가 발견된 이슈나 추가로 필요해진 작업은 원본 플랜을 수정하지 않고 별도의 "수정 계획" 파일로 분리한다. 원본을 대체(supersede)하는 것이 아니라 원본에 딸린 추가 작업이므로, 파일명과 `relates_to` 필드로 원본과의 관계를 명시한다.

```
docs/plans/YYYY-MM-DD-<feature-slug>-fix-N.md
```

- `N`: 1부터 증가하는 순번 (동일 원본 플랜에 대한 수정 계획이 여러 개면 `-fix-1`, `-fix-2`, ...)
- 원본 플랜 파일은 그대로 두고 수정하지 않는다.

## Frontmatter

파일 최상단에 YAML frontmatter를 넣는다.

```yaml
---
date: YYYY-MM-DD
feature: <feature-slug>
spec: <기반 spec 파일명>
status: planned
relates_to: <원본 plan 파일명>  # 수정 계획일 때만 포함, 원본 플랜이면 필드 자체를 생략
---
```

| 필드 | 타입 | 필수 | 값 | 설명 |
|---|---|---|---|---|
| `date` | string (`YYYY-MM-DD`) | 필수 | - | 컨펌된 날짜, 파일명의 날짜와 동일 |
| `feature` | string (kebab-case) | 필수 | - | 파일명의 slug와 동일, 기반 스펙의 `feature`와 동일 |
| `spec` | string (파일명) | 필수 | - | 이 플랜이 기반하는 spec 파일명 |
| `status` | enum | 필수 | `planned` \| `in_progress` \| `done` | 작성 시점엔 항상 `planned`. 착수하면 `in_progress`, 모든 phase의 step이 끝나면 `done`으로 수동 갱신 |
| `relates_to` | string (파일명) | 선택 | - | 이 플랜이 참조하는 원본 plan 파일명. 수정 계획일 때만 포함, 원본 플랜이면 생략 |

## 본문 섹션

### 1. 개요

이 플랜이 어떤 스펙을 기반으로 하는지, 전체적으로 어떤 전략/순서로 접근하는지 서술. 왜 이렇게 phase를 나눴는지 포함.

### 2. Phase 목록

각 phase는 `## Phase N: <이름>` 형식의 섹션으로 작성한다.

- **Phase 경계**: 하나의 phase는 그 자체로 독립적으로 동작/검증 가능한 마일스톤이어야 한다. phase가 끝나면 "무엇을 확인하면 되는지"가 명확해야 한다.
- **Step**: phase 안의 각 작업 항목은 완료 시 검증 가능한 산출물 단위로 작성하고, 체크박스로 표시한다. 파일 단위로 억지로 쪼개지 않는다 — "무엇이 되는지(결과)"를 기준으로 삼는다.
- **Reviewer 친화성**: 코드 리뷰로 확인할 수 있는 step에는 관련 파일 경로, 클래스명, 컴포넌트명, API 계약 등 정적 검증 단서를 명시한다. 백틱 사용은 권장하지만 필수는 아니다.
- **검증 step 분리**: 빌드/테스트/린트/브라우저 확인/수동 API 호출 같은 검증 작업은 구현 산출물 step과 섞지 말고 별도 step으로 둔다. 자동 reviewer가 확인할 수 없는 수동·브라우저·외부 연동 검증은 그 성격을 step 문장에 명시한다.
- Step을 완료하면 체크박스를 체크한다 (계획 내용 자체를 수정하는 것이 아니라 진행상황을 기록하는 것이므로 허용).

```markdown
## Phase N: <phase 이름>

<이 phase가 끝나면 어떤 상태가 되는지, 마일스톤 설명>

- [ ] step 1 — <검증 가능한 산출물>
- [ ] step 2 — <검증 가능한 산출물>
```

## 템플릿

```markdown
---
date: YYYY-MM-DD
feature: <feature-slug>
spec: <spec 파일명>
status: planned
---

# <기능명> 플랜

## 개요

## Phase 1: <phase 이름>

- [ ]

## Phase 2: <phase 이름>

- [ ]
```
