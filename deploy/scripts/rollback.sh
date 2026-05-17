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
  [ -r "$ZY_HOME/.last-backup" ] || die "未找到 .last-backup，请显式指定 --to <timestamp>"
  BACKUP_PATH="$(cat "$ZY_HOME/.last-backup")"
else
  BACKUP_PATH="$ZY_BACKUP_DIR/$TO"
fi

[ -d "$BACKUP_PATH" ] || die "备份目录不存在：$BACKUP_PATH"

log_step "1. 停止服务"
systemctl stop zy-engine || true

log_step "2. 还原备份 ← $BACKUP_PATH"
for d in lib frontend conf systemd nginx; do
  if [ -d "$BACKUP_PATH/$d" ]; then
    rm -rf "$ZY_HOME/${d:?}"
    cp -a "$BACKUP_PATH/$d" "$ZY_HOME/"
    log_ok "还原 $d"
  fi
done
[ -f "$BACKUP_PATH/manifest.json" ] && cp "$BACKUP_PATH/manifest.json" "$ZY_HOME/"

chown -R "$ZY_USER:$ZY_USER" "$ZY_HOME"
chmod 600 "$ZY_ENV_FILE" 2>/dev/null || true

log_step "3. 重启服务"
systemctl daemon-reload
systemctl start zy-engine
sleep 3
systemctl is-active --quiet zy-engine || die "启动失败，请查看：journalctl -u zy-engine -n 100"

log_step "4. 健康检查"
"$SCRIPT_DIR/healthcheck.sh"
log_ok "回滚完成 → $(cat "$ZY_HOME/manifest.json" 2>/dev/null | grep -oE '"version":[[:space:]]*"[^"]+"' || echo unknown)"
