# 모멘티브 프론트엔드

Next.js. Vercel 배포 (Root Directory: `frontend/`, Ignored Build Step으로 경로 필터링).

## 스택
- Next.js, TypeScript
- 백엔드 API: `NEXT_PUBLIC_API_BASE_URL` 환경변수로 주입 (api.모멘티브도메인)

## 컨벤션 (첫 기능 구현하며 채워나갈 것 — 지금은 뼈대만)
- 컴포넌트 디렉토리 구조
- API 호출 방식 (fetch wrapper 등)
- 스타일링 방식
  - Tailwind CSS v4. 브랜드 색상/타이포/radius/shadow 토큰은 `globals.css`의 `@theme`로 정의하고, 브랜드 컨셉(색상/톤/무드)은 `../design.md` 참고
  - 컴포넌트는 `src/components/<category>/` (core, commerce, forms, navigation, feedback 등) 아래 배치
  - `/style-guide` 페이지에서 구현된 컴포넌트 실물 확인 가능

## 검증 방법
- `npm run build`로 빌드 확인
- `npm run lint`
