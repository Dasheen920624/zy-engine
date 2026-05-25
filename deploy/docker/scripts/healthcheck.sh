#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"
require_env_file
require_docker

MODE="${1:-core}"
DIFY_REQUIRED_SERVICES=(api db_postgres nginx plugin_daemon redis sandbox ssrf_proxy weaviate web worker worker_beat)
DIFY_HEALTHY_SERVICES=(api db_postgres redis sandbox)

check_dify_services_running() {
  local service
  local running_service
  for service in "${DIFY_REQUIRED_SERVICES[@]}"; do
    running_service="$(dify_compose ps --services --status running "$service")"
    test "$running_service" = "$service" || fail "Dify required service is not running: $service"
  done
}

check_dify_services_healthy() {
  local service
  local container_id
  local health
  for service in "${DIFY_HEALTHY_SERVICES[@]}"; do
    container_id="$(dify_compose ps -q "$service")"
    test -n "$container_id" || fail "Dify required service container is missing: $service"
    health="$(docker inspect --format '{{.State.Health.Status}}' "$container_id")"
    test "$health" = "healthy" || fail "Dify service is not healthy: $service ($health)"
  done
}

if test "$MODE" = "full"; then
  managed_full_compose ps
  dify_compose ps
elif test "$MODE" = "core"; then
  core_compose ps
else
  fail "usage: $0 core|full"
fi

core_compose exec -T postgres pg_isready -U "$MEDKERNEL_DB_USERNAME" -d "$MEDKERNEL_DB_NAME"
curl -fsS "http://localhost:${MEDKERNEL_BACKEND_PORT:-18080}/medkernel/actuator/health" >/dev/null
curl -fsS "http://localhost:${MEDKERNEL_BACKEND_PORT:-18080}/medkernel/api/v1/system/ping" >/dev/null
curl -fsS "http://localhost:${MEDKERNEL_FRONTEND_PORT:-8088}/healthz" >/dev/null
curl -fsS "http://localhost:${MEDKERNEL_NEO4J_BROWSER_PORT:-7474}/" >/dev/null

if test "$MODE" = "full"; then
  check_dify_services_running
  check_dify_services_healthy
  curl -fsS "http://localhost:${MEDKERNEL_PROMETHEUS_PORT:-9090}/-/ready" >/dev/null
  curl -fsS "http://localhost:${MEDKERNEL_GRAFANA_PORT:-3000}/api/health" >/dev/null
  dify_compose exec -T api curl -fsS http://localhost:5001/health >/dev/null
  curl -fsS "http://localhost:${DIFY_HTTP_PORT:-8090}/" >/dev/null
fi

printf 'MedKernel %s mode is healthy\n' "$MODE"
