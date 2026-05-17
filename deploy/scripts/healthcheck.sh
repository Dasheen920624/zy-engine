#!/usr/bin/env bash
# 健康检查 —— Linux / Unix
# 用法：./healthcheck.sh [--url http://localhost:18080/zy-engine] [--timeout 10]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/common.sh"

BASE_URL="${ZYENGINE_HEALTH_URL:-http://localhost:${ZYENGINE_HTTP_PORT:-18080}/zy-engine}"
TIMEOUT=10

while [[ $# -gt 0 ]]; do
  case "$1" in
    --url) BASE_URL="$2"; shift 2 ;;
    --timeout) TIMEOUT="$2"; shift 2 ;;
    -h|--help) sed -n '1,5p' "$0"; exit 0 ;;
    *) die "未知参数：$1" ;;
  esac
done

log_step "Healthcheck: $BASE_URL"

probe() {
  local path="$1"
  local label="$2"
  local trace="hc-$(date +%s)-$RANDOM"
  local resp
  resp=$(curl -fsS -m "$TIMEOUT" -H "X-Trace-Id: $trace" "$BASE_URL$path" 2>&1) || {
    log_err "$label  $path  失败：$resp"
    return 1
  }
  if echo "$resp" | grep -q '"success"[[:space:]]*:[[:space:]]*true'; then
    log_ok "$label  $path  trace=$trace"
    return 0
  else
    log_err "$label  $path  返回 success!=true：$(echo "$resp" | head -c 200)"
    return 1
  fi
}

FAIL=0
probe "/api/health"            "health  "  || FAIL=$((FAIL+1))
probe "/api/system/providers"  "providers" || FAIL=$((FAIL+1))
probe "/api/system/org-context" "org-ctx " || FAIL=$((FAIL+1))

if [ "$FAIL" -eq 0 ]; then
  log_ok "全部健康检查通过"
  exit 0
fi
log_err "$FAIL 项检查失败"
exit 1
