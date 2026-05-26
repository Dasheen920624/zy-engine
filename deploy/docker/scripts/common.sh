#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DEFAULT_RUNTIME_ROOT="/Users/zhikunzheng/work/medkernel/runtime"
RUNTIME_ROOT="${MEDKERNEL_RUNTIME_ROOT:-$DEFAULT_RUNTIME_ROOT}"
ENV_FILE="${MEDKERNEL_ENV_FILE:-$RUNTIME_ROOT/env/medkernel.env}"
COMPOSE_FILE="$DOCKER_DIR/compose.yml"
MONITORING_COMPOSE_FILE="$DOCKER_DIR/compose.monitoring.yml"
DIFY_LOCK_COMPOSE_FILE="$DOCKER_DIR/dify/compose.lock.yml"

fail() {
  printf 'error: %s\n' "$*" >&2
  exit 1
}

replace_setting() {
  local key="$1"
  local value="$2"
  local file="$3"
  local tmp_file
  tmp_file="$(mktemp)"
  awk -F= -v key="$key" -v value="$value" '
    BEGIN { updated = 0 }
    $1 == key { print key "=" value; updated = 1; next }
    { print }
    END { if (!updated) print key "=" value }
  ' "$file" > "$tmp_file"
  mv "$tmp_file" "$file"
}

require_env_file() {
  test -f "$ENV_FILE" || fail "runtime environment is missing; run $SCRIPT_DIR/bootstrap-runtime.sh first"
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
  RUNTIME_ROOT="${MEDKERNEL_RUNTIME_ROOT:-$RUNTIME_ROOT}"
  DIFY_VERSION="${DIFY_VERSION:-v1.14.0}"
  DIFY_DOCKER_DIR="$RUNTIME_ROOT/dify/$DIFY_VERSION/docker"
}

require_docker() {
  command -v docker >/dev/null 2>&1 || fail "Docker CLI is not installed or not on PATH"
  docker info >/dev/null 2>&1 || fail "Docker Desktop is not running"
}

checksum_file() {
  local file="$1"
  local dir
  local base
  dir="$(cd "$(dirname "$file")" && pwd)"
  base="$(basename "$file")"
  if command -v sha256sum >/dev/null 2>&1; then
    (cd "$dir" && sha256sum "$base")
  elif command -v shasum >/dev/null 2>&1; then
    (cd "$dir" && shasum -a 256 "$base")
  else
    fail "sha256sum or shasum is required to create backup checksum"
  fi
}

verify_checksum() {
  local file="$1"
  local checksum="$file.sha256"
  local dir
  local checksum_base
  test -f "$checksum" || fail "backup checksum file does not exist: $checksum"
  dir="$(cd "$(dirname "$checksum")" && pwd)"
  checksum_base="$(basename "$checksum")"
  if command -v sha256sum >/dev/null 2>&1; then
    (cd "$dir" && sha256sum -c "$checksum_base")
  elif command -v shasum >/dev/null 2>&1; then
    (cd "$dir" && shasum -a 256 -c "$checksum_base")
  else
    fail "sha256sum or shasum is required to verify backup checksum"
  fi
}

core_compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

managed_full_compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" -f "$MONITORING_COMPOSE_FILE" "$@"
}

dify_compose_file() {
  if test -f "$DIFY_DOCKER_DIR/docker-compose.yaml"; then
    printf '%s\n' "$DIFY_DOCKER_DIR/docker-compose.yaml"
    return
  fi
  if test -f "$DIFY_DOCKER_DIR/docker-compose.yml"; then
    printf '%s\n' "$DIFY_DOCKER_DIR/docker-compose.yml"
    return
  fi
  fail "official Dify Compose file is missing in $DIFY_DOCKER_DIR"
}

dify_compose() {
  local compose_file
  compose_file="$(dify_compose_file)"
  test -f "$DIFY_LOCK_COMPOSE_FILE" || fail "Dify image lock Compose file is missing: $DIFY_LOCK_COMPOSE_FILE"
  docker compose --env-file "$DIFY_DOCKER_DIR/.env" --project-name medkernel-dify \
    -f "$compose_file" -f "$DIFY_LOCK_COMPOSE_FILE" "$@"
}
