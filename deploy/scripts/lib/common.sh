#!/usr/bin/env bash
# 公共函数库：日志 / 探测 / 错误处理
# 由其它 *.sh 脚本通过 `source "$(dirname "$0")/lib/common.sh"` 引入

set -euo pipefail

# ---------------------------------------------------------------------------
# 颜色与日志
# ---------------------------------------------------------------------------
if [ -t 1 ]; then
  COLOR_RESET="\033[0m"
  COLOR_RED="\033[31m"
  COLOR_GREEN="\033[32m"
  COLOR_YELLOW="\033[33m"
  COLOR_BLUE="\033[34m"
  COLOR_GRAY="\033[90m"
else
  COLOR_RESET=""; COLOR_RED=""; COLOR_GREEN=""; COLOR_YELLOW=""; COLOR_BLUE=""; COLOR_GRAY=""
fi

log_info()  { printf "${COLOR_BLUE}[INFO]${COLOR_RESET}  %s\n" "$*"; }
log_ok()    { printf "${COLOR_GREEN}[OK]${COLOR_RESET}    %s\n" "$*"; }
log_warn()  { printf "${COLOR_YELLOW}[WARN]${COLOR_RESET}  %s\n" "$*" >&2; }
log_err()   { printf "${COLOR_RED}[FAIL]${COLOR_RESET}  %s\n" "$*" >&2; }
log_skip()  { printf "${COLOR_GRAY}[SKIP]${COLOR_RESET}  %s\n" "$*"; }
log_step()  { printf "\n${COLOR_BLUE}==>${COLOR_RESET} %s\n" "$*"; }

die() { log_err "$*"; exit 1; }

# ---------------------------------------------------------------------------
# 通用环境探测
# ---------------------------------------------------------------------------
detect_os() {
  if [ -r /etc/os-release ]; then
    . /etc/os-release
    echo "${ID:-unknown}:${VERSION_ID:-unknown}"
  else
    uname -s
  fi
}

detect_arch() {
  case "$(uname -m)" in
    x86_64|amd64)    echo "x86_64" ;;
    aarch64|arm64)   echo "aarch64" ;;
    loongarch64)     echo "loongarch64" ;;
    sw_64)           echo "sw_64" ;;
    *) echo "unknown"; return 1 ;;
  esac
}

# ---------------------------------------------------------------------------
# 端口 / 服务探测
# ---------------------------------------------------------------------------
port_in_use() {
  local port="$1"
  if command -v ss >/dev/null 2>&1; then
    ss -ltnH "sport = :$port" 2>/dev/null | grep -q .
  elif command -v netstat >/dev/null 2>&1; then
    netstat -ltn 2>/dev/null | awk '{print $4}' | grep -qE ":$port$"
  else
    return 2
  fi
}

# ---------------------------------------------------------------------------
# 数据库连通（按 dialect 选）
# ---------------------------------------------------------------------------
check_db_oracle() {
  command -v sqlplus >/dev/null 2>&1 || { log_skip "sqlplus 未安装，跳过 Oracle 连通"; return 0; }
  local connect="${MEDKERNEL_DB_CONNECT:-}"
  local user="${MEDKERNEL_DB_USERNAME:-}"
  local pwd="${MEDKERNEL_DB_PASSWORD:-}"
  [ -z "$connect" ] || [ -z "$user" ] || [ -z "$pwd" ] && { log_skip "Oracle 凭据未设，跳过"; return 0; }
  if echo "SELECT 1 FROM DUAL;" | sqlplus -s "${user}/${pwd}@${connect}" 2>&1 | grep -q '^[[:space:]]*1$'; then
    log_ok "Oracle 连通：${connect}"
  else
    log_err "Oracle 连通失败：${connect}"; return 1
  fi
}

check_db_dm() {
  command -v disql >/dev/null 2>&1 || { log_skip "disql 未安装，跳过达梦连通"; return 0; }
  log_skip "达梦连通检查需 disql；详见 README 验证 SQL"
}

check_db_postgres() {
  command -v psql >/dev/null 2>&1 || { log_skip "psql 未安装，跳过 PG 连通"; return 0; }
  local host="${MEDKERNEL_DB_HOST:-}"
  local user="${MEDKERNEL_DB_USERNAME:-}"
  local db="${MEDKERNEL_DB_NAME:-}"
  [ -z "$host" ] || [ -z "$user" ] || [ -z "$db" ] && { log_skip "PG 凭据未设，跳过"; return 0; }
  PGPASSWORD="${MEDKERNEL_DB_PASSWORD:-}" psql -h "$host" -U "$user" -d "$db" -c 'SELECT 1;' -At >/dev/null \
    && log_ok "PG 连通：${host}/${db}" || { log_err "PG 连通失败"; return 1; }
}

# ---------------------------------------------------------------------------
# 路径常量
# ---------------------------------------------------------------------------
MK_HOME="${MK_HOME:-/zoesoft/medkernel}"
MK_BACKUP_DIR="${MK_BACKUP_DIR:-/zoesoft/medkernel.bak}"
MK_USER="${MK_USER:-medkernel}"
MK_ENV_FILE="${MK_ENV_FILE:-$MK_HOME/conf/medkernel.env}"

load_env() {
  if [ -r "$MK_ENV_FILE" ]; then
    # shellcheck disable=SC1090
    set -a; source "$MK_ENV_FILE"; set +a
    log_info "已加载环境变量：$MK_ENV_FILE"
  else
    log_warn "未找到 $MK_ENV_FILE，使用默认或 shell 已有变量"
  fi
}

# ---------------------------------------------------------------------------
# 错误陷阱
# ---------------------------------------------------------------------------
on_error() {
  local code=$?
  log_err "脚本异常退出，行号 $1，退出码 $code"
  exit $code
}
trap 'on_error $LINENO' ERR
