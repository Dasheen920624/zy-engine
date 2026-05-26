#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"
require_env_file
require_docker

mkdir -p "$RUNTIME_ROOT/backups"
BACKUP_FILE="${1:-$RUNTIME_ROOT/backups/medkernel-$(date +%Y%m%d-%H%M%S).dump}"
CHECKSUM_FILE="$BACKUP_FILE.sha256"

core_compose exec -T postgres \
  pg_dump -U "$MEDKERNEL_DB_USERNAME" -d "$MEDKERNEL_DB_NAME" -Fc > "$BACKUP_FILE"
test -s "$BACKUP_FILE" || fail "backup was created but is empty: $BACKUP_FILE"
checksum_file "$BACKUP_FILE" > "$CHECKSUM_FILE"
test -s "$CHECKSUM_FILE" || fail "backup checksum was created but is empty: $CHECKSUM_FILE"

printf 'PostgreSQL backup created: %s\n' "$BACKUP_FILE"
printf 'PostgreSQL backup checksum created: %s\n' "$CHECKSUM_FILE"
