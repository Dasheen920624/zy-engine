#!/usr/bin/env bash
# 回滚 —— Linux / Unix
# 用法：sudo ./rollback.sh --to <timestamp 或 last>
#   --to last           回滚到 .last-backup
#   --to 20260517_083000 回滚到指定备份目录

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/common.sh"

TO="last"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --to) TO="$2"; shift 2 ;;
    -h|--help) sed -n '1,5p' "$0"; exit 0 ;;
    *) die "未知参数：$1" ;;
  esac
done

[ "$EUID" -eq 0 ] || die "需要 root 权限（sudo）"

if [ "$TO" = "last" ]; then
  [ -r "$MK_HOME/.last-backup" ] || die "未找到 .last-backup，请显式指定 --to <timestamp>"
  BACKUP_PATH="$(cat "$MK_HOME/.last-backup")"
else
  BACKUP_PATH="$MK_BACKUP_DIR/$TO"
fi

[ -d "$BACKUP_PATH" ] || die "备份目录不存在：$BACKUP_PATH"

log_step "1. 停止服务"
systemctl stop medkernel || true

log_step "2. 还原备份 ← $BACKUP_PATH"
for d in lib frontend conf systemd nginx; do
  if [ -d "$BACKUP_PATH/$d" ]; then
    rm -rf "$MK_HOME/${d:?}"
    cp -a "$BACKUP_PATH/$d" "$MK_HOME/"
    log_ok "还原 $d"
  fi
done
[ -f "$BACKUP_PATH/manifest.json" ] && cp "$BACKUP_PATH/manifest.json" "$MK_HOME/"

chown -R "$MK_USER:$MK_USER" "$MK_HOME"
chmod 600 "$MK_ENV_FILE" 2>/dev/null || true

log_step "3. 重启服务"
systemctl daemon-reload
systemctl start medkernel
sleep 3
systemctl is-active --quiet medkernel || die "启动失败，请查看：journalctl -u medkernel -n 100"

log_step "4. 健康检查"
"$SCRIPT_DIR/healthcheck.sh"
log_ok "回滚完成 → $(cat "$MK_HOME/manifest.json" 2>/dev/null | grep -oE '"version":[[:space:]]*"[^"]+"' || echo unknown)"
