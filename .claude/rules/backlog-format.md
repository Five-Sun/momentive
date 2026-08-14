# Backlog 작성 규격

`plan`의 Phase 검증이 실패했을 때, reviewer가 실패 원인과 조치를 기록하는 문서의 규격이다. plan 파일 자체는 순수 계획 문서로 유지하고(실패 이력을 남기지 않음), 실패로부터 배운 것은 이 별도 문서에 쌓아 같은 실수의 반복을 줄인다.

## 파일 위치 및 이름

```
.claude/backlog/YYYY-MM-DD-<feature-slug>-phase<N>-<seq>.md
```

- `YYYY-MM-DD`: 검증 실패가 발생한 날짜
- `<feature-slug>`: 해당 plan/spec의 feature slug와 동일
- `<N>`: 실패한 Phase 번호
- `<seq>`: 같은 날 같은 feature-slug·같은 Phase에서 검증이 여러 번 실패하면 `01`, `02`... 순서로 증가. 최초 실패도 `01`부터 붙여 항상 일관된 패턴 유지 (나중에 glob/grep으로 다루기 쉽게)

폴더 방식(단일 파일 append가 아님)을 쓰는 이유: `specs/`, `plans/`와 명명 관례를 통일하고, 여러 컴퓨터에서 동시에 작업할 때 단일 파일 append 방식이 유발하는 git 충돌을 피하기 위함이다.

## Frontmatter

```yaml
---
date: YYYY-MM-DD
feature: <feature-slug>
phase: <N>
category: <카테고리>
---
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `date` | string (`YYYY-MM-DD`) | 필수 | 검증 실패 발생 날짜, 파일명의 날짜와 동일 |
| `feature` | string (kebab-case) | 필수 | 파일명의 slug와 동일 |
| `phase` | integer | 필수 | 실패한 Phase 번호 |
| `category` | enum | 필수 | `backend` \| `frontend` \| `infra` \| `test` \| `spec-ambiguity` \| `other` — 반복 패턴을 찾기 위한 태그. 애매하면 `other` |

## 본문 섹션

아래 4개 섹션을 이 순서대로, 이 제목 그대로 포함한다.

### 1. 실패

Phase 검증 항목 중 정확히 무엇이, 어떤 조건에서 실패했는지 구체적으로 서술한다. ("테스트 실패" 같은 뭉뚱그린 표현 대신, 어떤 검증 체크박스가 실패했는지 명시)

### 2. 원인

표면적 증상이 아니라 근본 원인을 서술한다. "왜 이 문제가 발생할 수밖에 없었는지"에 답한다.

### 3. 조치

실제로 어떻게 고쳐서 통과시켰는지 서술한다.

### 4. 재발 방지

다음에 비슷한 작업(같은 category 또는 같은 영역)을 시작하기 전에 무엇을 미리 체크하면 이 실패를 피할 수 있는지, 실행 가능한 한 줄로 남긴다. 이 섹션이 이 문서의 핵심이다 — 단순 기록이 아니라 다음 작업에 실제로 쓰일 체크리스트 항목이어야 한다.

## 작성 주체 및 시점

reviewer 에이전트가 Phase 검증 실패를 감지한 시점에 자동으로 기록한다. plan 파일은 건드리지 않는다.

## 조회 방법

새 Phase를 시작하기 전, planner 또는 구현 에이전트는 `.claude/backlog/`에서 같은 `category` 또는 같은 `feature`의 기존 항목을 먼저 훑어 재발 방지 체크리스트로 참고한다.

```bash
# 같은 카테고리의 과거 실패 훑기
grep -l "category: backend" .claude/backlog/*.md

# 같은 기능의 과거 실패 훑기
ls .claude/backlog/*-<feature-slug>-*.md
```

## 템플릿

```markdown
---
date: YYYY-MM-DD
feature: <feature-slug>
phase: <N>
category: <backend|frontend|infra|test|spec-ambiguity|other>
---

# <feature-slug> / Phase <N> — YYYY-MM-DD

## 실패

## 원인

## 조치

## 재발 방지
```
