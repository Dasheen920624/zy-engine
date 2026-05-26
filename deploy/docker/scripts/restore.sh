#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"
require_env_file
require_docker

BACKUP_FILE="${1:-}"
test -n "$BACKUP_FILE" || fail "usage: $0 <postgres-backup.dump>"
test -f "$BACKUP_FILE" || fail "backup file does not exist: $BACKUP_FILE"
verify_checksum "$BACKUP_FILE"
printf 'PostgreSQL backup checksum verified: %s.sha256\n' "$BACKUP_FILE"

core_compose exec -T postgres \
  pg_restore -U "$MEDKERNEL_DB_USERNAME" -d "$MEDKERNEL_DB_NAME" \
  --clean --if-exists --no-owner < "$BACKUP_FILE"

printf 'PostgreSQL restore completed from: %s\n' "$BACKUP_FILE"
