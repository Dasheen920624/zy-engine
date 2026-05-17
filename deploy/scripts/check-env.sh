#!/usr/bin/env bash
# 部署前环境检查 —— Linux / Unix
# 用法：sudo ./check-env.sh [--profile centos7-x86_64-oracle]
#
# 检查项：OS / CPU / JDK / locale / 时区 / 磁盘 / 端口 / 防火墙 / SELinux / 数据库连通 / 字符集

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/common.sh"

PROFILE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --profile) PROFILE="$2"; shift 2 ;;
    -h|--help) sed -n '1,10p' "$0"; exit 0 ;;
    *) die "未知参数：$1" ;;
  esac
done

if [ -n "$PROFILE" ]; then
  PROFILE_FILE="$SCRIPT_DIR/../profiles/${PROFILE}.env"
  [ -r "$PROFILE_FILE" ] || die "Profile 不存在：$PROFILE_FILE"
  # shellcheck disable=SC1090
  set -a; source "$PROFILE_FILE"; set +a
  log_info "已加载 profile：$PROFILE"
fi

load_env || true

OK_COUNT=0; WARN_COUNT=0; FAIL_COUNT=0; SKIP_COUNT=0
record_ok()   { OK_COUNT=$((OK_COUNT+1)); log_ok   "$@"; }
record_warn() { WARN_COUNT=$((WARN_COUNT+1)); log_warn "$@"; }
record_fail() { FAIL_COUNT=$((FAIL_COUNT+1)); log_err  "$@"; }
record_skip() { SKIP_COUNT=$((SKIP_COUNT+1)); log_skip "$@"; }

log_step "OS 与硬件"
record_ok "OS: $(detect_os)"
ARCH="$(detect_arch || echo unknown)"
[ "$ARCH" = "unknown" ] && record_fail "未识别的 CPU 架构：$(uname -m)" || record_ok "CPU 架构：$ARCH"

log_step "JDK"
if command -v java >/dev/null 2>&1; then
  JAVA_VER="$(java -version 2>&1 | head -1)"
  if echo "$JAVA_VER" | grep -qE '"1\.8\.[0-9]+'; then
    record_ok "JDK：$JAVA_VER"
  else
    record_fail "JDK 非 1.8：$JAVA_VER（本项目固定 JDK 1.8）"
  fi
else
  record_fail "未检测到 java；请安装 JDK 1.8（建议毕昇 JDK / Temurin / OpenJDK）"
fi

log_step "locale / 时区"
LC="${LANG:-}"
if [[ "$LC" == *UTF-8* || "$LC" == *utf8* ]]; then
  record_ok "locale：$LC"
else
  record_warn "locale 非 UTF-8：$LC，建议 export LANG=zh_CN.UTF-8"
fi
TZ="$(date +%Z)"
if [ "$TZ" = "CST" ] || [ "$TZ" = "+08" ] || [ "$TZ" = "Asia/Shanghai" ]; then
  record_ok "时区：$TZ"
else
  record_warn "时区非 Asia/Shanghai：$TZ"
fi

log_step "目录与磁盘"
if [ -d "$ZY_HOME" ]; then
  FREE_GB=$(df -BG "$ZY_HOME" | awk 'NR==2 {gsub(/G/,"",$4); print $4}')
  if [ "${FREE_GB:-0}" -ge 10 ]; then
    record_ok "$ZY_HOME 可用空间 ${FREE_GB}GB"
  else
    record_warn "$ZY_HOME 可用空间仅 ${FREE_GB}GB，建议 ≥ 10GB"
  fi
else
  record_warn "$ZY_HOME 不存在（首次部署会创建）"
fi

log_step "端口"
PORT_BACKEND="${ZYENGINE_HTTP_PORT:-18080}"
if port_in_use "$PORT_BACKEND"; then
  record_fail "后端端口 $PORT_BACKEND 已被占用"
else
  record_ok "后端端口 $PORT_BACKEND 空闲"
fi

PORT_FRONTEND="${ZYENGINE_FRONTEND_PORT:-80}"
if port_in_use "$PORT_FRONTEND"; then
  record_warn "前端端口 $PORT_FRONTEND 已被占用（Nginx 已运行？）"
else
  record_ok "前端端口 $PORT_FRONTEND 空闲"
fi

log_step "SELinux / 防火墙"
if command -v getenforce >/dev/null 2>&1; then
  SE="$(getenforce)"
  case "$SE" in
    Enforcing) record_warn "SELinux: Enforcing（部署脚本将自动配置 fcontext）" ;;
    Permissive) record_ok "SELinux: Permissive" ;;
    Disabled) record_ok "SELinux: Disabled" ;;
    *) record_warn "SELinux 状态未知：$SE" ;;
  esac
else
  record_skip "SELinux 未启用或未安装"
fi

if systemctl is-active --quiet firewalld 2>/dev/null; then
  record_warn "firewalld 启用；如端口未开请运行 firewall-cmd"
else
  record_skip "firewalld 未启用"
fi

log_step "数据库"
DIALECT="${ZYENGINE_DB_DIALECT:-}"
case "$DIALECT" in
  oracle)   check_db_oracle   || true ;;
  dm)       check_db_dm       || true ;;
  postgres) check_db_postgres || true ;;
  "")       record_warn "未设置 ZYENGINE_DB_DIALECT，跳过数据库连通" ;;
  *)        record_warn "未知 dialect：$DIALECT" ;;
esac

log_step "结果"
echo ""
echo "  OK   : $OK_COUNT"
echo "  WARN : $WARN_COUNT"
echo "  SKIP : $SKIP_COUNT"
echo "  FAIL : $FAIL_COUNT"
echo ""

if [ "$FAIL_COUNT" -gt 0 ]; then
  log_err "存在 FAIL 项，请先修复后再继续安装。"
  exit 1
fi
log_ok "检查通过，可继续 install-offline.sh / upgrade.sh"
