#!/usr/bin/env bash
# 升级到新版本 —— Linux / Unix
# 用法：sudo ./upgrade.sh --to v1.3.0 [--backup-only] [--migrate-db]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/common.sh"

TO_VER=""
BACKUP_ONLY=0
MIGRATE_DB=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --to) TO_VER="$2"; shift 2 ;;
    --backup-only) BACKUP_ONLY=1; shift ;;
    --migrate-db) MIGRATE_DB=1; shift ;;
    -h|--help) sed -n '1,5p' "$0"; exit 0 ;;
    *) die "未知参数：$1" ;;
  esac
done

[ "$EUID" -eq 0 ] || die "需要 root 权限（sudo）"

log_step "1. 备份当前版本"
TS="$(date +%Y%m%d_%H%M%S)"
BACKUP_PATH="$MK_BACKUP_DIR/$TS"
mkdir -p "$BACKUP_PATH"
for d in lib frontend conf systemd nginx; do
  if [ -d "$MK_HOME/$d" ]; then
    cp -a "$MK_HOME/$d" "$BACKUP_PATH/"
    log_ok "备份 $d → $BACKUP_PATH/"
  fi
done
[ -f "$MK_HOME/manifest.json" ] && cp "$MK_HOME/manifest.json" "$BACKUP_PATH/"
log_ok "备份完成：$BACKUP_PATH"
echo "$BACKUP_PATH" > "$MK_HOME/.last-backup"

[ "$BACKUP_ONLY" -eq 1 ] && { log_ok "仅备份，已完成。"; exit 0; }

[ -n "$TO_VER" ] || die "请指定 --to <version>，例：--to v1.3.0"

log_step "2. 停止服务"
systemctl stop medkernel || true

log_step "3. 解压新版发布包"
NEW_PKG="/tmp/medkernel-${TO_VER}.tar.gz"
[ -f "$NEW_PKG" ] || die "未找到 $NEW_PKG；请先把发布包传到 /tmp/"
TMPDIR_NEW="$(mktemp -d)"
tar -xzvf "$NEW_PKG" -C "$TMPDIR_NEW" --strip-components=1 >/dev/null
log_ok "解压到临时目录：$TMPDIR_NEW"

log_step "4. 覆盖到 $MK_HOME（保留 conf）"
# conf 不覆盖，避免覆盖客户填的凭据
for d in lib frontend db scripts systemd nginx docs profiles manifest.json CHANGELOG.md; do
  if [ -e "$TMPDIR_NEW/$d" ]; then
    rm -rf "$MK_HOME/${d:?}"
    cp -a "$TMPDIR_NEW/$d" "$MK_HOME/"
  fi
done
chown -R "$ZY_USER:$ZY_USER" "$MK_HOME"
chmod 600 "$ZY_ENV_FILE" 2>/dev/null || true
log_ok "新版本已就位"

log_step "5. 数据库迁移"
if [ "$MIGRATE_DB" -eq 1 ]; then
  "$SCRIPT_DIR/install-offline.sh" --migrate-db --skip-init-db || die "DB 迁移失败"
else
  log_skip "未指定 --migrate-db，跳过 DDL；如新版本有迁移脚本，请人工评估后执行"
fi

log_step "6. 重启 systemd"
systemctl daemon-reload
systemctl start medkernel
sleep 3
systemctl is-active --quiet medkernel || die "启动失败，请查看：journalctl -u medkernel -n 100"

log_step "7. 健康检查"
if "$SCRIPT_DIR/healthcheck.sh"; then
  log_ok "升级成功，新版本：$TO_VER"
  echo "回滚指令（如出问题，5 分钟内可执行）：sudo $SCRIPT_DIR/rollback.sh --to $TS"
else
  log_err "健康检查失败，建议立即回滚"
  exit 1
fi
