#!/usr/bin/env bash
# 로컬 개발 환경 일괄 실행: DB(docker compose) + 백엔드(Spring Boot) + 프론트(Next.js)
# 사용법: ./dev.sh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
LOG_DIR="$ROOT_DIR/.dev-logs"
mkdir -p "$LOG_DIR"

BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"

cleaned_up=false
cleanup() {
  if [ "$cleaned_up" = true ]; then return; fi
  cleaned_up=true
  echo
  echo "종료 중..."
  # gradlew/npm은 래퍼 프로세스라 자식(JVM/node)까지 정리하려면 포트 기준으로 죽인다
  lsof -ti:8081,3000 -sTCP:LISTEN 2>/dev/null | xargs -r kill -9
  echo "종료 완료"
}
trap cleanup EXIT INT TERM

echo "[1/3] DB(docker compose) 기동..."
(cd "$BACKEND_DIR" && docker compose up -d)

echo "[2/3] 백엔드(Spring Boot) 기동... (로그: $BACKEND_LOG)"
(cd "$BACKEND_DIR" && ./gradlew bootRun --console=plain) > "$BACKEND_LOG" 2>&1 &

echo "  백엔드 준비 대기 중 (http://localhost:8081/health)..."
for i in $(seq 1 60); do
  if curl -sf http://localhost:8081/health >/dev/null 2>&1; then
    echo "  백엔드 준비 완료"
    break
  fi
  sleep 2
  if [ "$i" -eq 60 ]; then
    echo "  백엔드가 120초 내에 기동하지 않았습니다. $BACKEND_LOG 확인하세요."
    exit 1
  fi
done

echo "[3/3] 프론트(Next.js) 기동... (로그: $FRONTEND_LOG)"
if [ ! -f "$FRONTEND_DIR/.env.local" ]; then
  cp "$FRONTEND_DIR/.env.local.example" "$FRONTEND_DIR/.env.local"
fi
if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
  echo "  node_modules 없음 — npm install 실행..."
  (cd "$FRONTEND_DIR" && npm install)
fi
(cd "$FRONTEND_DIR" && npm run dev) > "$FRONTEND_LOG" 2>&1 &

echo "  프론트 준비 대기 중 (http://localhost:3000)..."
for i in $(seq 1 30); do
  if curl -sf http://localhost:3000 >/dev/null 2>&1; then
    echo "  프론트 준비 완료"
    break
  fi
  sleep 1
done

cat <<EOF

모멘티브 로컬 개발 서버가 실행 중입니다.
  - 백엔드: http://localhost:8081  (로그: tail -f $BACKEND_LOG)
  - 프론트: http://localhost:3000  (로그: tail -f $FRONTEND_LOG)

Ctrl+C로 종료합니다.
EOF

wait || true
