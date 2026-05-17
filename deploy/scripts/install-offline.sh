#!/usr/bin/env bash
# 离线安装 —— Linux / Unix
# 用法：sudo ./install-offline.sh [--init-db | --migrate-db | --skip-init-db]
#
# 前置：发布包已解压到 $ZY_HOME（默认 /opt/zy-engine），profile 已选好。

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/common.sh"

INIT_DB=0
MIGRATE_DB=0
SKIP_DB=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --init-db)     INIT_DB=1; shift ;;
    --migrate-db)  MIGRATE_DB=1; shift ;;
    --skip-init-db) SKIP_DB=1; shift ;;
    -h|--help) sed -n '1,10p' "$0"; exit 0 ;;
    *) die "未知参数：$1" ;;
  esac
done

[ "$EUID" -eq 0 ] || die "需要 root 权限（sudo）"
load_env

log_step "1. 检查环境"
"$SCRIPT_DIR/check-env.sh" || die "环境检查失败"

log_step "2. 创建运行用户与目录"
if ! id "$ZY_USER" >/dev/null 2>&1; then
  useradd -m -s /bin/bash "$ZY_USER"
  log_ok "已创建用户 $ZY_USER"
else
  log_ok "用户 $ZY_USER 已存在"
fi

for d in "$ZY_HOME/logs" "$ZY_HOME/conf" "$ZY_BACKUP_DIR"; do
  mkdir -p "$d"
  chown -R "$ZY_USER:$ZY_USER" "$d"
done
chmod 750 "$ZY_HOME/logs"
[ -f "$ZY_ENV_FILE" ] && chmod 600 "$ZY_ENV_FILE" && chown "$ZY_USER:$ZY_USER" "$ZY_ENV_FILE"

log_step "3. 处理数据库 DDL"
DIALECT="${ZYENGINE_DB_DIALECT:-}"
if [ "$SKIP_DB" -eq 1 ]; then
  log_skip "按 --skip-init-db 跳过 DDL 处理"
elif [ "$INIT_DB" -eq 1 ] || [ "$MIGRATE_DB" -eq 1 ]; then
  case "$DIALECT" in
    oracle)
      DDL_DIR="$ZY_HOME/db/oracle"
      log_info "Oracle DDL 目录：$DDL_DIR"
      if [ "$INIT_DB" -eq 1 ]; then
        log_info "请执行（信息科 / DBA）：sqlplus $ZYENGINE_DB_USERNAME/$ZYENGINE_DB_PASSWORD@$ZYENGINE_DB_CONNECT @$DDL_DIR/zyengine_core_ddl_with_comments.sql"
      fi
      log_info "请执行：sqlplus ... @$DDL_DIR/zyengine_org_context_migration.sql"
      ;;
    dm)
      DDL_DIR="$ZY_HOME/db/dm"
      log_info "达梦 DDL 目录：$DDL_DIR"
      log_info "请执行（信息科）：disql ${ZYENGINE_DB_USERNAME}/${ZYENGINE_DB_PASSWORD}@${ZYENGINE_DB_CONNECT} -e \"START '$DDL_DIR/zyengine_core_ddl_with_comments.sql';\""
      ;;
    postgres)
      DDL_DIR="$ZY_HOME/db/postgres"
      log_info "PG DDL 目录：$DDL_DIR"
      if command -v psql >/dev/null 2>&1; then
        PGPASSWORD="${ZYENGINE_DB_PASSWORD:-}" psql \
          -h "${ZYENGINE_DB_HOST:-localhost}" -p "${ZYENGINE_DB_PORT:-5432}" \
          -U "${ZYENGINE_DB_USERNAME:-zyengine}" -d "${ZYENGINE_DB_NAME:-zyengine}" \
          -v ON_ERROR_STOP=1 \
          -f "$DDL_DIR/zyengine_core_ddl_with_comments.sql"
        log_ok "PG DDL 已执行"
      else
        log_warn "psql 未安装；请由 DBA 执行 $DDL_DIR/zyengine_core_ddl_with_comments.sql"
      fi
      ;;
    *)
      log_warn "未设置或未知 dialect ($DIALECT)；跳过 DDL，请人工处理"
      ;;
  esac
else
  log_skip "未指定 --init-db / --migrate-db，跳过 DDL"
fi

log_step "4. 注册 systemd"
if [ -f "$ZY_HOME/systemd/zy-engine.service" ]; then
  install -m 644 "$ZY_HOME/systemd/zy-engine.service" /etc/systemd/system/zy-engine.service
  systemctl daemon-reload
  systemctl enable zy-engine
  log_ok "systemd 单元已注册：zy-engine.service"
else
  log_warn "$ZY_HOME/systemd/zy-engine.service 不存在，跳过 systemd"
fi

log_step "5. 启动应用"
systemctl restart zy-engine
sleep 3
systemctl is-active --quiet zy-engine && log_ok "zy-engine 已运行" || die "启动失败，请查看：journalctl -u zy-engine -n 100"

log_step "6. 健康检查"
"$SCRIPT_DIR/healthcheck.sh"

log_step "7. 完成"
log_ok "安装完成。版本：$(cat "$ZY_HOME/manifest.json" 2>/dev/null | grep -oE '"version":[[:space:]]*"[^"]+"' || echo unknown)"
echo ""
echo "下一步建议："
echo "  - 配置 Nginx：sudo cp $ZY_HOME/nginx/zy-engine.conf /etc/nginx/conf.d/"
echo "  - 跑客户验收剧本：见 zy-engine-mvp/docs/04_客户验收剧本与报告模板.md"
echo "  - 接入监控：见 zy-engine-mvp/docs/09_内网部署与版本管理.md §11"
