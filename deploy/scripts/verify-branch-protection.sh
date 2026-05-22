#!/usr/bin/env bash
# 分支保护规则验证脚本
# 验证 main/develop 分支的保护规则是否符合 v1.0 GA 要求
#
# 用法：
#   ./verify-branch-protection.sh [--repo owner/repo] [--token GITHUB_TOKEN]
#
# 环境变量：
#   GITHUB_REPOSITORY: 仓库路径（默认从 git remote 推断）
#   GITHUB_TOKEN: GitHub Personal Access Token

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/lib/common.sh"

# ---------------------------------------------------------------------------
# 参数解析
# ---------------------------------------------------------------------------
REPO="${GITHUB_REPOSITORY:-}"
TOKEN="${GITHUB_TOKEN:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo)   REPO="$2";   shift 2 ;;
    --token)  TOKEN="$2";  shift 2 ;;
    -h|--help)
      echo "用法: $0 [--repo owner/repo] [--token GITHUB_TOKEN]"
      exit 0 ;;
    *) die "未知参数: $1" ;;
  esac
done

# ---------------------------------------------------------------------------
# 从 git remote 推断仓库
# ---------------------------------------------------------------------------
if [ -z "$REPO" ]; then
  remote_url="$(git remote get-url origin 2>/dev/null || true)"
  if [ -n "$remote_url" ]; then
    # https://github.com/owner/repo.git -> owner/repo
    REPO="$(echo "$remote_url" | sed -E 's|.*github.com[:/]([^/]+/[^/]+)(\.git)?|\1|')"
  fi
fi

[ -z "$REPO" ] && die "无法推断仓库路径，请指定 --repo owner/repo"

# ---------------------------------------------------------------------------
# 验证函数
# ---------------------------------------------------------------------------
PASS=0
FAIL=0

check_rule() {
  local name="$1"
  local expected="$2"
  local actual="$3"
  if [ "$actual" = "$expected" ]; then
    log_ok "${name}: ${actual}"
    PASS=$((PASS + 1))
  else
    log_err "${name}: 期望 ${expected}，实际 ${actual}"
    FAIL=$((FAIL + 1))
  fi
}

# ---------------------------------------------------------------------------
# GitHub API 验证（需要 token）
# ---------------------------------------------------------------------------
if [ -n "$TOKEN" ]; then
  log_step "通过 GitHub API 验证分支保护规则"

  for branch in main develop; do
    log_info "检查 ${branch} 分支..."

    response="$(curl -s -H "Authorization: token ${TOKEN}" \
      -H "Accept: application/vnd.github+json" \
      "https://api.github.com/repos/${REPO}/branches/${branch}/protection" 2>/dev/null || echo '{}')"

    if echo "$response" | grep -q '"message"'; then
      log_warn "${branch} 分支保护规则未设置或无权限读取"
      continue
    fi

    # 检查 PR 要求
    pr_required="$(echo "$response" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(d.get('required_pull_request_reviews', {}).get('enabled', False))" 2>/dev/null || echo "unknown")"
    check_rule "${branch}/require_pr" "True" "$pr_required"

    # 检查 status checks
    checks_enabled="$(echo "$response" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(d.get('required_status_checks', {}).get('enabled', False))" 2>/dev/null || echo "unknown")"
    check_rule "${branch}/require_status_checks" "True" "$checks_enabled"

    # 检查 force push
    force_push="$(echo "$response" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(d.get('allow_force_pushes', {}).get('enabled', True))" 2>/dev/null || echo "unknown")"
    check_rule "${branch}/allow_force_push" "False" "$force_push"

    # 检查删除
    allow_delete="$(echo "$response" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(d.get('allow_deletions', {}).get('enabled', True))" 2>/dev/null || echo "unknown")"
    check_rule "${branch}/allow_deletion" "False" "$allow_delete"
  done
else
  log_warn "未提供 GITHUB_TOKEN，跳过 API 验证"
  log_info "请手动检查 GitHub Settings > Branches > Branch protection rules"
fi

# ---------------------------------------------------------------------------
# 本地 Git 验证
# ---------------------------------------------------------------------------
log_step "本地 Git 验证"

# 检查 main 分支是否存在
if git rev-parse --verify main >/dev/null 2>&1; then
  log_ok "main 分支存在"
  PASS=$((PASS + 1))
else
  log_err "main 分支不存在"
  FAIL=$((FAIL + 1))
fi

# 检查 develop 分支是否存在
if git rev-parse --verify develop >/dev/null 2>&1; then
  log_ok "develop 分支存在"
  PASS=$((PASS + 1))
else
  log_err "develop 分支不存在"
  FAIL=$((FAIL + 1))
fi

# 检查 tag 签名能力
if git config user.signingkey >/dev/null 2>&1; then
  log_ok "GPG 签名密钥已配置"
  PASS=$((PASS + 1))
else
  log_warn "GPG 签名密钥未配置（发布时需配置）"
fi

# 检查 CI 工作流
if [ -f ".github/workflows/ci.yml" ]; then
  log_ok "CI 工作流存在 (.github/workflows/ci.yml)"
  PASS=$((PASS + 1))

  # 检查 guard-rules job
  if grep -q "guard-rules" ".github/workflows/ci.yml"; then
    log_ok "guard-rules 门禁 job 存在"
    PASS=$((PASS + 1))
  else
    log_err "guard-rules 门禁 job 不存在"
    FAIL=$((FAIL + 1))
  fi
else
  log_err "CI 工作流不存在"
  FAIL=$((FAIL + 1))
fi

# ---------------------------------------------------------------------------
# 汇总
# ---------------------------------------------------------------------------
echo ""
log_step "验证结果汇总"
TOTAL=$((PASS + FAIL))
printf "  PASS: %d  FAIL: %d  TOTAL: %d\n" "$PASS" "$FAIL" "$TOTAL"

if [ "$FAIL" -gt 0 ]; then
  log_err "分支保护验证失败：${FAIL} 项不合规"
  exit 1
else
  log_ok "分支保护验证通过：${PASS}/${TOTAL} 项全部合规"
  exit 0
fi
