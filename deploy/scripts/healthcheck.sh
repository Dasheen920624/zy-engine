#!/usr/bin/env bash
# 健康检查 —— Linux / Unix
# 用法：./healthcheck.sh [--url http://localhost:18080/medkernel] [--timeout 10] [--monitoring]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/common.sh"

BASE_URL="${MEDKERNEL_HEALTH_URL:-http://localhost:${MEDKERNEL_HTTP_PORT:-18080}/medkernel}"
TIMEOUT=10
CHECK_MONITORING=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --url) BASE_URL="$2"; shift 2 ;;
    --timeout) TIMEOUT="$2"; shift 2 ;;
    --monitoring) CHECK_MONITORING=true; shift ;;
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

# GA-OPS-01: 增强 actuator/health 和 Prometheus 指标检查
ACTUATOR_URL="${BASE_URL%%/medkernel}:18081/actuator/health"
log_step "Actuator: $ACTUATOR_URL"
actuator_resp=$(curl -fsS -m "$TIMEOUT" "$ACTUATOR_URL" 2>&1) || {
  log_err "actuator/health  失败：$actuator_resp"
  FAIL=$((FAIL+1))
}
if [ -n "$actuator_resp" ]; then
  if echo "$actuator_resp" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    log_ok "actuator/health  UP"
  else
    log_err "actuator/health  非 UP：$(echo "$actuator_resp" | head -c 200)"
    FAIL=$((FAIL+1))
  fi
fi

# Actuator readiness 就绪探针检查（含 Provider 状态）
READINESS_URL="${BASE_URL%%/medkernel}:18081/medkernel/actuator/health/readiness"
log_step "Readiness: $READINESS_URL"
readiness_resp=$(curl -fsS -m "$TIMEOUT" "$READINESS_URL" 2>&1) || {
  log_err "actuator/health/readiness  失败：$readiness_resp"
  FAIL=$((FAIL+1))
}
if [ -n "$readiness_resp" ]; then
  if echo "$readiness_resp" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    log_ok "actuator/health/readiness  UP"
  else
    log_err "actuator/health/readiness  非 UP：$(echo "$readiness_resp" | head -c 200)"
    FAIL=$((FAIL+1))
  fi
fi

# Prometheus 指标验证：检查 /actuator/prometheus 返回非空数据
PROMETHEUS_METRICS_URL="${BASE_URL%%/medkernel}:18081/medkernel/actuator/prometheus"
log_step "Prometheus Metrics: $PROMETHEUS_METRICS_URL"
metrics_resp=$(curl -fsS -m "$TIMEOUT" "$PROMETHEUS_METRICS_URL" 2>&1) || {
  log_err "actuator/prometheus  失败：$metrics_resp"
  FAIL=$((FAIL+1))
}
if [ -n "$metrics_resp" ]; then
  metrics_count=$(echo "$metrics_resp" | grep -c '^medkernel_' || true)
  if [ "$metrics_count" -gt 0 ]; then
    log_ok "actuator/prometheus  返回 $metrics_count 个 medkernel 指标"
  else
    log_err "actuator/prometheus  无 medkernel 指标数据"
    FAIL=$((FAIL+1))
  fi
fi

# === 监控组件健康检查（默认必须通过，--monitoring 时可选） ===
PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
GRAFANA_URL="${GRAFANA_URL:-http://localhost:3000}"
ALERTMANAGER_URL="${ALERTMANAGER_URL:-http://localhost:9093}"

check_monitoring() {
  local monitoring_fail=0

  # Prometheus 健康检查
  if curl -fsS -m 5 "$PROMETHEUS_URL/-/healthy" >/dev/null 2>&1; then
    log_ok "Prometheus  可达"
    # 检查是否有活跃的 critical 告警
    active_alerts=$(curl -fsS -m 5 "$PROMETHEUS_URL/api/v1/alerts?state=firing" 2>/dev/null | grep -c '"severity":"critical"' || true)
    if [ "$active_alerts" -gt 0 ]; then
      log_err "SLO: $active_alerts 个 critical 告警正在触发"
      monitoring_fail=$((monitoring_fail+1))
    else
      log_ok "SLO: 无 critical 告警触发"
    fi
  else
    log_err "Prometheus 不可达"
    monitoring_fail=$((monitoring_fail+1))
  fi

  # Grafana 健康检查
  if curl -fsS -m 5 "$GRAFANA_URL/api/health" >/dev/null 2>&1; then
    log_ok "Grafana  可达"
  else
    log_err "Grafana 不可达"
    monitoring_fail=$((monitoring_fail+1))
  fi

  # Alertmanager 健康检查
  if curl -fsS -m 5 "$ALERTMANAGER_URL/-/healthy" >/dev/null 2>&1; then
    log_ok "Alertmanager  可达"
  else
    log_err "Alertmanager 不可达"
    monitoring_fail=$((monitoring_fail+1))
  fi

  return $monitoring_fail
}

if [ "$CHECK_MONITORING" = true ]; then
  log_step "监控组件检查（--monitoring 模式，可选）"
  check_monitoring || log_warn "部分监控组件检查失败（--monitoring 模式下不阻塞）"
else
  log_step "监控组件检查（必须通过）"
  check_monitoring || FAIL=$((FAIL+$?))
fi

if [ "$FAIL" -eq 0 ]; then
  log_ok "全部健康检查通过"
  exit 0
fi
log_err "$FAIL 项检查失败"
exit 1
