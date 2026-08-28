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
- 데이터 접근: 백엔드 API 호출은 `src/lib/api/`의 공통 `apiFetch` 래퍼(`src/lib/api/client.ts`)를 통해서만 한다. 각 도메인 API 파일(`products.ts` 등)이 `fetch`를 직접 호출하지 않는다. 실패 응답은 `ApiError`(`status`, `errorCode`, `fieldErrors?`)로 throw한다. localStorage 접근은 `src/lib/storage/` 유틸을 통해 SSR 안전하게 처리한다.
- 라우팅 계약: `href`, `router.push`, search param 이름은 spec/API 계약과 일치해야 한다. 브라우저 클릭 검증은 QA 범위지만, 정적 코드에서 잘못된 경로가 보이면 correctness 버그로 본다.
- 클라이언트 컴포넌트: `"use client"`는 hook, browser API, event handler가 필요한 파일에만 둔다. 서버 컴포넌트로 충분한 파일을 불필요하게 client로 올리지 않는다.

### 권장
- 새 컴포넌트나 토큰을 추가하면 `/style-guide` 또는 `docs/design.md` 중 적절한 곳에 사용 예시와 의도를 남긴다.
- arbitrary Tailwind 값(`h-[...]`, `text-[...]`, `rounded-[...]`)은 기존 토큰/유틸로 표현하기 어려운 경우에만 사용하고, 반복되면 토큰화한다.
- 아이콘은 유니코드 글리프보다 `lucide-react` 같은 실제 아이콘 컴포넌트를 우선한다.
- 상태 계산, 금액 계산, storage 조작은 페이지 컴포넌트 안에 길게 두기보다 `src/lib/` 또는 hook으로 분리할 수 있는지 검토한다.
- 데이터 페칭은 현재 client component의 `useEffect` + `apiFetch` 패턴을 유지한다. TanStack Query 등 서버 상태 라이브러리는 폴링, 뮤테이션 후 캐시 무효화 같은 구체적 요구가 실제로 생기기 전까지 도입하지 않는다.

### API 에러 처리
- `ApiError`에 `fieldErrors`가 있으면 해당 폼 필드 아래 인라인으로 표시하고, 없으면 `src/components/feedback/Toast.tsx`로 전역 표시한다.
- 컴포넌트의 `catch` 블록에서 `ApiError`를 임의 문자열 메시지로 뭉뚱그리지 않는다. `errorCode`/`fieldErrors`를 분기해 활용할 수 있는 형태로 다룬다.

### 인증 상태 관리
- 로그인 상태는 `AuthProvider`(Context)로 전파한다. `(shell)/layout.tsx`(서버 컴포넌트)에서 쿠키로 `/me`를 먼저 호출해 초기값을 채우고, 이후 client에서는 Context로 상태를 갱신한다.
- 로그인/로그아웃/refresh API 호출은 `src/lib/api/auth.ts`에만 둔다. 컴포넌트는 `auth.ts`를 직접 호출하지 않고 `AuthProvider`가 노출하는 `login`/`logout`을 통해서만 접근한다.
- `apiFetch`가 401을 받으면 refresh API로 자동 갱신 후 원요청을 1회 재시도한다. 동시에 여러 요청이 401을 맞아도 refresh는 한 번만 실행되도록 in-flight 요청을 공유한다. refresh도 실패하면 `ApiError(401)`을 그대로 던져 로그아웃 상태로 전환한다.
- 보호된 라우트(로그인 필수 페이지) 패턴은 이 컨벤션에서 다루지 않는다. 실제로 그런 페이지가 필요해지는 기능 spec에서 정한다.

### Write 폼/검증
- 폼은 React Hook Form + Zod로 작성한다. `useState` 기반 수동 폼 상태 관리를 새로 만들지 않는다.
- 폼 필드는 `src/components/forms/`의 필드 컴포넌트(`TextField`, `PasswordField` 등)로 구성하고, 에러 메시지 렌더링은 필드 컴포넌트 안에서 통일한다.
- 서버에서 온 `ApiError.fieldErrors`는 RHF `setError(fieldName, { message })`로 매핑해 클라이언트 검증 에러와 같은 자리에 표시한다.

## 검증 방법
- `npm run build`로 빌드 확인
- `npm run lint`
- 브라우저 렌더링, 스크린샷 비교, 실제 클릭 플로우는 수동 검증 또는 별도 QA 단계에서 수행
