# 로그인 요청 흐름 분석

QA/Tester가 로그인 기능의 테스트케이스를 설계할 때 참고하는 문서다. 실제 소스 코드를 근거로 작성했으며, 코드로 확인하지 못한 부분은 "확인 필요"로 명시한다. 시각화된 버전(Archify 시퀀스 다이어그램)도 함께 있다 — 필요 시 같은 근거로 재생성 가능.

## 요청 흐름 (코드 근거)

| 단계 | 위치 | 무엇을 하는지 |
|---|---|---|
| 1 | `frontend/src/app/(shell)/login/page.tsx` (`LoginPage`) | `react-hook-form` + `zod`(`loginSchema`)로 이메일/비밀번호 검증 후 제출 시 `useAuth().login(values)` 호출 |
| 2 | `frontend/src/lib/auth/AuthProvider.tsx` (`AuthProvider.login`) | `authApi.login(request)` 호출 후 성공 시 `setUser(loggedInUser)` |
| 3 | `frontend/src/lib/api/auth.ts` (`login`) | `apiFetch<AuthUser>("/auth/login", { method: "POST", body: ... })` |
| 4 | `frontend/src/lib/api/client.ts` (`apiFetch` → `requestOnce`) | `fetch(..., { credentials: "include", headers: {"Content-Type": "application/json"} })`. 비정상 응답은 `ApiError`(status, errorCode, fieldErrors)로 변환 |
| 5 | `backend/.../auth/controller/AuthController.java` (`login`, `@PostMapping("/login")`) | `authService.login(request)` 호출 → `setAuthCookies(response, result)` → `result.user()`(`UserResponse`) 반환 |
| 6 | `backend/.../auth/service/AuthService.java` (`login`, `@Transactional`) | (a) `userRepository.findByEmail()` (b) `passwordEncoder.matches()` (c) `issueAuthResult(user)` |
| 7 | `backend/.../auth/repository/UserRepository.java` / `domain/User.java` | `findByEmail(String): Optional<User>`, 엔티티 테이블명 `users` |
| 8 | `backend/.../common/config/SecurityConfig.java` (`passwordEncoder()` bean) | `BCryptPasswordEncoder` — 원문 비밀번호는 저장/비교 시 해시로만 다룸 |
| 9 | `backend/.../auth/security/JwtTokenProvider.java` | `createAccessToken`(HS256, 30분, `type=access`+`role` claim), `createRefreshToken`(14일, `type=refresh`) |
| 10 | `backend/.../auth/repository/RefreshTokenRepository.java` / `domain/RefreshToken.java` | RefreshToken은 원문이 아니라 SHA-256 해시(`AuthService.hash()`)로 `refresh_token` 테이블에 저장 |
| 11 | `backend/.../auth/security/AuthCookieProvider.java` | `access_token`/`refresh_token` 쿠키를 `httpOnly`, `path("/")`로 설정. `secure`/`sameSite`는 아래 설정값 참고 |
| 12 | `frontend/src/lib/auth/AuthProvider.tsx` | 응답으로 받은 `AuthUser`로 `setUser()` → 로그인 상태 갱신 |
| 13 | `frontend/src/app/(shell)/login/page.tsx` | `router.push("/mypage")`로 이동. `ApiError.errorCode === "INVALID_CREDENTIALS"`면 인라인 에러 메시지 표시 |

## 관련 설정 (코드로 확인함, 2026-09-17 기준)

- **CORS** (`backend/.../common/config/WebConfig.java`): `allowedOrigins(momentive.cors.allowed-origins)`, `allowedMethods(GET,POST,PUT,PATCH,DELETE,OPTIONS)`, `allowCredentials(true)`.
- **쿠키 secure/same-site 기본값** (`application.yml`): `MOMENTIVE_COOKIE_SECURE` 기본 `false`, `MOMENTIVE_COOKIE_SAME_SITE` 기본 `Lax`. Railway 운영은 `MOMENTIVE_COOKIE_SECURE=true`, `MOMENTIVE_COOKIE_SAME_SITE=None`으로 오버라이드(PR #29, cross-site 쿠키 이슈 대응).
- **인증 경로 규칙** (`SecurityConfig.java`): `/health`, `/auth/signup`, `/auth/login`, `/auth/logout`, `/auth/refresh`, `/products/**`, swagger 문서는 `permitAll`. `/admin/**`은 `ROLE_ADMIN` 필요. 그 외 전부 인증 필요. CSRF 비활성화, `JwtAuthenticationFilter`가 `UsernamePasswordAuthenticationFilter` 앞에서 JWT를 검증.
- **`/admin` 서버사이드 부트스트랩**: PR #29 이후 `frontend/src/app/admin/layout.tsx`는 더 이상 `fetchServerUser()`(SSR)를 쓰지 않고, `AdminGuard`가 마운트 후 클라이언트에서 `authApi.me()`로 role을 확인한다 (cross-site 쿠키가 SSR에는 전달되지 않기 때문).
- **일반 화면**: `frontend/src/app/(shell)/layout.tsx`는 여전히 `fetchServerUser()`(SSR)로 초기 유저를 가져온다 — `/admin`과 달리 이 경로는 아직 cross-site 쿠키 문제에 노출돼 있을 수 있다 (Todo.md G2에 기록됨).

## 확인 필요 (추측하지 않음)

- 프론트/백엔드가 서로 다른 오리진일 때, 비 `/admin` 서버사이드 화면(`(shell)/layout.tsx`)에서 로그인 상태가 실제로 어떻게 보이는지는 코드만으로는 확정할 수 없다 — 브라우저 재현 필요.
- 로그인 실패(잘못된 비밀번호/존재하지 않는 이메일) 시 `INVALID_CREDENTIALS` 외 다른 에러 케이스(계정 잠김, rate limit 등)가 있는지는 코드에 없어 "미지원"으로 간주했다.

## 테스트 관점 힌트 (Tester용)

- 정상 로그인 → 쿠키 2개 수신 → `/mypage` 이동
- 잘못된 비밀번호 / 존재하지 않는 이메일 → `INVALID_CREDENTIALS` 인라인 메시지, 쿠키 미발급
- 로그인 후 `/admin` 접근 (일반 계정 → 403/리다이렉트, ADMIN 계정 → 정상)
- 로그인 후 새로고침 시 세션 유지 여부 (SSR 경로 vs 클라이언트 경로 차이 확인)
- AccessToken 만료(30분) 후 자동 갱신(`/auth/refresh`) 동작 확인
