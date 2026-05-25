#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"
require_env_file
require_docker

test "${1:-}" = "--confirm-unconfigured" || \
  fail "usage: $0 --confirm-unconfigured (only before adding Dify users, apps, or provider credentials)"

DIFY_ENV_FILE="$DIFY_DOCKER_DIR/.env"
test -f "$DIFY_ENV_FILE" || fail "Dify environment is missing; run bootstrap-runtime.sh first"

DIFY_DB_USERNAME="$(awk -F= '$1 == "DB_USERNAME" { print substr($0, index($0, "=") + 1); exit }' "$DIFY_ENV_FILE")"
DIFY_DB_DATABASE="$(awk -F= '$1 == "DB_DATABASE" { print substr($0, index($0, "=") + 1); exit }' "$DIFY_ENV_FILE")"
DIFY_DB_USERNAME="${DIFY_DB_USERNAME:-postgres}"
DIFY_DB_DATABASE="${DIFY_DB_DATABASE:-dify}"
[[ "$DIFY_DB_USERNAME" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || fail "unsupported Dify database username"
[[ "$DIFY_DB_DATABASE" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || fail "unsupported Dify database name"

db_password="$(openssl rand -hex 24)"
sandbox_key="$(openssl rand -hex 32)"

# Changing SECRET_KEY after configuring Dify can make encrypted settings unreadable.
dify_compose stop api worker worker_beat plugin_daemon nginx >/dev/null
dify_compose up -d db_postgres >/dev/null
dify_compose exec -T db_postgres \
  psql -v ON_ERROR_STOP=1 -U "$DIFY_DB_USERNAME" -d "$DIFY_DB_DATABASE" \
  -c "ALTER ROLE \"$DIFY_DB_USERNAME\" WITH PASSWORD '$db_password';" >/dev/null

replace_setting "SECRET_KEY" "sk-$(openssl rand -hex 32)" "$DIFY_ENV_FILE"
replace_setting "DB_PASSWORD" "$db_password" "$DIFY_ENV_FILE"
replace_setting "REDIS_PASSWORD" "$(openssl rand -hex 24)" "$DIFY_ENV_FILE"
replace_setting "CODE_EXECUTION_API_KEY" "$sandbox_key" "$DIFY_ENV_FILE"
replace_setting "SANDBOX_API_KEY" "$sandbox_key" "$DIFY_ENV_FILE"
replace_setting "PLUGIN_DAEMON_KEY" "$(openssl rand -hex 32)" "$DIFY_ENV_FILE"
replace_setting "PLUGIN_DIFY_INNER_API_KEY" "$(openssl rand -hex 32)" "$DIFY_ENV_FILE"
chmod 600 "$DIFY_ENV_FILE"

dify_compose up -d >/dev/null
printf 'Dify initial template secrets rotated; run healthcheck.sh full after services settle\n'
