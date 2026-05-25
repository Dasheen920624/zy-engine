#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

MODE="${1:-core}"
case "$MODE" in
  core)
    "$SCRIPT_DIR/bootstrap-runtime.sh" --skip-dify
    require_env_file
    require_docker
    core_compose up -d --build
    ;;
  full)
    "$SCRIPT_DIR/bootstrap-runtime.sh"
    require_env_file
    require_docker
    managed_full_compose up -d --build
    dify_compose up -d
    ;;
  *)
    fail "usage: $0 core|full"
    ;;
esac

printf 'started MedKernel %s mode; verify with %s %s\n' "$MODE" "$SCRIPT_DIR/healthcheck.sh" "$MODE"
