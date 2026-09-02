#!/usr/bin/env bash
# 로컬 개발 환경 일괄 실행: DB(docker compose) + 백엔드(Spring Boot) + 프론트(Next.js)
# 사용법: ./dev.sh
# macOS / Linux / Windows(Git Bash) 지원 — OS를 판별해 포트 정리·Gradle 실행 방식을 분기한다.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
LOG_DIR="$ROOT_DIR/.dev-logs"
mkdir -p "$LOG_DIR"

BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"

BACKEND_PORT=8081
FRONTEND_PORT=3000

# ---------------------------------------------------------------------------
# OS 판별
# ---------------------------------------------------------------------------
case "$(uname -s)" in
  Darwin*)              PLATFORM="macos" ;;
  Linux*)               PLATFORM="linux" ;;
  MINGW*|MSYS*|CYGWIN*) PLATFORM="windows" ;;
  *)                    PLATFORM="unknown" ;;
esac

# Windows(Git Bash)에서는 gradlew 셸 스크립트 대신 배치 래퍼를 쓴다.
if [ "$PLATFORM" = "windows" ]; then
  GRADLE_CMD="./gradlew.bat"
else
  GRADLE_CMD="./gradlew"
fi

# ---------------------------------------------------------------------------
# JDK 21 확보
#   backend/build.gradle.kts가 Java 21 툴체인을 요구하는데, 툴체인 자동 다운로드는
#   설정돼 있지 않다. JAVA_HOME이 21이 아니면 OS별 표준 설치 경로에서 찾아 지정한다.
# ---------------------------------------------------------------------------
is_java21() {
  [ -x "$1/bin/java" ] && "$1/bin/java" -version 2>&1 | grep -q 'version "21'
}

ensure_java21() {
  if [ -n "${JAVA_HOME:-}" ] && is_java21 "$JAVA_HOME"; then
    return 0
  fi

  local candidates=()
  case "$PLATFORM" in
    macos)
      candidates+=(/opt/homebrew/Cellar/openjdk@21/*/libexec/openjdk.jdk/Contents/Home)
      candidates+=(/usr/local/Cellar/openjdk@21/*/libexec/openjdk.jdk/Contents/Home)
      candidates+=(/Library/Java/JavaVirtualMachines/*21*/Contents/Home)
      candidates+=("$HOME"/.jdks/*21*)
      ;;
    linux)
      candidates+=(/usr/lib/jvm/*21*)
      candidates+=("$HOME"/.jdks/*21*)
      ;;
    windows)
      candidates+=("$HOME"/.jdks/*21*)
      candidates+=("/c/Program Files/Java"/*21*)
      candidates+=("/c/Program Files/Eclipse Adoptium"/*21*)
      candidates+=("/c/Program Files/Microsoft/jdk-21"*)
      ;;
  esac

  # bash 3.2(macOS 기본)에서는 set -u + 빈 배열 확장이 에러라 길이를 먼저 확인한다.
  if [ ${#candidates[@]} -eq 0 ]; then
    return 1
  fi

  local candidate
  for candidate in "${candidates[@]}"; do
    if is_java21 "$candidate"; then
      export JAVA_HOME="$candidate"
      echo "  JDK 21 자동 감지: $JAVA_HOME"
      return 0
    fi
  done

  return 1
}

# ---------------------------------------------------------------------------
# 포트 정리 (OS별 분기)
#   gradlew/npm은 래퍼 프로세스라 자식(JVM/node)까지 정리하려면 포트 기준으로 죽인다.
#   macOS/Linux는 lsof, Windows(Git Bash)는 lsof가 없으므로 netstat + taskkill을 쓴다.
# ---------------------------------------------------------------------------
kill_ports() {
  case "$PLATFORM" in
    windows)
      local port pid
      for port in "$BACKEND_PORT" "$FRONTEND_PORT"; do
        # netstat 출력의 마지막 컬럼이 PID. LISTENING 상태만 대상으로 한다.
        for pid in $(netstat -ano 2>/dev/null | grep -E "LISTENING" | grep -E ":$port[[:space:]]" | awk '{print $NF}' | sort -u); do
          [ -n "$pid" ] && [ "$pid" != "0" ] && taskkill //F //PID "$pid" >/dev/null 2>&1 || true
        done
      done
      ;;
    *)
      lsof -ti:"$BACKEND_PORT","$FRONTEND_PORT" -sTCP:LISTEN 2>/dev/null | xargs -r kill -9 2>/dev/null || true
      ;;
  esac
}

cleaned_up=false
cleanup() {
  if [ "$cleaned_up" = true ]; then return; fi
  cleaned_up=true
  echo
  echo "종료 중..."
  kill_ports
  echo "종료 완료"
}
trap cleanup EXIT INT TERM

echo "[0/3] 환경 확인... (OS: $PLATFORM)"
if ! command -v docker >/dev/null 2>&1; then
  echo "  docker를 찾을 수 없습니다. Docker Desktop이 설치·실행 중인지 확인하세요."
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "  Docker 데몬이 응답하지 않습니다. Docker Desktop을 실행한 뒤 다시 시도하세요."
  exit 1
fi
if ! ensure_java21; then
  echo "  JDK 21을 찾을 수 없습니다. backend/build.gradle.kts가 Java 21 툴체인을 요구합니다."
  echo "  JDK 21을 설치하거나, 이미 있다면 JAVA_HOME을 직접 지정한 뒤 다시 실행하세요."
  echo "    예) JAVA_HOME=/path/to/jdk-21 ./dev.sh"
  exit 1
fi

echo "[1/3] DB(docker compose) 기동..."
(cd "$BACKEND_DIR" && docker compose up -d)

echo "[2/3] 백엔드(Spring Boot) 기동... (로그: $BACKEND_LOG)"
(cd "$BACKEND_DIR" && $GRADLE_CMD bootRun --console=plain) > "$BACKEND_LOG" 2>&1 &

echo "  백엔드 준비 대기 중 (http://localhost:$BACKEND_PORT/health)..."
for i in $(seq 1 60); do
  if curl -sf "http://localhost:$BACKEND_PORT/health" >/dev/null 2>&1; then
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

echo "  프론트 준비 대기 중 (http://localhost:$FRONTEND_PORT)..."
for i in $(seq 1 30); do
  if curl -sf "http://localhost:$FRONTEND_PORT" >/dev/null 2>&1; then
    echo "  프론트 준비 완료"
    break
  fi
  sleep 1
done

cat <<EOF

모멘티브 로컬 개발 서버가 실행 중입니다.
  - 백엔드: http://localhost:$BACKEND_PORT  (로그: tail -f $BACKEND_LOG)
  - 프론트: http://localhost:$FRONTEND_PORT  (로그: tail -f $FRONTEND_LOG)

Ctrl+C로 종료합니다.
EOF

wait || true
