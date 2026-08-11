# 커밋/브랜치 규칙

혼자 개발 + AI 에이전트 팀 체제를 전제로 한 경량 Git Flow. 커밋 타입, 브랜치 접두사, spec 파일명이 모두 같은 이름 체계로 연결된다.

## 커밋 메시지

[Conventional Commits](https://www.conventionalcommits.org/ko/v1.0.0/) 형식을 따른다.

```
<타입>[(스코프)]: <설명>
```

- 콜론 뒤 공백 1칸
- 설명은 한글로 작성
- 스코프는 선택. 필요할 때만 `backend`/`frontend` 등을 붙인다 (예: `feat(backend): 회원가입 API 추가`)
- Breaking change는 표준 방식대로 표기: 타입/스코프 뒤 `!` 또는 footer의 `BREAKING CHANGE: <설명>`

### 타입

| 타입 | 용도 |
|---|---|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서만 변경 |
| `style` | 코드 동작에 영향 없는 포맷팅 |
| `refactor` | 기능 변경 없는 코드 구조 개선 |
| `perf` | 성능 개선 |
| `test` | 테스트 추가/수정 |
| `build` | 빌드 시스템, 의존성 변경 |
| `ci` | CI 설정 변경 |
| `chore` | 위에 속하지 않는 잡무 (스캐폴딩, 설정 등) |

### 예시

```
feat: 회원가입 API 추가
fix(frontend): 장바구니 수량 음수 버그 수정
docs: spec-format 규칙 추가
```

## 브랜치

경량 Git Flow — `develop`, `release`, `hotfix` 브랜치 없이 `main`에서 바로 작업 브랜치를 판다.

```
<타입>/<feature-slug>
```

- `<타입>`: 위 커밋 타입 10종 중 하나를 그대로 접두사로 사용
- `<feature-slug>`: 해당 작업의 spec 파일(`specs/YYYY-MM-DD-<feature-slug>.md`)과 동일한 slug 사용 — spec, 브랜치, 커밋이 같은 이름으로 추적된다

예: spec이 `specs/2026-08-11-user-signup.md`라면 브랜치는 `feat/user-signup`

급한 수정도 별도 hotfix 브랜치 없이 `fix/<slug>`로 만들어 바로 main을 대상으로 PR을 연다.

## 병합

- 항상 GitHub PR을 거친다 (로컬에서 바로 main에 merge하지 않는다)
- 병합 방식은 **Squash merge** — main 로그는 브랜치당 커밋 1개로 정리됨
- PR 병합 후 작업 브랜치는 삭제한다
