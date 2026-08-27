# 모멘티브 프론트엔드

Next.js. Vercel 배포 (Root Directory: `frontend/`, Ignored Build Step으로 경로 필터링).

## 스택
- Next.js, TypeScript
- 백엔드 API: `NEXT_PUBLIC_API_BASE_URL` 환경변수로 주입 (api.모멘티브도메인)

## 컨벤션

아래 **필수** 항목은 frontend reviewer의 실패 판정 기준이다. **권장** 항목은 simplification/efficiency 제안으로 보고하되, correctness나 필수 컨벤션 위반이 아니면 단독으로 phase를 실패시키지 않는다.

### 필수
- App Router 구조: 사용자 화면은 원칙적으로 `src/app/(shell)/` 아래에 둔다. `/style-guide`처럼 셸을 의도적으로 피해야 하는 페이지는 예외 사유가 plan/spec에 드러나야 한다.
- 컴포넌트 위치: 재사용 컴포넌트는 `src/components/<category>/` 아래에 둔다. 기본 카테고리는 `core`, `commerce`, `forms`, `navigation`, `feedback`, `skeleton`이다.
- 컴포넌트 재사용: 새 UI를 만들기 전 `src/components/`, `docs/design.md`, `/style-guide`에 같은 역할의 컴포넌트가 있는지 먼저 확인한다. 기존 컴포넌트로 표현 가능한 UI를 중복 구현하지 않는다.
- 스타일링: Tailwind CSS v4를 사용한다. 브랜드 색상/타이포/radius/shadow는 `src/app/globals.css`의 CSS 변수와 `@theme` 토큰, 그리고 `docs/design.md`의 컴포넌트 패턴을 우선한다.
- 색상/그림자: 컴포넌트 파일에 raw hex/rgb/hsl 색상값이나 임의 shadow 값을 직접 쓰지 않는다. 새 토큰이 필요하면 `globals.css`와 `docs/design.md`를 함께 갱신한다.
- 타입: props, API 응답, localStorage 데이터는 명시 타입을 둔다. `any`는 외부 입력을 좁히기 전의 국소적인 지점이 아니면 사용하지 않는다.
- 데이터 접근: 백엔드 API 호출은 `src/lib/api/`의 fetch wrapper를 통해 모은다. localStorage 접근은 `src/lib/storage/` 유틸을 통해 SSR 안전하게 처리한다.
- 라우팅 계약: `href`, `router.push`, search param 이름은 spec/API 계약과 일치해야 한다. 브라우저 클릭 검증은 QA 범위지만, 정적 코드에서 잘못된 경로가 보이면 correctness 버그로 본다.
- 클라이언트 컴포넌트: `"use client"`는 hook, browser API, event handler가 필요한 파일에만 둔다. 서버 컴포넌트로 충분한 파일을 불필요하게 client로 올리지 않는다.

### 권장
- 새 컴포넌트나 토큰을 추가하면 `/style-guide` 또는 `docs/design.md` 중 적절한 곳에 사용 예시와 의도를 남긴다.
- arbitrary Tailwind 값(`h-[...]`, `text-[...]`, `rounded-[...]`)은 기존 토큰/유틸로 표현하기 어려운 경우에만 사용하고, 반복되면 토큰화한다.
- 아이콘은 유니코드 글리프보다 `lucide-react` 같은 실제 아이콘 컴포넌트를 우선한다.
- 상태 계산, 금액 계산, storage 조작은 페이지 컴포넌트 안에 길게 두기보다 `src/lib/` 또는 hook으로 분리할 수 있는지 검토한다.

## 검증 방법
- `npm run build`로 빌드 확인
- `npm run lint`
- 브라우저 렌더링, 스크린샷 비교, 실제 클릭 플로우는 수동 검증 또는 별도 QA 단계에서 수행
