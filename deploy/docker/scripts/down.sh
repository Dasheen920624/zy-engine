#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"
require_env_file
require_docker

MODE="${1:-core}"
case "$MODE" in
  core)
    core_compose down
    ;;
  full)
    if test -d "$DIFY_DOCKER_DIR"; then
      dify_compose down
    fi
    managed_full_compose down
    ;;
  optional)
    core_compose stop neo4j
    if test -d "$DIFY_DOCKER_DIR"; then
      dify_compose stop
    fi
    ;;
  *)
    fail "usage: $0 core|full|optional"
    ;;
esac

printf 'stopped MedKernel %s services without deleting persistent data\n' "$MODE"
