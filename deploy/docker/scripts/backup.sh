#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"
require_env_file
require_docker

mkdir -p "$RUNTIME_ROOT/backups"
BACKUP_FILE="${1:-$RUNTIME_ROOT/backups/medkernel-$(date +%Y%m%d-%H%M%S).dump}"

core_compose exec -T postgres \
  pg_dump -U "$MEDKERNEL_DB_USERNAME" -d "$MEDKERNEL_DB_NAME" -Fc > "$BACKUP_FILE"
test -s "$BACKUP_FILE" || fail "backup was created but is empty: $BACKUP_FILE"

printf 'PostgreSQL backup created: %s\n' "$BACKUP_FILE"
